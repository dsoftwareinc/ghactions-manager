package com.dsoftware.ghtoolbar.workflow.data

import com.dsoftware.ghtoolbar.api.GitHubWorkflowRun
import com.dsoftware.ghtoolbar.api.Workflows
import com.dsoftware.ghtoolbar.workflow.RepositoryCoordinates
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.pullrequest.data.GHListLoaderBase

class WorkflowRunListLoader(
    progressManager: ProgressManager,
    private val requestExecutor: GithubApiRequestExecutor,
    private val repositoryCoordinates: RepositoryCoordinates,
    private val listModel: CollectionListModel<GitHubWorkflowRun>,
) : GHListLoaderBase<GitHubWorkflowRun>(progressManager) {

    var loaded: Boolean = false

    private var resetDisposable: Disposable

    init {
        requestExecutor.addListener(this) { reset() }

        resetDisposable = Disposer.newDisposable()
        Disposer.register(this, resetDisposable)
    }

    override fun reset() {
        LOG.info("Removing all from the list model")
        listModel.removeAll()
        loaded = false

        Disposer.dispose(resetDisposable)
        resetDisposable = Disposer.newDisposable()
        Disposer.register(this, resetDisposable)

        loadMore()
    }

    //This should not be needed, it is weird that originally it requires error != null to be able to load data
    override fun canLoadMore() = !loading && !loaded

    override fun doLoadMore(indicator: ProgressIndicator, update: Boolean): List<GitHubWorkflowRun> {
        LOG.info("Do load more update: $update, indicator: $indicator")

        LOG.info("Get workflow runs")
        val request = Workflows.getWorkflowRuns(repositoryCoordinates)
        val response = requestExecutor.execute(indicator, request)
        val result = response.workflow_runs
        LOG.info("Got ${result.size} workflows")
        //This is quite slow - N+1 requests, but there are no simpler way to get it, at least now.
        result.parallelStream().forEach {
            LOG.info("Get workflow by url ${it.workflow_url}")
            it.workflowName = requestExecutor.execute(Workflows.getWorkflowByUrl(it.workflow_url)).name
        }
        loaded = true
        return result
    }

    companion object {
        private val LOG = logger<WorkflowRunListLoader>()
    }
}