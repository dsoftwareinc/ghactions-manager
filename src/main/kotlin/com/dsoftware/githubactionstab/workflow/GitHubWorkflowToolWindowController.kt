package com.dsoftware.githubactionstab.workflow

import com.dsoftware.githubactionstab.ui.GitHubWorkflowToolWindowTabController
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.dsoftware.githubactionstab.workflow.action.GHWorkflowToolWindowFactory

@Service
internal class GitHubWorkflowToolWindowController(private val project: Project) : Disposable {
    @RequiresEdt
    fun isAvailable(): Boolean {
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(GHWorkflowToolWindowFactory.ID) ?: return false
        return toolWindow.isAvailable
    }

    @RequiresEdt
    fun activate() {
        LOG.debug("GitHubWorkflowToolWindowController::Activate")
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(GHWorkflowToolWindowFactory.ID) ?: return
        toolWindow.activate {
            getTabController(toolWindow)
        }
    }

    private fun getTabController(toolWindow: ToolWindow): GitHubWorkflowToolWindowTabController? =
        toolWindow.contentManagerIfCreated?.selectedContent?.getUserData(GitHubWorkflowToolWindowTabController.KEY)

    override fun dispose() {
    }

    companion object {
        private val LOG = Logger.getInstance("com.dsoftware.githubactionstab.workflow")
    }
}