package com.dsoftware.ghmanager.actions

import com.dsoftware.ghmanager.data.SingleRunDataLoader
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon

abstract class OpenInBrowserAction(
    text: String = "Open GitHub Link in Browser",
    description: String? = null,
    icon: Icon = AllIcons.Xml.Browsers.Chrome,
) : DumbAwareAction(text, description, icon) {

    override fun update(e: AnActionEvent) {
        val data = getData(e.dataContext)
        e.presentation.isEnabledAndVisible = data != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        getData(e.dataContext)?.let { BrowserUtil.browse(it) }
    }

    abstract fun getData(dataContext: DataContext): String?
    companion object{
        @JvmStatic
        protected val LOG = logger<OpenInBrowserAction>()
    }
}

class WorkflowOpenInBrowserAction : OpenInBrowserAction("Open this workflow run in browser") {

    override fun getData(dataContext: DataContext): String? {
        dataContext.getData(CommonDataKeys.PROJECT) ?: return null
        return dataContext.getData(ActionKeys.SELECTED_WORKFLOW_RUN)?.html_url
    }
}

class JobOpenInBrowserAction : OpenInBrowserAction("Open this job in browser") {

    override fun getData(dataContext: DataContext): String? {
        dataContext.getData(CommonDataKeys.PROJECT) ?: return null
        return dataContext.getData(ActionKeys.SELECTED_JOB)?.htmlUrl
    }
}

//TODO
class PullRequestOpenInBrowserAction : OpenInBrowserAction("Open Pull-Request in Browser") {
    override fun getData(dataContext: DataContext): String? {
        dataContext.getData(CommonDataKeys.PROJECT) ?: return null
        LOG.info("${dataContext.getData(ActionKeys.SELECTED_WORKFLOW_RUN)?.url}")
        return dataContext.getData(ActionKeys.SELECTED_WORKFLOW_RUN)
            ?.pull_requests
            ?.firstOrNull()
            ?.url
    }
}