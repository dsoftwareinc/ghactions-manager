package com.dsoftware.ghtoolbar.data

import com.dsoftware.ghtoolbar.api.Workflows
import com.dsoftware.ghtoolbar.api.model.GitHubWorkflowRun
import com.dsoftware.ghtoolbar.workflow.RepositoryCoordinates
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.data.request.GithubRequestPagination
import org.jetbrains.plugins.github.pullrequest.data.GHListLoaderBase
import java.util.concurrent.TimeUnit

class WorkflowRunListLoader(
    progressManager: ProgressManager,
    private val requestExecutor: GithubApiRequestExecutor,
    private val repositoryCoordinates: RepositoryCoordinates,
    private val listModel: CollectionListModel<GitHubWorkflowRun>,
) : GHListLoaderBase<GitHubWorkflowRun>(progressManager) {
    var totalCount: Int = 1
    private val pageSize = 50
    var page: Int = 0
    private var resetDisposable: Disposable

    init {
//        requestExecutor.addListener(this) { reset() }

        resetDisposable = Disposer.newDisposable()
        Disposer.register(this, resetDisposable)
        val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
        scheduler.scheduleWithFixedDelay({
            loadMore(update = true)
        }, 60, 60, TimeUnit.SECONDS)
    }

    override fun reset() {
        LOG.debug("Removing all from the list model")
        super.reset()
        page = 0
        Disposer.dispose(resetDisposable)
        resetDisposable = Disposer.newDisposable()
        Disposer.register(this, resetDisposable)
        listModel.removeAll()
    }

    override fun canLoadMore() = !loading && (page * pageSize < totalCount)

    override fun doLoadMore(indicator: ProgressIndicator, update: Boolean): List<GitHubWorkflowRun> {
        LOG.info("Do load more update: $update, indicator: $indicator")
        if (!update) {
            page += 1
        }

        val request = Workflows.getWorkflowRuns(
            repositoryCoordinates,
            pagination = GithubRequestPagination(page, pageSize)
        )
        val response = requestExecutor.execute(indicator, request)
        totalCount = response.total_count
        val result = response.workflow_runs
        if (update) {
            result.forEach { updateData(it) }
        }
        LOG.debug("Got ${result.size} in page $page workflows (totalCount=$totalCount)")
        return result
    }

    companion object {
        private val LOG = logger<WorkflowRunListLoader>()
    }
}