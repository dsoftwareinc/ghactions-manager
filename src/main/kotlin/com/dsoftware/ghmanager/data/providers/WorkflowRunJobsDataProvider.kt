package com.dsoftware.ghmanager.data.providers

import com.dsoftware.ghmanager.api.GithubApi
import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor

class WorkflowRunJobsDataProvider(
    progressManager: ProgressManager,
    requestExecutor: GithubApiRequestExecutor,
    jobsUrl: String
) : DataProvider<WorkflowRunJobs>(
    progressManager,
    requestExecutor,
    GithubApi.getWorkflowRunJobs(jobsUrl),
    WorkflowRunJobs(0, emptyList())
)