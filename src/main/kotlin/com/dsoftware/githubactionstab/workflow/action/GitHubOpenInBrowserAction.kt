package com.dsoftware.githubactionstab.workflow.action

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.DumbAwareAction

class GithubOpenInBrowserAction : DumbAwareAction("Open GitHub link in browser") {

    override fun update(e: AnActionEvent) {
        val data = getData(e.dataContext)
        e.presentation.isEnabledAndVisible = data != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        getData(e.dataContext)?.let { BrowserUtil.browse(it) }
    }

    private fun getData(dataContext: DataContext): String? {
        dataContext.getData(CommonDataKeys.PROJECT) ?: return null
        return getDataFromWorkflow(dataContext)
    }

    private fun getDataFromWorkflow(dataContext: DataContext): String? {
        val workflow = dataContext.getData(GitHubWorkflowRunActionKeys.SELECTED_WORKFLOW_RUN) ?: return null
        return workflow.html_url
    }
}