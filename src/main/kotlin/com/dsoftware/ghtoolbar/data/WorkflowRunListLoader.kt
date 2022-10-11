package com.dsoftware.ghtoolbar.data

import com.dsoftware.ghtoolbar.api.Workflows
import com.dsoftware.ghtoolbar.api.model.GitHubWorkflowRun
import com.dsoftware.ghtoolbar.workflow.RepositoryCoordinates
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.data.request.GithubRequestPagination
import org.jetbrains.plugins.github.pullrequest.data.GHListLoaderBase
import java.util.concurrent.TimeUnit

class WorkflowRunListLoader(
    progressManager: ProgressManager,
    private val requestExecutor: GithubApiRequestExecutor,
    private val repositoryCoordinates: RepositoryCoordinates,
) : GHListLoaderBase<GitHubWorkflowRun>(progressManager) {
    var totalCount: Int = 1
    private val pageSize = 30
    private val page: Int = 1
    private val frequency: Long = 30

    init {

        val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
        scheduler.scheduleWithFixedDelay({
            loadMore(update = true)
        }, frequency, frequency, TimeUnit.SECONDS)
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
            val newRuns = result.filter { run -> loadedData.all { it != run } }
            result.forEach { run ->
                updateData(run)
            }
            return newRuns
        }
        LOG.debug("Got ${result.size} in page $page workflows (totalCount=$totalCount)")
        return result
    }

    companion object {
        private val LOG = logger<WorkflowRunListLoader>()
    }
}