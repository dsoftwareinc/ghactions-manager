package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.GitHubLog
import com.dsoftware.ghmanager.api.GithubApi
import com.dsoftware.ghmanager.api.model.JobsList
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.util.LazyCancellableBackgroundProcessValue
import java.io.IOException
import java.util.EventListener
import java.util.concurrent.CompletableFuture
import kotlin.properties.ReadOnlyProperty

open class DataProvider<T>(
    private val progressManager: ProgressManager,
    private val requestExecutor: GithubApiRequestExecutor,
    private val githubApiRequest: GithubApiRequest<T>,
    private val errorValue: T?,
) {
    private val runChangesEventDispatcher = EventDispatcher.create(DataProviderChangeListener::class.java)

    private val value: LazyCancellableBackgroundProcessValue<T> =
        LazyCancellableBackgroundProcessValue.create(progressManager) {
            try {
                LOG.info("Executing ${githubApiRequest.url}")
                val request = githubApiRequest
                val response = requestExecutor.execute(it, request)
                response
            } catch (ioe: IOException) {
                LOG.warn("Error when getting $githubApiRequest.url: $ioe")
                errorValue ?: throw ioe
            }
        }

    val request by backgroundProcessValue(value)

    private fun <T> backgroundProcessValue(backingValue: LazyCancellableBackgroundProcessValue<T>)
        : ReadOnlyProperty<Any?, CompletableFuture<T>> =
        ReadOnlyProperty { _, _ -> backingValue.value }

    fun url(): String = githubApiRequest.url

    @RequiresEdt
    fun reload() {
        value.drop()
        runChangesEventDispatcher.multicaster.changed()
    }

    fun addRunChangesListener(disposable: Disposable, listener: DataProviderChangeListener) =
        runChangesEventDispatcher.addListener(listener, disposable)

    interface DataProviderChangeListener : EventListener {
        fun changed() {}
    }

    companion object {
        private val LOG = thisLogger()
    }
}


class WorkflowRunLogsDataProvider(
    progressManager: ProgressManager,
    requestExecutor: GithubApiRequestExecutor,
    logsUrl: String,
) : DataProvider<GitHubLog>(
    progressManager,
    requestExecutor,
    GithubApi.getDownloadUrlForWorkflowLog(logsUrl),
    emptyMap()
)


class WorkflowRunJobsDataProvider(
    progressManager: ProgressManager,
    requestExecutor: GithubApiRequestExecutor,
    jobsUrl: String
) : DataProvider<JobsList>(
    progressManager,
    requestExecutor,
    GithubApi.getWorkflowRunJobs(jobsUrl),
    JobsList(0, emptyList())
)