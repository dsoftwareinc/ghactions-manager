package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import java.util.EventListener

class SingleRunDataLoader(
    private val requestExecutor: GithubApiRequestExecutor
) : Disposable {
    private val invalidationEventDispatcher = EventDispatcher.create(DataInvalidatedListener::class.java)

    private val cache = CacheBuilder.newBuilder()
        .removalListener<String, DataProvider<*>> {
            invalidationEventDispatcher.multicaster.providerChanged(it.key!!)
        }
        .maximumSize(200)
        .build<String, DataProvider<*>>()


//    fun getLogsDataProvider(workflowRun: WorkflowRun): WorkflowRunLogsDataProvider {
//        return cache.get(workflowRun.logsUrl) {
//            WorkflowRunLogsDataProvider(progressManager, requestExecutor, workflowRun.logsUrl)
//        } as WorkflowRunLogsDataProvider
//    }

    fun getJobLogDataProvider(job: Job): JobLogDataProvider {
        return cache.get("${job.url}/logs") {
            JobLogDataProvider(progressManager, requestExecutor, "${job.url}/logs")
        } as JobLogDataProvider
    }

    fun getJobsDataProvider(workflowRun: WorkflowRun): WorkflowRunJobsDataProvider {
        return cache.get(workflowRun.jobsUrl) {
            WorkflowRunJobsDataProvider(progressManager, requestExecutor, workflowRun.jobsUrl)
        } as WorkflowRunJobsDataProvider
    }

    fun <T> createDataProvider(request: GithubApiRequest<T>): DataProvider<T> {
        return DataProvider(progressManager, requestExecutor, request, null)
    }

    @RequiresEdt
    fun invalidateAllData() {
        LOG.debug("All cache invalidated")
        cache.invalidateAll()
    }

    fun addInvalidationListener(disposable: Disposable, listener: (String) -> Unit) =
        invalidationEventDispatcher.addListener(object : DataInvalidatedListener {
            override fun providerChanged(url: String) {
                listener(url)
            }
        }, disposable)

    override fun dispose() {
        invalidateAllData()
    }

    companion object {
        private val LOG = logger<SingleRunDataLoader>()
        private val progressManager = ProgressManager.getInstance()
    }

    private interface DataInvalidatedListener : EventListener {
        fun providerChanged(url: String)
    }
}