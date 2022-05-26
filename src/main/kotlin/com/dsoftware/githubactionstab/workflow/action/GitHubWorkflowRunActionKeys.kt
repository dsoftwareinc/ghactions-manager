package com.dsoftware.githubactionstab.workflow.action

import com.dsoftware.githubactionstab.api.GitHubWorkflowRun
import com.dsoftware.githubactionstab.workflow.GitHubWorkflowRunSelectionContext
import com.intellij.openapi.actionSystem.DataKey

object GitHubWorkflowRunActionKeys {
    @JvmStatic
    val SELECTED_WORKFLOW_RUN =
        DataKey.create<GitHubWorkflowRun>("com.dsoftware.githubactionstab.workflow.list.selected")

    @JvmStatic
    val ACTION_DATA_CONTEXT =
        DataKey.create<GitHubWorkflowRunSelectionContext>("com.dsoftware.githubactionstab.workflowrun.action.datacontext")
}