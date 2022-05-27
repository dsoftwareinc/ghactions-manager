package com.dsoftware.githubactionstab.workflow.data

import com.dsoftware.githubactionstab.api.GitHubWorkflowRun
import com.dsoftware.githubactionstab.workflow.RepositoryCoordinates
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
