package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.WorkflowRunFilter
import com.dsoftware.ghmanager.api.Workflows
import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.intellij.collaboration.async.CompletableFutureUtil
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.data.request.GithubRequestPagination
import org.jetbrains.plugins.github.pullrequest.data.GHListLoader
import org.jetbrains.plugins.github.util.NonReusableEmptyProgressIndicator
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

class WorkflowRunListLoader(
    private val progressManager: ProgressManager,
    private val requestExecutor: GithubApiRequestExecutor,
    private val repositoryCoordinates: RepositoryCoordinates,
    settingsService: GhActionsSettingsService,
    private val filter: WorkflowRunFilter,
) : Disposable {
    private var lastFuture = CompletableFuture.completedFuture(emptyList<WorkflowRun>())
    private val loadingStateChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)
    private val errorChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)
    val url: String = Workflows.getWorkflowRuns(repositoryCoordinates, filter).url
    var totalCount: Int = 1
    val frequency: Long = settingsService.state.frequency.toLong()
    private val pageSize = 30
    private val page: Int = 1
    val listModel = CollectionListModel<WorkflowRun>()
    private val task: ScheduledFuture<*>
    var refreshRuns: Boolean = true
    private var progressIndicator = NonReusableEmptyProgressIndicator()
    private val dataEventDispatcher = EventDispatcher.create(GHListLoader.ListDataListener::class.java)
    val loadedData = ArrayList<WorkflowRun>()
    var error: Throwable? by Delegates.observable(null) { _, _, _ ->
        errorChangeEventDispatcher.multicaster.eventOccurred()
    }

    var loading: Boolean by Delegates.observable(false) { _, _, _ ->
        loadingStateChangeEventDispatcher.multicaster.eventOccurred()
    }

    init {
        val checkedDisposable = Disposer.newCheckedDisposable()
        Disposer.register(this, checkedDisposable)
        val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
        task = scheduler.scheduleWithFixedDelay({
            if (refreshRuns) loadMore(update = true)
        }, 1, frequency, TimeUnit.SECONDS)
        LOG.debug("Create CollectionListModel<WorkflowRun>() and loader")
        listModel.removeAll()
        addDataListener(this, object : GHListLoader.ListDataListener {
            override fun onDataAdded(startIdx: Int) {
                val loadedData = this@WorkflowRunListLoader.loadedData
                listModel.replaceAll(loadedData.sorted())
            }

            override fun onDataUpdated(idx: Int) {
                val loadedData = this@WorkflowRunListLoader.loadedData
                listModel.setElementAt(loadedData[idx], idx)
            }
        })
    }

    fun loadMore(update: Boolean = false) {
        val indicator = progressIndicator
        if (canLoadMore() || update) {
            loading = true
            requestLoadMore(indicator, update).handleOnEdt { list, error ->
                if (indicator.isCanceled) return@handleOnEdt
                loading = false
                if (error != null) {
                    if (!CompletableFutureUtil.isCancellation(error)) this.error = error
                } else if (!list.isNullOrEmpty()) {
                    val startIdx = loadedData.size
                    loadedData.addAll(list)
                    dataEventDispatcher.multicaster.onDataAdded(startIdx)
                }
            }
        }
    }

    private fun requestLoadMore(indicator: ProgressIndicator, update: Boolean): CompletableFuture<List<WorkflowRun>> {
        lastFuture = lastFuture.thenCompose {
            progressManager.submitIOTask(indicator) {
                doLoadMore(indicator, update)
            }
        }
        return lastFuture
    }

    override fun dispose() {
        progressIndicator.cancel()
        task.cancel(true)
    }

    fun reset() {
        LOG.debug("Removing all from the list model")
        lastFuture = lastFuture.handle { _, _ ->
            listOf()
        }
        progressIndicator.cancel()
        progressIndicator = NonReusableEmptyProgressIndicator()
        error = null
        loading = false
        loadedData.clear()
        dataEventDispatcher.multicaster.onAllDataRemoved()
    }

    fun canLoadMore() = !loading && (page * pageSize < totalCount)

    fun doLoadMore(indicator: ProgressIndicator, update: Boolean): List<WorkflowRun> {
        LOG.debug("Do load more update: $update, indicator: $indicator")

        val request = Workflows.getWorkflowRuns(
            repositoryCoordinates,
            filter,
            pagination = GithubRequestPagination(page, pageSize),
        )
        val response = requestExecutor.execute(indicator, request)
        totalCount = response.total_count
        val workflowRuns = response.workflow_runs
        if (update) {
            val existingRunIds = loadedData.mapIndexed { idx, it -> it.id to idx }.toMap()
            val newRuns = workflowRuns.filter { !existingRunIds.contains(it.id) }
            // Update existing runs

            workflowRuns
                .filter { existingRunIds.contains(it.id) }
                .forEach { run -> // Update
                    val index = existingRunIds.getOrDefault(run.id, null)
                    if (index != null && loadedData[index] != run) {
                        loadedData[index] = run
                        if (newRuns.isEmpty()) // No point in updating if anyway we will send replaceAll
                            dataEventDispatcher.multicaster.onDataUpdated(index)
                    }
                }

            // Add new runs.
            return newRuns
        }
        LOG.debug("Got ${workflowRuns.size} in page $page workflows (totalCount=$totalCount)")
        return workflowRuns
    }

    fun addDataListener(disposable: Disposable, listener: GHListLoader.ListDataListener) =
        dataEventDispatcher.addListener(listener, disposable)

    fun addLoadingStateChangeListener(disposable: Disposable, listener: () -> Unit) =
        SimpleEventListener.addDisposableListener(loadingStateChangeEventDispatcher, disposable, listener)

    fun addErrorChangeListener(disposable: Disposable, listener: () -> Unit) =
        SimpleEventListener.addDisposableListener(errorChangeEventDispatcher, disposable, listener)

    companion object {
        private val LOG = logger<WorkflowRunListLoader>()
    }
}