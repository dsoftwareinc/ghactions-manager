package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.Workflows
import com.dsoftware.ghmanager.api.model.GitHubWorkflowRun
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.vcs.log.runInEdt
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
) : GHListLoaderBase<GitHubWorkflowRun>(progressManager) {
    val url: String = Workflows.getWorkflowRuns(repositoryCoordinates).url
    var totalCount: Int = 1
    val frequency: Long = settingsService.state.frequency.toLong()
    private val pageSize = 30
    private val page: Int = 1
    val listModel = CollectionListModel<GitHubWorkflowRun>()
    private val task: ScheduledFuture<*>
    var refreshRuns: Boolean = true

    init {
        val checkedDisposable = Disposer.newCheckedDisposable()
        Disposer.register(this, checkedDisposable)
        val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
        task = scheduler.scheduleWithFixedDelay({
            if (refreshRuns)
                runInEdt(checkedDisposable) { loadMore(update = true) }
        }, 1, frequency, TimeUnit.SECONDS)
        LOG.debug("Create CollectionListModel<GitHubWorkflowRun>() and loader")
        listModel.removeAll()
        addDataListener(this, object : GHListLoader.ListDataListener {
            override fun onDataAdded(startIdx: Int) {
                val loadedData = this@WorkflowRunListLoader.loadedData
                loadedData.sortWith { o1, o2 -> o2.run_number.compareTo(o1.run_number) }
                listModel.replaceAll(loadedData)
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

    override fun doLoadMore(indicator: ProgressIndicator, update: Boolean): List<GitHubWorkflowRun> {
        LOG.debug("Do load more update: $update, indicator: $indicator")

        val request = Workflows.getWorkflowRuns(
            repositoryCoordinates,
            pagination = GithubRequestPagination(page, pageSize)
        )
        val response = requestExecutor.execute(indicator, request)
        totalCount = response.total_count
        val result = response.workflow_runs
        if (update) {
            val existingRunIds = loadedData.mapIndexed { idx, it -> it.id to idx }.toMap()
            result.filter { existingRunIds.containsKey(it.id) }.forEach { run ->
                val index = existingRunIds[run.id] ?: -1
                if (index > -1) {
                    loadedData[index] = run
                    dataEventDispatcher.multicaster.onDataUpdated(index)
                }
            }
            return result.filter { !existingRunIds.contains(it.id) }
        }
        LOG.debug("Got ${result.size} in page $page workflows (totalCount=$totalCount)")
        return result
    }

    companion object {
        private val LOG = logger<WorkflowRunListLoader>()
    }
}