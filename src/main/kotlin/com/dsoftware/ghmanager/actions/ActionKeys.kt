package com.dsoftware.ghmanager.actions

import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.dsoftware.ghmanager.data.WorkflowRunSelectionContext
import com.intellij.openapi.actionSystem.DataKey

object ActionKeys {
    @JvmStatic
    val SELECTED_WORKFLOW_RUN =
        DataKey.create<WorkflowRun>("com.dsoftware.actions.tab.workflow.list.selected")

    @JvmStatic
    val SELECTED_WORKFLOW_RUN_FILEPATH =
        DataKey.create<String>("com.dsoftware.actions.workflow.list.selected.path")

    @JvmStatic
    val SELECTED_JOB =
        DataKey.create<Job>("com.dsoftware.actions.workflow.jobs.selected")

    @JvmStatic
    val SELECTED_WF_CONTEXT =
        DataKey.create<WorkflowRunSelectionContext>("com.dsoftware.ghactions.workflowrun.action.datacontext")
}