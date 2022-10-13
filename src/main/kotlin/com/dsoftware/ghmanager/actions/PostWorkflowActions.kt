package com.dsoftware.ghmanager.actions

import com.dsoftware.ghmanager.api.Workflows
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon

abstract class PostUrlAction(
    text: String, description: String?, icon: Icon
) : DumbAwareAction(text, description, icon) {

    override fun update(e: AnActionEvent) {
        val url = getUrl(e.dataContext)
        e.presentation.isEnabledAndVisible = url != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("CancelWorkflowAction action")
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

    abstract fun getUrl(dataContext: DataContext): String?

    companion object {
        private val LOG = logger<CancelWorkflowAction>()
    }
}

class CancelWorkflowAction : PostUrlAction("Cancel Workflow", null, AllIcons.Actions.Cancel) {
    override fun update(e: AnActionEvent) {
        val context = e.dataContext.getData(ActionKeys.SELECTED_WORKFLOW_RUN)
        val url = context?.cancel_url
        val status = context?.status
        e.presentation.isEnabledAndVisible = (url != null) && (status == "in_progress" || status == "queued")
    }

    override fun getUrl(dataContext: DataContext): String? {
        dataContext.getData(CommonDataKeys.PROJECT) ?: return null
        return dataContext.getData(ActionKeys.SELECTED_WORKFLOW_RUN)?.cancel_url
    }

}

class RerunWorkflowAction : PostUrlAction("Rerun Workflow", null, AllIcons.Actions.Rerun) {
    override fun getUrl(dataContext: DataContext): String? {
        dataContext.getData(CommonDataKeys.PROJECT) ?: return null
        return dataContext.getData(ActionKeys.SELECTED_WORKFLOW_RUN)?.rerun_url
    }
}