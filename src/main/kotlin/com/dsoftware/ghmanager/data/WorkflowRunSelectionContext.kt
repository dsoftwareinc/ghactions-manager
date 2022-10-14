package com.dsoftware.ghmanager.data

import WorkflowRunJob
import com.dsoftware.ghmanager.api.model.GitHubWorkflowRun
import com.dsoftware.ghmanager.workflow.data.WorkflowDataLoader
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.plugins.github.api.GHRepositoryPath
import org.jetbrains.plugins.github.api.GithubServerPath
import javax.swing.ListModel
import kotlin.properties.Delegates

class WorkflowRunDataContext(
    val runsListModel: ListModel<GitHubWorkflowRun>,
    val dataLoader: WorkflowDataLoader,
    val runsListLoader: WorkflowRunListLoader,
) : Disposable {
    override fun dispose() {
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
    val dataContext: WorkflowRunDataContext,
    val runSelectionHolder: WorkflowRunListSelectionHolder = WorkflowRunListSelectionHolder(),
    val jobSelectionHolder: JobListSelectionHolder = JobListSelectionHolder(),
) {
    val jobDataProviderModel: SingleValueModel<WorkflowRunJobsDataProvider?> = SingleValueModel(null)
    val logDataProviderModel: SingleValueModel<WorkflowRunLogsDataProvider?> = SingleValueModel(null)

    init {
        runSelectionHolder.addSelectionChangeListener(parentDisposable) {
            LOG.debug("runSelectionHolder selection change listener")
            setNewJobsProvider()
            setNewLogProvider()
        }
        dataContext.dataLoader.addInvalidationListener(parentDisposable) {
            LOG.debug("invalidation listener")
            setNewJobsProvider()
            setNewLogProvider()
        }

    }

    private fun setNewJobsProvider() {
        val oldJobDataProviderModelValue = jobDataProviderModel.value
        if (oldJobDataProviderModelValue != null && jobsDataProvider != null && oldJobDataProviderModelValue.url() != jobsDataProvider?.url()) {
            jobDataProviderModel.value = null
        }
        jobDataProviderModel.value = jobsDataProvider
    }
    private fun setNewLogProvider() {
        val oldValue = logDataProviderModel.value
        if (oldValue != null && logsDataProvider != null && oldValue.url() != logsDataProvider?.url()) {
            logDataProviderModel.value = null
        }
        logDataProviderModel.value = logsDataProvider
    }

    fun resetAllData() {
        LOG.debug("resetAllData")
        dataContext.runsListLoader.reset()
        dataContext.runsListLoader.loadMore()
        dataContext.dataLoader.invalidateAllData()
    }

    private val workflowRun: GitHubWorkflowRun?
        get() = runSelectionHolder.selection

    val logsDataProvider: WorkflowRunLogsDataProvider?
        get() = workflowRun?.let { dataContext.dataLoader.getLogsDataProvider(it.logs_url) }
    val jobsDataProvider: WorkflowRunJobsDataProvider?
        get() = workflowRun?.let { dataContext.dataLoader.getJobsDataProvider(it.jobs_url) }

    companion object {
        private val LOG = logger<WorkflowRunSelectionContext>()
    }
}