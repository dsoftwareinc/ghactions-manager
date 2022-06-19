package com.dsoftware.ghtoolbar.workflow.action.list

import com.dsoftware.ghtoolbar.workflow.action.ActionKeys
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger

class ReloadListAction : RefreshAction("Refresh Workflow Runs", null, AllIcons.Actions.Refresh) {
    override fun update(e: AnActionEvent) {
        val context = e.getData(ActionKeys.ACTION_DATA_CONTEXT)
        e.presentation.isEnabled = context != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("ReloadListAction action performed")
        e.getRequiredData(ActionKeys.ACTION_DATA_CONTEXT).resetAllData()
    }

    companion object {
        private val LOG = logger<ReloadListAction>()
    }
}