package com.dsoftware.ghtoolbar.workflow

import com.dsoftware.ghtoolbar.api.model.GitHubWorkflowRun
import com.dsoftware.ghtoolbar.data.WorkflowDataLoader

import com.dsoftware.ghtoolbar.data.WorkflowRunListLoader
import com.dsoftware.ghtoolbar.data.WorkflowRunLogsDataProvider
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.plugins.github.api.GHRepositoryPath
import org.jetbrains.plugins.github.api.GithubServerPath
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import javax.swing.ListModel
import kotlin.properties.Delegates

class WorkflowRunDataContext(
    val repositoryCoordinates: RepositoryCoordinates,
    val runsListModel: ListModel<GitHubWorkflowRun>,
    val dataLoader: WorkflowDataLoader,
    val runsListLoader: WorkflowRunListLoader,
    val account: GithubAccount
) : Disposable {
    override fun dispose() {
    }

}

data class RepositoryCoordinates(val serverPath: GithubServerPath, val repositoryPath: GHRepositoryPath) {
    fun toUrl(): String {
        return serverPath.toUrl() + "/" + repositoryPath
    }

    override fun toString(): String {
        return "$serverPath/$repositoryPath"
    }
}

class WorkflowRunListSelectionHolder {

    @get:RequiresEdt
    @set:RequiresEdt
    var selection: GitHubWorkflowRun? by Delegates.observable(null) { _, _, _ ->
        selectionChangeEventDispatcher.multicaster.eventOccurred()
    }

    private val selectionChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

    @RequiresEdt
    fun addSelectionChangeListener(disposable: Disposable, listener: () -> Unit) =
        SimpleEventListener.addDisposableListener(selectionChangeEventDispatcher, disposable, listener)
}


class WorkflowRunSelectionContext internal constructor(
    private val dataContext: WorkflowRunDataContext,
    private val selectionHolder: WorkflowRunListSelectionHolder
) {

    fun resetAllData() {
        LOG.info("resetAllData")
        dataContext.runsListLoader.reset()
        dataContext.dataLoader.invalidateAllData()
    }

    val workflowRun: GitHubWorkflowRun?
        get() = selectionHolder.selection

    val workflowRunLogsDataProvider: WorkflowRunLogsDataProvider?
        get() = workflowRun?.let { dataContext.dataLoader.getDataProvider(it.logs_url) }

    companion object {
        private val LOG = logger<WorkflowRunSelectionContext>()
    }
}