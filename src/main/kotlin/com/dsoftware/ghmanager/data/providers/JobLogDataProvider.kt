package com.dsoftware.ghmanager.data.providers

import com.dsoftware.ghmanager.api.GithubApi
import com.dsoftware.ghmanager.api.model.Job
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor

class JobLogDataProvider(
    progressManager: ProgressManager,
    requestExecutor: GithubApiRequestExecutor,
    job: Job
) : DataProvider<String>(
    progressManager,
    requestExecutor,
    GithubApi.getJobLog(job),
    null
)