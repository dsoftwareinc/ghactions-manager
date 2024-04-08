package com.dsoftware.ghmanager.data.providers

import com.dsoftware.ghmanager.api.GhApiRequestExecutor
import com.dsoftware.ghmanager.api.GithubApi
import com.dsoftware.ghmanager.api.model.WorkflowRunJobs

class JobsDataProvider(
    requestExecutor: GhApiRequestExecutor,
    jobsUrl: String
) : DataProvider<WorkflowRunJobs>(
    requestExecutor,
    GithubApi.getWorkflowRunJobs(jobsUrl),
    WorkflowRunJobs(0, emptyList())
)