package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates



class WorkflowRunSelectionContext internal constructor(
    parentDisposable: Disposable,
    val dataLoader: SingleRunDataLoader,
    val runsListLoader: WorkflowRunListLoader,
    val repositoryMapping: GHGitRepositoryMapping,
    val runSelectionHolder: WorkflowRunListSelectionHolder = WorkflowRunListSelectionHolder(),
    val jobSelectionHolder: JobListSelectionHolder = JobListSelectionHolder(),
) : Disposable {
    private val frequency: Long = runsListLoader.frequency()
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
        Disposer.register(parentDisposable, this)
        runSelectionHolder.addSelectionChangeListener(parentDisposable) {
            LOG.debug("runSelectionHolder selection change listener")
            setNewJobsProvider()
            setNewLogProvider()
        }
        dataLoader.addInvalidationListener(this) {
            LOG.debug("invalidation listener")
            jobDataProviderLoadModel.value = null
            logDataProviderLoadModel.value = null
        }
        val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
        task = scheduler.scheduleWithFixedDelay({
            LOG.info("Checking updated status for ${workflowRun}")
            val status = workflowRun?.status
            if (workflowRun != null && status != "completed") {
                jobsDataProvider?.reload()
                logsDataProvider?.reload()
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