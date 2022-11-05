package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.google.common.cache.CacheBuilder
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import java.util.EventListener
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


class SingleRunDataLoader(
    private val requestExecutor: GithubApiRequestExecutor
) : Disposable {

    private val cache = CacheBuilder.newBuilder()
        .removalListener<String, DataProvider<*>> {
            runInEdt { invalidationEventDispatcher.multicaster.providerChanged(it.key!!) }
        }
        .maximumSize(200)
        .build<String, DataProvider<*>>()


    private val invalidationEventDispatcher = EventDispatcher.create(DataInvalidatedListener::class.java)

    fun getLogsDataProvider(workflowRun: WorkflowRun): WorkflowRunLogsDataProvider {

        return cache.get(workflowRun.logs_url) {
            WorkflowRunLogsDataProvider(progressManager, requestExecutor, workflowRun.logs_url)
        } as WorkflowRunLogsDataProvider

    }

    fun getJobsDataProvider(workflowRun: WorkflowRun): WorkflowRunJobsDataProvider {

        return cache.get(workflowRun.jobs_url) {
            WorkflowRunJobsDataProvider(progressManager, requestExecutor, workflowRun.jobs_url)
        } as WorkflowRunJobsDataProvider
    }

    fun <T> createDataProvider(request: GithubApiRequest<T>): DataProvider<T> {
        return DefaultDataProvider(progressManager, requestExecutor, request)
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

open class ListSelectionHolder<T> {

    @get:RequiresEdt
    @set:RequiresEdt
    var selection: T? by Delegates.observable(null) { _, _, _ ->
        selectionChangeEventDispatcher.multicaster.eventOccurred()
    }

    private val selectionChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

    @RequiresEdt
    fun addSelectionChangeListener(disposable: Disposable, listener: () -> Unit) =
        SimpleEventListener.addDisposableListener(selectionChangeEventDispatcher, disposable, listener)
}

class WorkflowRunListSelectionHolder : ListSelectionHolder<WorkflowRun>()
class JobListSelectionHolder : ListSelectionHolder<Job>()


class WorkflowRunSelectionContext internal constructor(
    parentDisposable: Disposable,
    val dataLoader: SingleRunDataLoader,
    val runsListLoader: WorkflowRunListLoader,
    val repositoryMapping: GHGitRepositoryMapping,
    val runSelectionHolder: WorkflowRunListSelectionHolder = WorkflowRunListSelectionHolder(),
    val jobSelectionHolder: JobListSelectionHolder = JobListSelectionHolder(),
) : Disposable {
    private val frequency: Long = runsListLoader.frequency
    private val task: ScheduledFuture<*>
    val runsListModel: CollectionListModel<WorkflowRun>
        get() = runsListLoader.listModel
    private val workflowRun: WorkflowRun?
        get() = runSelectionHolder.selection
    val jobDataProviderLoadModel: SingleValueModel<WorkflowRunJobsDataProvider?> = SingleValueModel(null)
    val logDataProviderLoadModel: SingleValueModel<WorkflowRunLogsDataProvider?> = SingleValueModel(null)

    val logsDataProvider: WorkflowRunLogsDataProvider?
        get() = workflowRun?.let { dataLoader.getLogsDataProvider(it) }
    val jobsDataProvider: WorkflowRunJobsDataProvider?
        get() = workflowRun?.let { dataLoader.getJobsDataProvider(it) }

    init {
        Disposer.register(parentDisposable, dataLoader)
        Disposer.register(parentDisposable, runsListLoader)
        runSelectionHolder.addSelectionChangeListener(parentDisposable) {
            LOG.debug("runSelectionHolder selection change listener")
            setNewJobsProvider()
            setNewLogProvider()
        }
        dataLoader.addInvalidationListener(parentDisposable) {
            LOG.debug("invalidation listener")
            setNewJobsProvider()
            setNewLogProvider()
        }
        val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
        task = scheduler.scheduleWithFixedDelay({
            runInEdt {
                val status = workflowRun?.status
                if (status != "completed") {
                    jobsDataProvider?.reload()
                    logsDataProvider?.reload()
                }
            }
        }, 1, frequency, TimeUnit.SECONDS)
    }

    private fun setNewJobsProvider() {
        val oldValue = jobDataProviderLoadModel.value
        val newValue = jobsDataProvider
        if (newValue != oldValue) {
            jobSelectionHolder.selection = null
        }
        if (oldValue != newValue && newValue != null && oldValue?.url() != newValue.url()) {
            jobDataProviderLoadModel.value = newValue
        }
    }

    private fun setNewLogProvider() {
        val oldValue = logDataProviderLoadModel.value
        val newValue = logsDataProvider
        if (oldValue != newValue && newValue != null && oldValue?.url() != newValue.url()) {
            logDataProviderLoadModel.value = newValue
        }
    }

    fun resetAllData() {
        LOG.debug("resetAllData")
        runsListLoader.reset()
        runsListLoader.loadMore()
        dataLoader.invalidateAllData()
    }

    companion object {
        private val LOG = logger<WorkflowRunSelectionContext>()
    }

    override fun dispose() {
        task.cancel(true)
    }
}