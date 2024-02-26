// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.dsoftware.ghmanager.actions

import com.dsoftware.ghmanager.api.model.WorkflowType
import com.dsoftware.ghmanager.i18n.MessagesBundle.message
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionMenu
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.popup.JBPopupFactory

class WorkflowTypesActionsGroup : ActionGroup(message("action-group.name.select-workflow"), true) {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isPerformGroup = true
        e.presentation.isDisableGroupIfEmpty = false
        e.presentation.putClientProperty(ActionMenu.SUPPRESS_SUBMENU, true)
        e.presentation.isEnabledAndVisible = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        JBPopupFactory.getInstance()
            .createActionGroupPopup(
                "Select Workflow to Dispatch",
                this,
                e.dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false, null, -1, null,
                ActionPlaces.getActionGroupPopupPlace(null)
            ).showInBestPositionFor(e.dataContext)
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) return EMPTY_ARRAY
        val context = e.getData(ActionKeys.ACTION_DATA_CONTEXT) ?: return EMPTY_ARRAY
        val workflowTypeList: List<WorkflowType> = context.runsListLoader.workflowTypes
        LOG.debug("Got ${workflowTypeList.size} workflow types")

        val children: List<AnAction> = workflowTypeList.map { workflowType ->
            WorkflowDispatchAction(workflowType)
        }.toList()

        return children.toTypedArray()
    }

    companion object {
        private val LOG = logger<WorkflowTypesActionsGroup>()
    }
}
