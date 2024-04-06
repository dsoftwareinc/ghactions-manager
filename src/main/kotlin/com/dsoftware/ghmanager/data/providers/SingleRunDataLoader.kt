package com.dsoftware.ghmanager.data.providers

import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import java.util.EventListener

class SingleRunDataLoader(private val requestExecutor: GithubApiRequestExecutor) : Disposable {
    private val progressManager = ProgressManager.getInstance()
    private val invalidationEventDispatcher = EventDispatcher.create(DataInvalidatedListener::class.java)

    private val cache = CacheBuilder.newBuilder()
        .removalListener<String, DataProvider<*>> {}
        .maximumSize(200)
        .build<String, DataProvider<*>>()

    fun getJobLogDataProvider(job: Job): LogDataProvider {
        return cache.get("${job.url}/logs") {
            LogDataProvider(progressManager, requestExecutor, job)
        } as LogDataProvider
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
        cache.invalidateAll()
        invalidationEventDispatcher.multicaster.dataLoadeInvalidated()
    }

    fun addInvalidationListener(disposable: Disposable, listener: () -> Unit) =
        invalidationEventDispatcher.addListener(object : DataInvalidatedListener {
            override fun dataLoadeInvalidated() {
                listener()
            }
        }, disposable)

    override fun dispose() {
        invalidateAllData()
    }

    private interface DataInvalidatedListener : EventListener {
        fun dataLoadeInvalidated()
    }
}