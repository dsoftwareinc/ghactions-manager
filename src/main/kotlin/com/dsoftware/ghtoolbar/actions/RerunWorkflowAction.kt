package com.dsoftware.ghtoolbar.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction

class RerunWorkflowAction : DumbAwareAction("Rerun Workflow") {
    //todo
    override fun update(e: AnActionEvent) {
        val data = getData(e.dataContext)
        e.presentation.isEnabledAndVisible = data != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("RerunWorkflowAction action")
        e.dataContext.getData(CommonDataKeys.PROJECT) ?: return
        getData(e.dataContext)?.let {
            LOG.debug("Triggering rerun ${it}")
        }
    }

    private fun getData(dataContext: DataContext): String? {
        dataContext.getData(CommonDataKeys.PROJECT) ?: return null
        return dataContext.getData(ActionKeys.SELECTED_WORKFLOW_RUN)?.rerun_url
    }

    companion object {
        private val LOG = logger<RerunWorkflowAction>()
    }
}