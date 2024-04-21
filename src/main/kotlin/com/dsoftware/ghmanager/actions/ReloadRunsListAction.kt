package com.dsoftware.ghmanager.actions

import com.dsoftware.ghmanager.i18n.MessagesBundle.message
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger

class ReloadRunsListAction :
    RefreshAction(message("action.name.refresh-workflow-runs"), null, AllIcons.Actions.Refresh) {
    override fun update(e: AnActionEvent) {
        val context = e.getData(ActionKeys.SELECTED_WF_CONTEXT)
        e.presentation.isEnabled = context != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("ReloadRunsListAction action performed")
        e.getRequiredData(ActionKeys.SELECTED_WF_CONTEXT).resetAllData()
    }

    companion object {
        private val LOG = logger<ReloadRunsListAction>()
    }
}