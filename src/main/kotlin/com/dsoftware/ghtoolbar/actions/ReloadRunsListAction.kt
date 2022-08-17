package com.dsoftware.ghtoolbar.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger

class ReloadRunsListAction : RefreshAction("Refresh Workflow Runs", null, AllIcons.Actions.Refresh) {
    override fun update(e: AnActionEvent) {
        val context = e.getData(ActionKeys.ACTION_DATA_CONTEXT)
        e.presentation.isEnabled = context != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("ReloadRunsListAction action performed")
        e.getRequiredData(ActionKeys.ACTION_DATA_CONTEXT).resetAllData()
    }

    companion object {
        private val LOG = logger<ReloadRunsListAction>()
    }
}