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
    val url: String,
    val errorValue: T,
) {
    private val runChangesEventDispatcher = EventDispatcher.create(WorkflowRunChangedListener::class.java)

    protected val value: LazyCancellableBackgroundProcessValue<T> = backingValue {
        try {
            LOG.debug("Executing $url")
            val request = buildRequest(url)
            val response = requestExecutor.execute(it, request)
            response
        } catch (ioe: IOException) {
            LOG.warn("Error when getting $url: $ioe")
            errorValue
        }
    }

    val request by backgroundProcessValue(value)
    abstract fun buildRequest(url: String): GithubApiRequest<T>

    private fun <T> backgroundProcessValue(backingValue: LazyCancellableBackgroundProcessValue<T>): ReadOnlyProperty<Any?, CompletableFuture<T>> =
        ReadOnlyProperty { _, _ -> backingValue.value }


    @RequiresEdt
    fun reloadLog() {
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

class WorkflowRunLogsDataProvider(
    progressManager: ProgressManager,
    requestExecutor: GithubApiRequestExecutor,
    logsUrl: String,
) : DataProvider<Map<String, String>>(
    progressManager,
    requestExecutor,
    logsUrl,
    emptyMap()
) {
    override fun buildRequest(url: String) = Workflows.getDownloadUrlForWorkflowLog(url)

}


//TODO Use this as replacement for logs when logs are too big
class WorkflowRunJobsDataProvider(
    progressManager: ProgressManager,
    requestExecutor: GithubApiRequestExecutor,
    jobsUrl: String
) : DataProvider<WorkflowRunJobs>(
    progressManager,
    requestExecutor,
    jobsUrl,
    WorkflowRunJobs(0, emptyList())
) {
    override fun buildRequest(url: String) = Workflows.getWorkflowRunJobs(url)
}