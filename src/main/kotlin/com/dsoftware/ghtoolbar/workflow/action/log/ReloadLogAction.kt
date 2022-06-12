package com.dsoftware.ghtoolbar.workflow.action.log

import com.dsoftware.ghtoolbar.workflow.action.ActionKeys
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger

class ReloadLogAction : RefreshAction("Refresh Workflow Log", null, AllIcons.Actions.Refresh) {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(ActionKeys.ACTION_DATA_CONTEXT)?.workflowRunDataProvider
        e.presentation.isEnabled = selection != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.info("GitHubWorkflowLogReloadAction action performed")
        e.getRequiredData(ActionKeys.ACTION_DATA_CONTEXT).workflowRunDataProvider?.reloadLog()
    }

    companion object {
        private val LOG = logger<ReloadLogAction>()
    }
}