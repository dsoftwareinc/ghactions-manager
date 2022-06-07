package com.dsoftware.githubactionstab.workflow.data

import com.google.common.cache.CacheBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.util.*

class WorkflowDataLoader(private val dataProviderFactory: (String) -> WorkflowRunDataProvider) :
    Disposable {

    private var isDisposed = false
    private val cache = CacheBuilder.newBuilder()
        .removalListener<String, WorkflowRunDataProvider> {
            runInEdt { invalidationEventDispatcher.multicaster.providerChanged(it.key!!) }
        }
        .maximumSize(200)
        .build<String, WorkflowRunDataProvider>()

    private val invalidationEventDispatcher = EventDispatcher.create(DataInvalidatedListener::class.java)

    fun getDataProvider(url: String): WorkflowRunDataProvider {
        if (isDisposed) throw IllegalStateException("Already disposed")

        return cache.get(url) {
            dataProviderFactory(url)
        }
    }

    @RequiresEdt
    fun invalidateAllData() {
        LOG.info("All cache invalidated")
        cache.invalidateAll()
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
        private val LOG = thisLogger()
    }
}