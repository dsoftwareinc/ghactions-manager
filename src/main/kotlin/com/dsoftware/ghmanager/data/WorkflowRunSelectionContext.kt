package com.dsoftware.ghmanager.data

import WorkflowRunJob
import com.dsoftware.ghmanager.api.model.GitHubWorkflowRun
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
import org.jetbrains.plugins.github.api.GHRepositoryPath
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.GithubServerPath
import java.util.EventListener
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


class SingleRunDataLoader(
    private val requestExecutor: GithubApiRequestExecutor
) : Disposable {

    private var isDisposed = false
    private val cache = CacheBuilder.newBuilder()
        .removalListener<String, DataProvider<*>> {
            runInEdt { invalidationEventDispatcher.multicaster.providerChanged(it.key!!) }
        }
        .maximumSize(200)
        .build<String, DataProvider<*>>()


    private val invalidationEventDispatcher = EventDispatcher.create(DataInvalidatedListener::class.java)

    fun getLogsDataProvider(workflowRun: GitHubWorkflowRun): WorkflowRunLogsDataProvider {
        if (isDisposed) throw IllegalStateException("Already disposed")

        return cache.get(workflowRun.logs_url) {
            WorkflowRunLogsDataProvider(progressManager, requestExecutor, workflowRun.logs_url)
        } as WorkflowRunLogsDataProvider

    }

    fun getJobsDataProvider(workflowRun: GitHubWorkflowRun): WorkflowRunJobsDataProvider {
        if (isDisposed) throw IllegalStateException("Already disposed")

        return cache.get(workflowRun.jobs_url) {
            WorkflowRunJobsDataProvider(progressManager, requestExecutor, workflowRun.jobs_url)
        } as WorkflowRunJobsDataProvider
    }

    fun <T> createDataProvider(request: GithubApiRequest<T>): DataProvider<T> {
        if (isDisposed) throw IllegalStateException("Already disposed")
        return DefaultDataProvider(progressManager, requestExecutor, request)
    }

    @RequiresEdt
    fun invalidateAllData() {
        LOG.debug("All cache invalidated")
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
        LOG.debug("Disposing...")
        invalidateAllData()
        isDisposed = true
    }

    companion object {
        private val LOG = logger<SingleRunDataLoader>()
        private val progressManager = ProgressManager.getInstance()
    }
}

data class RepositoryCoordinates(val serverPath: GithubServerPath, val repositoryPath: GHRepositoryPath) {

    override fun toString(): String {
        return "$serverPath/$repositoryPath"
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

class WorkflowRunListSelectionHolder : ListSelectionHolder<GitHubWorkflowRun>()
class JobListSelectionHolder : ListSelectionHolder<WorkflowRunJob>()


class WorkflowRunSelectionContext internal constructor(
    parentDisposable: Disposable,
    val dataLoader: SingleRunDataLoader,
    val runsListLoader: WorkflowRunListLoader,
    val runSelectionHolder: WorkflowRunListSelectionHolder = WorkflowRunListSelectionHolder(),
    val jobSelectionHolder: JobListSelectionHolder = JobListSelectionHolder(),
) : Disposable {
    val jobDataProviderLoadModel: SingleValueModel<WorkflowRunJobsDataProvider?> = SingleValueModel(null)
    val logDataProviderLoadModel: SingleValueModel<WorkflowRunLogsDataProvider?> = SingleValueModel(null)
    private val frequency: Long = runsListLoader.frequency
    private val task: ScheduledFuture<*>
    val runsListModel: CollectionListModel<GitHubWorkflowRun>
        get() = runsListLoader.listModel

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
            if (workflowRun?.status == "in_progress") runInEdt {
                jobsDataProvider?.reload()
                logsDataProvider?.reload()
            }
        }, 1, frequency, TimeUnit.SECONDS)
    }

    private fun setNewJobsProvider() {
        val oldValue = jobDataProviderLoadModel.value
        val newValue = jobsDataProvider
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

    private val workflowRun: GitHubWorkflowRun?
        get() = runSelectionHolder.selection

    val logsDataProvider: WorkflowRunLogsDataProvider?
        get() = workflowRun?.let { dataLoader.getLogsDataProvider(it) }
    val jobsDataProvider: WorkflowRunJobsDataProvider?
        get() = workflowRun?.let { dataLoader.getJobsDataProvider(it) }

    companion object {
        private val LOG = logger<WorkflowRunSelectionContext>()
    }

    override fun dispose() {
        task.cancel(true)
    }
}