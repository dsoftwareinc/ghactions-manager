package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.WorkflowRunFilter
import com.dsoftware.ghmanager.api.Workflows
import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.data.request.GithubRequestPagination
import org.jetbrains.plugins.github.pullrequest.data.GHListLoader
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class WorkflowRunListLoader(
    progressManager: ProgressManager,
    private val requestExecutor: GithubApiRequestExecutor,
    private val repositoryCoordinates: RepositoryCoordinates,
    settingsService: GhActionsSettingsService,
    private val filter: WorkflowRunFilter,
) : GHListLoaderBase<WorkflowRun>(progressManager) {
    val url: String = Workflows.getWorkflowRuns(repositoryCoordinates, filter).url
    var totalCount: Int = 1
    val frequency: Long = settingsService.state.frequency.toLong()
    private val pageSize = 30
    private val page: Int = 1
    val listModel = CollectionListModel<WorkflowRun>()
    private val task: ScheduledFuture<*>
    var refreshRuns: Boolean = true

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

    override fun dispose() {
        super.dispose()
        task.cancel(true)
    }

    override fun reset() {
        LOG.debug("Removing all from the list model")
        super.reset()
    }

    override fun canLoadMore() = !loading && (page * pageSize < totalCount)

    override fun doLoadMore(indicator: ProgressIndicator, update: Boolean): List<WorkflowRun> {
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

    companion object {
        private val LOG = logger<WorkflowRunListLoader>()
    }
}