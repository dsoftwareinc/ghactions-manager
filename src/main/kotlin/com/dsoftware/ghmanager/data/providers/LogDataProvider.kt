package com.dsoftware.ghmanager.data.providers

import com.dsoftware.ghmanager.api.GithubApi
import com.dsoftware.ghmanager.api.model.Job
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor

class LogDataProvider(
    requestExecutor: GithubApiRequestExecutor,
    job: Job
) : DataProvider<String>(
    requestExecutor,
    GithubApi.getLogForSingleJob(job),
    null
)