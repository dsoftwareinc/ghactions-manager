package com.dsoftware.githubactionstab.workflow

import com.dsoftware.githubactionstab.api.GitHubWorkflowRun
import com.dsoftware.githubactionstab.workflow.data.GitHubWorkflowRunDataContext
import com.dsoftware.githubactionstab.workflow.data.GitHubWorkflowRunDataProvider
import com.intellij.openapi.diagnostic.logger

class GitHubWorkflowRunSelectionContext internal constructor(
    private val dataContext: GitHubWorkflowRunDataContext,
    private val selectionHolder: GitHubWorkflowRunListSelectionHolder
) {

    fun resetAllData() {
        LOG.debug("resetAllData")
        dataContext.listLoader.reset()
        dataContext.dataLoader.invalidateAllData()
    }

    val workflowRun: GitHubWorkflowRun?
        get() = selectionHolder.selection

    val workflowRunDataProvider: GitHubWorkflowRunDataProvider?
        get() = workflowRun?.let { dataContext.dataLoader.getDataProvider(it.logs_url) }

    companion object {
        private val LOG = logger<GitHubWorkflowRunSelectionContext>()
    }
}