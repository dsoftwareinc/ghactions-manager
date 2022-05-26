package com.dsoftware.githubactionstab.workflow.data

import com.dsoftware.githubactionstab.api.GitHubWorkflowRun
import com.dsoftware.githubactionstab.workflow.GitHubRepositoryCoordinates
import com.intellij.openapi.Disposable
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import javax.swing.ListModel

class GitHubWorkflowRunDataContext(
    val gitHubRepositoryCoordinates: GitHubRepositoryCoordinates,
    val listModel: ListModel<GitHubWorkflowRun>,
    val dataLoader: GitHubWorkflowDataLoader,
    val listLoader: GitHubWorkflowRunListLoader,
    val account: GithubAccount
) : Disposable {
    override fun dispose() {
    }

}
