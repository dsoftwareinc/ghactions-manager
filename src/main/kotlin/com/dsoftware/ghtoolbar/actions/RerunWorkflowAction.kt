package com.dsoftware.ghtoolbar.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction

class RerunWorkflowAction : DumbAwareAction("Rerun Workflow", null, AllIcons.Actions.Rerun) {
    //todo
    override fun update(e: AnActionEvent) {
        val data = getUrl(e.dataContext)
        e.presentation.isEnabledAndVisible = data != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("RerunWorkflowAction action")
        e.dataContext.getData(CommonDataKeys.PROJECT) ?: return
        getUrl(e.dataContext)?.let {
            LOG.debug("Triggering rerun ${it}")
        }
    }

    private fun getUrl(dataContext: DataContext): String? {
        dataContext.getData(CommonDataKeys.PROJECT) ?: return null
        return dataContext.getData(ActionKeys.SELECTED_WORKFLOW_RUN)?.rerun_url
    }

    companion object {
        private val LOG = logger<RerunWorkflowAction>()
    }
}