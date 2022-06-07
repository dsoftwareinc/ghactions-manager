package com.dsoftware.githubactionstab.workflow

import com.dsoftware.githubactionstab.ui.WorkflowToolWindowTabController
import com.dsoftware.githubactionstab.workflow.action.WorkflowToolWindowFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.concurrency.annotations.RequiresEdt

@Service
internal class WorkflowToolWindowController(private val project: Project) : Disposable {
    @RequiresEdt
    fun isAvailable(): Boolean {
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(WorkflowToolWindowFactory.ID) ?: return false
        return toolWindow.isAvailable
    }

    @RequiresEdt
    fun activate() {
        LOG.info("WorkflowToolWindowController::Activate")
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(WorkflowToolWindowFactory.ID) ?: return
        toolWindow.activate {
            getTabController(toolWindow)
        }
    }

    private fun getTabController(toolWindow: ToolWindow): WorkflowToolWindowTabController? =
        toolWindow.contentManagerIfCreated?.selectedContent?.getUserData(WorkflowToolWindowTabController.KEY)

    override fun dispose() {
    }

    companion object {
        private val LOG = thisLogger()
    }
}