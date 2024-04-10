package com.dsoftware.ghmanager.data.providers

import com.dsoftware.ghmanager.api.GhApiRequestExecutor
import com.dsoftware.ghmanager.api.GithubApi
import com.dsoftware.ghmanager.api.model.Job

class LogDataProvider(
    requestExecutor: GhApiRequestExecutor,
    job: Job
) : DataProvider<String>(
    requestExecutor,
    GithubApi.getLogForSingleJob(job)
)