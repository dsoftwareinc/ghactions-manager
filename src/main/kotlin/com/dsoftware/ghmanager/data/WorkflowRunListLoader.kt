package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.Workflows
import com.dsoftware.ghmanager.api.model.GitHubWorkflowRun
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.data.request.GithubRequestPagination
import org.jetbrains.plugins.github.pullrequest.data.GHListLoaderBase
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class WorkflowRunListLoader(
    progressManager: ProgressManager,
    private val requestExecutor: GithubApiRequestExecutor,
    private val repositoryCoordinates: RepositoryCoordinates,
    settingsService: GhActionsSettingsService,
) : GHListLoaderBase<GitHubWorkflowRun>(progressManager) {
    var totalCount: Int = 1
    val frequency: Long = settingsService.state.frequency
    private val pageSize = 30
    private val page: Int = 1

    private val task: ScheduledFuture<*>

    init {
        val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
        task = scheduler.scheduleWithFixedDelay({
            loadMore(update = true)
        }, 1, frequency, TimeUnit.SECONDS)
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
        LOG.info("Do load more update: $update, indicator: $indicator")

        val request = Workflows.getWorkflowRuns(
            repositoryCoordinates,
            pagination = GithubRequestPagination(page, pageSize)
        )
        val response = requestExecutor.execute(indicator, request)
        totalCount = response.total_count
        val result = response.workflow_runs
        if (update) {
            val existingRunIds = loadedData.map { it.id }.toSet()
            result.filter { existingRunIds.contains(it.id) }.forEach { run ->
                updateData(run)
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