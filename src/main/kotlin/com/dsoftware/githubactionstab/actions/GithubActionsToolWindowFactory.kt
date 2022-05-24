package com.dsoftware.githubactionstab.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel



class GithubActionsToolWindowFactory : ToolWindowFactory {

    private val basePanel: JPanel? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) = with(toolWindow as ToolWindowEx) {
        toolWindow.getContentManager().addContent(
            ContentFactory.SERVICE.getInstance().createContent(basePanel, "", false)
        )
    }
}