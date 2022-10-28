package com.dsoftware.ghmanager.actions

import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.GitHubWorkflowRun
import com.dsoftware.ghmanager.data.WorkflowRunSelectionContext
import com.intellij.openapi.actionSystem.DataKey

object ActionKeys {
    @JvmStatic
    val SELECTED_WORKFLOW_RUN =
        DataKey.create<GitHubWorkflowRun>("com.dsoftware.githubactionstab.workflow.list.selected")

    @JvmStatic
    val SELECTED_WORKFLOW_RUN_FILEPATH =
        DataKey.create<String>("com.dsoftware.githubactionstab.workflow.list.selected.path")

    @JvmStatic
    val SELECTED_JOB =
        DataKey.create<Job>("com.dsoftware.githubactionstab.workflow.jobs.selected")

    @JvmStatic
    val ACTION_DATA_CONTEXT =
        DataKey.create<WorkflowRunSelectionContext>("com.dsoftware.githubactionstab.workflowrun.action.datacontext")
}