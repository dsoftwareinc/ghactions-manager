package com.dsoftware.ghtoolbar.workflow.data

import com.dsoftware.ghtoolbar.api.GitHubWorkflowRun
import com.dsoftware.ghtoolbar.workflow.RepositoryCoordinates
import com.intellij.openapi.Disposable
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import javax.swing.ListModel

class WorkflowRunDataContext(
    val repositoryCoordinates: RepositoryCoordinates,
    val listModel: ListModel<GitHubWorkflowRun>,
    val dataLoader: WorkflowDataLoader,
    val listLoader: WorkflowRunListLoader,
    val account: GithubAccount
) : Disposable {
    override fun dispose() {
    }

}
