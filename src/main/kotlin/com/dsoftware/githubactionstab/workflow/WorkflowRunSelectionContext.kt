package com.dsoftware.githubactionstab.workflow

import com.dsoftware.githubactionstab.api.GitHubWorkflowRun
import com.dsoftware.githubactionstab.workflow.data.WorkflowRunDataContext
import com.dsoftware.githubactionstab.workflow.data.WorkflowRunDataProvider
import com.intellij.openapi.diagnostic.logger

class WorkflowRunSelectionContext internal constructor(
    private val dataContext: WorkflowRunDataContext,
    private val selectionHolder: WorkflowRunListSelectionHolder
) {

    fun resetAllData() {
        LOG.debug("resetAllData")
        dataContext.listLoader.reset()
        dataContext.dataLoader.invalidateAllData()
    }

    val workflowRun: GitHubWorkflowRun?
        get() = selectionHolder.selection

    val workflowRunDataProvider: WorkflowRunDataProvider?
        get() = workflowRun?.let { dataContext.dataLoader.getDataProvider(it.logs_url) }

    companion object {
        private val LOG = logger<WorkflowRunSelectionContext>()
    }
}