package com.dsoftware.ghmanager.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger

class ReloadJobLogAction : RefreshAction("Refresh Job Log", null, AllIcons.Actions.Refresh) {
    override fun update(e: AnActionEvent) {
        val selection = e.getData(ActionKeys.ACTION_DATA_CONTEXT)?.logDataProvider
        e.presentation.isEnabled = selection != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("GitHubWorkflowLogReloadAction action performed")
        e.getRequiredData(ActionKeys.ACTION_DATA_CONTEXT).logDataProvider?.reload()
    }

    companion object {
        private val LOG = logger<ReloadJobLogAction>()
    }
}