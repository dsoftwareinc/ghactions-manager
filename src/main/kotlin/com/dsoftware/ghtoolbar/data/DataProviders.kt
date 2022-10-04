package com.dsoftware.ghtoolbar.data

import WorkflowRunJobs
import com.dsoftware.ghtoolbar.api.Workflows
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
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

abstract class DataProvider<T>(
    private val progressManager: ProgressManager,
    private val requestExecutor: GithubApiRequestExecutor,
    private val githubApiRequest: GithubApiRequest<T>,
    private val errorValue: T?,
) {
    private val runChangesEventDispatcher = EventDispatcher.create(WorkflowRunChangedListener::class.java)

    protected val value: LazyCancellableBackgroundProcessValue<T> = backingValue {
        try {
            LOG.debug("Executing ${githubApiRequest.url}")
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
        LOG.debug("reloadLog()")
        value.drop()
        runChangesEventDispatcher.multicaster.changed()
    }

    private fun <T> backingValue(supplier: (ProgressIndicator) -> T) =
        LazyCancellableBackgroundProcessValue.create(progressManager) {
            supplier(it)
        }

    fun addRunChangesListener(disposable: Disposable, listener: WorkflowRunChangedListener) =
        runChangesEventDispatcher.addListener(listener, disposable)

    interface WorkflowRunChangedListener : EventListener {
        fun changed() {}
    }

    companion object {
        private val LOG = thisLogger()
    }
}

class DefaultDataProvider<T>(
    progressManager: ProgressManager,
    requestExecutor: GithubApiRequestExecutor,
    githubApiRequest: GithubApiRequest<T>
) : DataProvider<T>(progressManager, requestExecutor, githubApiRequest, null)

class WorkflowRunLogsDataProvider(
    progressManager: ProgressManager,
    requestExecutor: GithubApiRequestExecutor,
    logsUrl: String,
) : DataProvider<Map<String, String>>(
    progressManager,
    requestExecutor,
    Workflows.getDownloadUrlForWorkflowLog(logsUrl),
    emptyMap()
)


class WorkflowRunJobsDataProvider(
    progressManager: ProgressManager,
    requestExecutor: GithubApiRequestExecutor,
    jobsUrl: String
) : DataProvider<WorkflowRunJobs>(
    progressManager,
    requestExecutor,
    Workflows.getWorkflowRunJobs(jobsUrl),
    WorkflowRunJobs(0, emptyList())
)