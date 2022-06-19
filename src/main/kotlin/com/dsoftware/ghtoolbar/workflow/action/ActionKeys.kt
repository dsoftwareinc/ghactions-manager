package com.dsoftware.ghtoolbar.workflow.action

import com.dsoftware.ghtoolbar.api.model.GitHubWorkflowRun
import com.dsoftware.ghtoolbar.workflow.WorkflowRunSelectionContext
import com.intellij.openapi.actionSystem.DataKey

object ActionKeys {
    @JvmStatic
    val SELECTED_WORKFLOW_RUN =
        DataKey.create<GitHubWorkflowRun>("com.dsoftware.githubactionstab.workflow.list.selected")

    @JvmStatic
    val ACTION_DATA_CONTEXT =
        DataKey.create<WorkflowRunSelectionContext>("com.dsoftware.githubactionstab.workflowrun.action.datacontext")
}