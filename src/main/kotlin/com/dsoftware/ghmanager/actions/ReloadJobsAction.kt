package com.dsoftware.ghmanager.actions

import com.dsoftware.ghmanager.i18n.MessagesBundle.message
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger

class ReloadJobsAction : RefreshAction(message("action.name.refresh-jobs-list"), null, AllIcons.Actions.Refresh) {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(ActionKeys.SELECTED_WF_CONTEXT)?.jobsDataProvider
        e.presentation.isEnabled = selection != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("ReloadJobsAction action performed")
        e.getRequiredData(ActionKeys.SELECTED_WF_CONTEXT).jobsDataProvider?.reload()
    }

    companion object {
        private val LOG = logger<ReloadJobsAction>()
    }
}