package com.dsoftware.githubactionstab.workflow.action

import com.dsoftware.githubactionstab.workflow.GitHubWorkflowToolWindowController
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Supplier

class GitHubViewPullRequestsAction : DumbAwareAction(
    { GHWorkflowToolWindowFactory.ID },
    Supplier { null },
    AllIcons.Vcs.Vendors.Github
) {

    override fun update(e: AnActionEvent) {
        LOG.debug("update")
        e.presentation.isEnabledAndVisible = isEnabledAndVisible(e)
    }

    private fun isEnabledAndVisible(e: AnActionEvent): Boolean {
        val project = e.project ?: return false

        return project.service<GitHubWorkflowToolWindowController>().isAvailable()
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project!!.service<GitHubWorkflowToolWindowController>().activate()
    }

    companion object {
        private val LOG = logger<GitHubViewPullRequestsAction>()
    }
}