package com.dsoftware.ghmanager.data.providers

import com.dsoftware.ghmanager.api.GithubApi
import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor

class JobsDataProvider(
    requestExecutor: GithubApiRequestExecutor,
    jobsUrl: String
) : DataProvider<WorkflowRunJobs>(
    requestExecutor,
    GithubApi.getWorkflowRunJobs(jobsUrl),
    WorkflowRunJobs(0, emptyList())
)