package com.dsoftware.ghtoolbar.actions

import com.dsoftware.ghtoolbar.api.Workflows
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction

class RerunWorkflowAction : DumbAwareAction("Rerun Workflow", null, AllIcons.Actions.Rerun) {

    override fun update(e: AnActionEvent) {
        val url = getUrl(e.dataContext)
        e.presentation.isEnabledAndVisible = url != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("RerunWorkflowAction action")
        e.dataContext.getData(CommonDataKeys.PROJECT) ?: return
        getUrl(e.dataContext)?.let {
            LOG.debug("Triggering rerun ${it}")
            val request = Workflows.postRerunWorkflow(it)
            val context = e.getRequiredData(ActionKeys.ACTION_DATA_CONTEXT)
            val future = context.dataContext.dataLoader.createDataProvider(request).request
            future.thenApply {
                context.resetAllData()
            }
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