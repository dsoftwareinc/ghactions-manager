package com.dsoftware.ghmanager.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger

class ReloadJobsAction : RefreshAction("Refresh Workflow Jobs", null, AllIcons.Actions.Refresh) {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(ActionKeys.ACTION_DATA_CONTEXT)?.jobsDataProvider
        e.presentation.isEnabled = selection != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("ReloadJobsAction action performed")
        e.getRequiredData(ActionKeys.ACTION_DATA_CONTEXT).jobsDataProvider?.reload()
    }

    companion object {
        private val LOG = logger<ReloadJobsAction>()
    }
}