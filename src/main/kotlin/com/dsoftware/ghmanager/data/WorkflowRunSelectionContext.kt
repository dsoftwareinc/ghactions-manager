package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.GhApiRequestExecutor
import com.dsoftware.ghmanager.api.WorkflowRunFilter
import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.dsoftware.ghmanager.data.providers.DataProvider
import com.dsoftware.ghmanager.data.providers.JobsDataProvider
import com.dsoftware.ghmanager.data.providers.LogDataProvider
import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.dsoftware.ghmanager.ui.panels.wfruns.LoadingErrorHandler
import com.google.common.cache.CacheBuilder
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.util.EventDispatcher
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.data.GHUser
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GithubUrlUtil
import java.util.EventListener
import java.util.concurrent.ScheduledFuture


class WorkflowRunSelectionContext internal constructor(
    parentDisposable: CheckedDisposable,
    val toolWindow: ToolWindow,
    val account: GithubAccount,
    val repositoryMapping: GHGitRepositoryMapping,
    val requestExecutor: GhApiRequestExecutor,
    val runSelectionHolder: WorkflowRunListSelectionHolder = WorkflowRunListSelectionHolder(),
    val jobSelectionHolder: JobListSelectionHolder = JobListSelectionHolder(),
) : Disposable.Parent {
    private val task: ScheduledFuture<*>

    val currentBranchName: String?
        get() = repositoryMapping.gitRepository.currentBranchName
    val runsListLoader: WorkflowRunListLoader

    private val invalidationEventDispatcher = EventDispatcher.create(DataInvalidatedListener::class.java)

    private val cache = CacheBuilder.newBuilder()
        .removalListener<String, DataProvider<*>> {}
        .maximumSize(200)
        .build<String, DataProvider<*>>()

    var selectedRunDisposable = Disposer.newDisposable("Selected run disposable")
    val jobDataProviderLoadModel: SingleValueModel<JobsDataProvider?> = SingleValueModel(null)
    val jobsDataProvider: JobsDataProvider?
        get() = runSelectionHolder.selection?.let { getJobsDataProvider(it) }

    var selectedJobDisposable = Disposer.newDisposable("Selected job disposable")
    val logDataProviderLoadModel: SingleValueModel<LogDataProvider?> = SingleValueModel(null)
    val logDataProvider: LogDataProvider?
        get() = jobSelectionHolder.selection?.let { getLogDataProvider(it) }


    init {
        Disposer.register(parentDisposable, this)
        val fullPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(repositoryMapping.remote.url)
            ?: throw IllegalArgumentException(
                "Invalid GitHub Repository URL - ${repositoryMapping.remote.url} is not a GitHub repository"
            )
        runsListLoader = WorkflowRunListLoader(
            toolWindow,
            this,
            requestExecutor,
            RepositoryCoordinates(account.server, fullPath),
            WorkflowRunFilter(),
        )
        task = ToolbarUtil.executeTaskAtSettingsFrequency(toolWindow.project) {
            if (runSelectionHolder.selection == null) {
                return@executeTaskAtSettingsFrequency
            }
            LOG.info("Checking updated status for $runSelectionHolder.selection.id")
            if (runSelectionHolder.selection?.status != "completed") {
                jobsDataProvider?.reload()
            }
            if (jobSelectionHolder.selection?.status != "completed") {
                logDataProvider?.reload()
            }
        }
        runSelectionHolder.addSelectionChangeListener(this) {
            LOG.debug("runSelectionHolder selection change listener")
            setNewJobsProvider()
            setNewLogProvider()
            selectedRunDisposable.dispose()
            selectedRunDisposable = Disposer.newDisposable("Selected run disposable")
        }
        jobSelectionHolder.addSelectionChangeListener(this) {
            LOG.debug("jobSelectionHolder selection change listener")
            setNewLogProvider()
            selectedJobDisposable.dispose()
            selectedJobDisposable = Disposer.newDisposable("Selected job disposable")
        }

    }


    private fun getLogDataProvider(job: Job): LogDataProvider {
        return cache.get("${job.url}/logs") { LogDataProvider(requestExecutor, job) } as LogDataProvider
    }

    private fun getJobsDataProvider(workflowRun: WorkflowRun): JobsDataProvider {
        return cache.get(workflowRun.jobsUrl) {
            JobsDataProvider(requestExecutor, workflowRun.jobsUrl)
        } as JobsDataProvider
    }

    fun <T> createDataProvider(request: GithubApiRequest<T>): DataProvider<T> {
        return DataProvider(requestExecutor, request)
    }

    fun getCurrentAccountGHUser(): GHUser {
        return runsListLoader.repoCollaborators.first { user -> user.shortName == account.name }
    }

    private fun setNewJobsProvider() {
        val oldValue = jobDataProviderLoadModel.value
        val newValue = jobsDataProvider
        if (oldValue != newValue && newValue != null && oldValue?.url() != newValue.url()) {
            jobDataProviderLoadModel.value = newValue
            jobSelectionHolder.selection = null
        }
    }

    private fun setNewLogProvider() {
        val oldValue = logDataProviderLoadModel.value
        val newValue = logDataProvider
        if (oldValue != newValue && oldValue?.url() != newValue?.url()) {
            logDataProviderLoadModel.value = newValue
        }
    }

    fun resetAllData() {
        runsListLoader.reset()
        runsListLoader.loadMore(true)
        jobDataProviderLoadModel.value = null
        runSelectionHolder.selection = null
        jobSelectionHolder.selection = null
        selectedRunDisposable.dispose()
        selectedJobDisposable.dispose()
    }

    companion object {
        private val LOG = logger<WorkflowRunSelectionContext>()
    }

    fun getLoadingErrorHandler(resetRunnable: () -> Unit): LoadingErrorHandler {
        return LoadingErrorHandler(toolWindow.project, account, resetRunnable)
    }

    override fun dispose() {}

    override fun beforeTreeDispose() {
        task.cancel(true)
    }

    fun updateFilter(filter: WorkflowRunFilter) {
        runsListLoader.setFilter(filter)
        resetAllData()
    }


    fun addInvalidationListener(disposable: Disposable, listener: () -> Unit) =
        invalidationEventDispatcher.addListener(object : DataInvalidatedListener {
            override fun dataLoadersInvalidated() {
                listener()
            }
        }, disposable)

    private interface DataInvalidatedListener : EventListener {
        fun dataLoadersInvalidated()
    }
}