package com.dsoftware.githubactionstab.workflow.action

import com.dsoftware.githubactionstab.api.GitHubWorkflowRun
import com.dsoftware.githubactionstab.workflow.WorkflowRunSelectionContext
import com.intellij.openapi.actionSystem.DataKey

object WorkflowRunActionKeys {
    @JvmStatic
    val SELECTED_WORKFLOW_RUN =
        DataKey.create<GitHubWorkflowRun>("com.dsoftware.githubactionstab.workflow.list.selected")

    @JvmStatic
    val ACTION_DATA_CONTEXT =
        DataKey.create<WorkflowRunSelectionContext>("com.dsoftware.githubactionstab.workflowrun.action.datacontext")
}