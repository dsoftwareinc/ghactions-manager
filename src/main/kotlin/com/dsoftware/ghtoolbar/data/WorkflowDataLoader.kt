package com.dsoftware.ghtoolbar.workflow.data

import com.dsoftware.ghtoolbar.data.WorkflowRunJobsDataProvider
import com.dsoftware.ghtoolbar.data.WorkflowRunLogsDataProvider
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import java.util.EventListener

class WorkflowDataLoader(
    private val requestExecutor: GithubApiRequestExecutor
) : Disposable {

    private var isDisposed = false
    private val progressManager = ProgressManager.getInstance()
    private val logsCache = CacheBuilder.newBuilder()
        .removalListener<String, WorkflowRunLogsDataProvider> {
            runInEdt { invalidationEventDispatcher.multicaster.providerChanged(it.key!!) }
        }
        .maximumSize(200)
        .build<String, WorkflowRunLogsDataProvider>()
    private val jobsCache = CacheBuilder.newBuilder()
        .removalListener<String, WorkflowRunJobsDataProvider> {
            runInEdt { invalidationEventDispatcher.multicaster.providerChanged(it.key!!) }
        }
        .maximumSize(200)
        .build<String, WorkflowRunJobsDataProvider>()

    private val invalidationEventDispatcher = EventDispatcher.create(DataInvalidatedListener::class.java)

    fun getLogsDataProvider(url: String): WorkflowRunLogsDataProvider {
        if (isDisposed) throw IllegalStateException("Already disposed")

        return logsCache.get(url) {
            WorkflowRunLogsDataProvider(progressManager, requestExecutor, url)
        }
    }

    fun getJobsDataProvider(url: String): WorkflowRunJobsDataProvider {
        if (isDisposed) throw IllegalStateException("Already disposed")

        return jobsCache.get(url) {
            WorkflowRunJobsDataProvider(progressManager, requestExecutor, url)
        }
    }

    @RequiresEdt
    fun invalidateAllData() {
        LOG.info("All cache invalidated")
        logsCache.invalidateAll()
    }

    private interface DataInvalidatedListener : EventListener {
        fun providerChanged(url: String)
    }

    fun addInvalidationListener(disposable: Disposable, listener: (String) -> Unit) =
        invalidationEventDispatcher.addListener(object : DataInvalidatedListener {
            override fun providerChanged(url: String) {
                listener(url)
            }
        }, disposable)

    override fun dispose() {
        LOG.info("Disposing...")
        invalidateAllData()
        isDisposed = true
    }

    companion object {
        private val LOG = logger<WorkflowDataLoader>()
    }
}