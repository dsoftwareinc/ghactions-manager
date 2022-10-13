package com.dsoftware.ghmanager.ui


import WorkflowRunJobs
import com.dsoftware.ghmanager.actions.ActionKeys
import com.dsoftware.ghmanager.data.DataProvider
import com.dsoftware.ghmanager.data.WorkflowDataContextRepository
import com.dsoftware.ghmanager.data.WorkflowRunJobsDataProvider
import com.dsoftware.ghmanager.data.WorkflowRunLogsDataProvider
import com.dsoftware.ghmanager.ui.panels.JobList
import com.dsoftware.ghmanager.ui.panels.LogConsolePanel
import com.dsoftware.ghmanager.ui.panels.WorkflowRunListLoaderPanel
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.dsoftware.ghmanager.ui.settings.GithubActionsManagerSettings
import com.dsoftware.ghmanager.workflow.JobListSelectionHolder
import com.dsoftware.ghmanager.workflow.WorkflowRunDataContext
import com.dsoftware.ghmanager.workflow.WorkflowRunListSelectionHolder
import com.dsoftware.ghmanager.workflow.WorkflowRunSelectionContext
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.ide.DataManager
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.ContextMenuPopupHandler
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.content.Content
import com.intellij.util.ui.UIUtil
import org.jetbrains.plugins.github.api.GithubApiRequestExecutorManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.i18n.GithubBundle
import org.jetbrains.plugins.github.pullrequest.ui.GHApiLoadingErrorHandler
import org.jetbrains.plugins.github.pullrequest.ui.GHCompletableFutureLoadingModel
import org.jetbrains.plugins.github.pullrequest.ui.GHLoadingModel
import org.jetbrains.plugins.github.pullrequest.ui.GHLoadingPanelFactory
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import java.awt.BorderLayout
import javax.swing.JComponent
import kotlin.properties.Delegates

class WorkflowToolWindowTabController(
    private val project: Project,
    repoSettings: GithubActionsManagerSettings.RepoSettings,
    repositoryMapping: GHGitRepositoryMapping,
    ghAccount: GithubAccount,
    private val dataContextRepository: WorkflowDataContextRepository,
    private val tab: Content,
) {
    private val settingsService = GhActionsSettingsService.getInstance(project)
    private val actionManager = ActionManager.getInstance()
    private val mainPanel: JComponent
    private val ghRequestExecutor = GithubApiRequestExecutorManager.getInstance().getExecutor(ghAccount)
    private var contentDisposable by Delegates.observable<Disposable?>(null) { _, oldValue, newValue ->
        if (oldValue != null) Disposer.dispose(oldValue)
        if (newValue != null) Disposer.register(tab.disposer!!, newValue)
    }

    init {
        tab.displayName =
            repoSettings.customName.ifEmpty { repositoryMapping.repositoryPath }
        mainPanel = tab.component.apply {
            layout = BorderLayout()
            background = UIUtil.getListBackground()
        }

        val repository = repositoryMapping.ghRepositoryCoordinates
        val remote = repositoryMapping.gitRemoteUrlCoordinates

        val disposable = Disposer.newDisposable()
        contentDisposable = Disposable {
            Disposer.dispose(disposable)
            dataContextRepository.clearContext(repository)
        }

        val loadingModel = GHCompletableFutureLoadingModel<WorkflowRunDataContext>(disposable).apply {
            future = dataContextRepository.acquireContext(project, repository, remote, ghAccount, ghRequestExecutor)
        }

        val errorHandler = GHApiLoadingErrorHandler(project, ghAccount) {
            val contextRepository = dataContextRepository
            contextRepository.clearContext(repository)
            loadingModel.future =
                contextRepository.acquireContext(project, repository, remote, ghAccount, ghRequestExecutor)
        }
        val panel = GHLoadingPanelFactory(
            loadingModel,
            "Not loading content",
            GithubBundle.message("cannot.load.data.from.github"),
            errorHandler,
        ).create { _, result ->
            LOG.debug("create content")
            val content = createContent(result, ghAccount, disposable)
            LOG.debug("done creating content")

            content
        }

        with(mainPanel) {
            removeAll()
            add(panel, BorderLayout.CENTER)
            revalidate()
            repaint()
        }
    }

    private fun createContent(
        context: WorkflowRunDataContext,
        account: GithubAccount,
        disposable: Disposable,
    ): JComponent {
        val runsSelectionHolder = WorkflowRunListSelectionHolder()
        val jobsSelectionHolder = JobListSelectionHolder()
        val selectionContext = WorkflowRunSelectionContext(context, runsSelectionHolder, jobsSelectionHolder)

        val jobsdataProviderModel = createJobsDataProviderModel(context, runsSelectionHolder, disposable)

        val (jobLoadingModel, jobModel) = createJobsLoadingModel(jobsdataProviderModel, disposable)
        val workflowRunsList = WorkflowRunListLoaderPanel
            .createWorkflowRunsListComponent(selectionContext, disposable)
        val errorHandler = GHApiLoadingErrorHandler(project, account) {
        }
        val jobLoadingPanel = GHLoadingPanelFactory(
            jobLoadingModel,
            "Select a workflow to show list of jobs",
            GithubBundle.message("cannot.load.data.from.github"),
            errorHandler
        ).create { _, _ ->
            createJobPanel(jobModel, selectionContext, settingsService.state)
        }
        val logsDataProviderModel = createLogsDataProviderModel(selectionContext, disposable)
        val (logLoadingModel, logModel) = createLogLoadingModel(logsDataProviderModel, jobsSelectionHolder, disposable)
        val logLoadingPanel = GHLoadingPanelFactory(
            logLoadingModel,
            "Select a job to show logs",
            GithubBundle.message("cannot.load.data.from.github"),
            errorHandler
        ).create { _, _ ->
            createLogPanel(logModel, disposable)
        }

        val runPanel = OnePixelSplitter(
            settingsService.state.jobListAboveLogs,
            "GitHub.Workflows.Component.Jobs",
            if (settingsService.state.jobListAboveLogs) 0.3f else 0.5f
        ).apply {
            background = UIUtil.getListBackground()
            isOpaque = true
            isFocusCycleRoot = true
            firstComponent = jobLoadingPanel
            secondComponent = logLoadingPanel
                .also {
                    (actionManager.getAction("Github.Workflow.Log.List.Reload") as RefreshAction)
                        .registerCustomShortcutSet(
                            it,
                            disposable
                        )
                }
        }

        return OnePixelSplitter("GitHub.Workflows.Component", 0.3f).apply {
            background = UIUtil.getListBackground()
            isOpaque = true
            isFocusCycleRoot = true
            firstComponent = workflowRunsList
            secondComponent = runPanel
        }.also {
            DataManager.registerDataProvider(it) { dataId ->
                when {
                    ActionKeys.ACTION_DATA_CONTEXT.`is`(dataId) -> selectionContext
                    else -> null
                }
            }
        }
    }

    private fun createJobPanel(
        jobModel: SingleValueModel<WorkflowRunJobs?>,
        selectionContext: WorkflowRunSelectionContext,
        settings: GithubActionsManagerSettings
    ): JComponent {
        val jobListPanel = JobList.createJobsListComponent(
            jobModel, selectionContext.jobSelectionHolder,
            infoInNewLine = !settings.jobListAboveLogs,
        )

        val panel = JBPanelWithEmptyText(BorderLayout()).apply {
            isOpaque = false
            add(jobListPanel, BorderLayout.CENTER)
        }
        return panel
    }

    private fun createLogPanel(logModel: SingleValueModel<String?>, disposable: Disposable): JComponent {
        LOG.debug("Create log panel")
        val console = LogConsolePanel(project, logModel, disposable)

        val panel = JBPanelWithEmptyText(BorderLayout()).apply {
            isOpaque = false
            add(console.component, BorderLayout.CENTER)
        }
        LOG.debug("Adding popup actions")
        val actionGroup = actionManager.getAction("Github.Workflow.Log.ToolWindow.List.Popup") as DefaultActionGroup
        actionGroup.removeAll()
        actionGroup.add(actionManager.getAction("Github.Workflow.Log.List.Reload"))
        actionGroup.add(
            object : ToggleUseSoftWrapsToolbarAction(SoftWrapAppliancePlaces.CONSOLE) {
                override fun getEditor(e: AnActionEvent): Editor? {
                    return console.editor
                }
            }
        )
        val contextMenuPopupHandler = ContextMenuPopupHandler.Simple(actionGroup)
        (console.editor as EditorEx).installPopupHandler(contextMenuPopupHandler)

        return panel
    }

    private fun createLogsDataProviderModel(
        context: WorkflowRunSelectionContext,
        parentDisposable: Disposable,
    ): SingleValueModel<WorkflowRunLogsDataProvider?> {
        val model: SingleValueModel<WorkflowRunLogsDataProvider?> = SingleValueModel(null)

        fun setNewProvider(provider: WorkflowRunLogsDataProvider?) {
            val oldValue = model.value
            if (oldValue != null && provider != null && oldValue.url() != provider.url()) {
                model.value = null
            }
            model.value = provider
        }
        Disposer.register(parentDisposable) {
            model.value = null
        }

        context.runSelectionHolder.addSelectionChangeListener(parentDisposable) {
            LOG.debug("selection change listener")
            val provider = context.runSelectionHolder.selection?.let {
                context.dataContext.dataLoader.getLogsDataProvider(it.logs_url)
            }
            setNewProvider(provider)
        }
        context.jobSelectionHolder.addSelectionChangeListener(parentDisposable) {
            val provider = context.runSelectionHolder.selection?.let {
                context.dataContext.dataLoader.getLogsDataProvider(it.logs_url)
            }
            setNewProvider(provider)
        }

        context.dataContext.dataLoader.addInvalidationListener(parentDisposable) {
            LOG.debug("invalidation listener")
            val selection = context.runSelectionHolder.selection
            if (selection != null && selection.logs_url == it) {
                setNewProvider(context.dataContext.dataLoader.getLogsDataProvider(selection.logs_url))
            }
        }

        return model
    }

    private fun createJobsDataProviderModel(
        context: WorkflowRunDataContext,
        runsSelectionHolder: WorkflowRunListSelectionHolder,
        parentDisposable: Disposable,
    ): SingleValueModel<WorkflowRunJobsDataProvider?> {
        val model: SingleValueModel<WorkflowRunJobsDataProvider?> = SingleValueModel(null)

        fun setNewProvider(provider: WorkflowRunJobsDataProvider?) {
            LOG.debug("createJobsDataProviderModel setNewProvider")
            val oldValue = model.value
            if (oldValue != null && provider != null && oldValue.url() != provider.url()) {
                model.value = null
            }
            model.value = provider
        }
        Disposer.register(parentDisposable) {
            model.value = null
        }

        runsSelectionHolder.addSelectionChangeListener(parentDisposable) {
            LOG.debug("createJobsDataProviderModel selection change listener")
            val provider = runsSelectionHolder.selection?.let { context.dataLoader.getJobsDataProvider(it.jobs_url) }
            setNewProvider(provider)
        }

        context.dataLoader.addInvalidationListener(parentDisposable) {
            LOG.debug("invalidation listener")
            val selection = runsSelectionHolder.selection
            if (selection != null && selection.logs_url == it) {
                setNewProvider(context.dataLoader.getJobsDataProvider(selection.jobs_url))
            }
        }

        return model
    }

    private fun createJobsLoadingModel(
        dataProviderModel: SingleValueModel<WorkflowRunJobsDataProvider?>,
        parentDisposable: Disposable,
    ): Pair<GHCompletableFutureLoadingModel<WorkflowRunJobs>, SingleValueModel<WorkflowRunJobs?>> {
        LOG.debug("createJobsDataProviderModel Create log loading model")
        val valueModel = SingleValueModel<WorkflowRunJobs?>(null)

        val loadingModel = GHCompletableFutureLoadingModel<WorkflowRunJobs>(parentDisposable).also {
            it.addStateChangeListener(object : GHLoadingModel.StateChangeListener {
                override fun onLoadingCompleted() {
                    if (it.resultAvailable) {
                        valueModel.value = it.result
                    }
                }

                override fun onReset() {
                    LOG.debug("onReset")
                    valueModel.value = it.result
                }
            })
        }

        var listenerDisposable: Disposable? = null

        dataProviderModel.addListener {
            LOG.debug("Jobs loading model Value changed")
            val provider = dataProviderModel.value
            loadingModel.future = provider?.request

            listenerDisposable = listenerDisposable?.let {
                Disposer.dispose(it)
                null
            }

            if (provider != null) {
                val disposable = Disposer.newDisposable().apply {
                    Disposer.register(parentDisposable, this)
                }
                provider.addRunChangesListener(disposable,
                    object : DataProvider.WorkflowRunChangedListener {
                        override fun changed() {
                            loadingModel.future = provider.request
                        }
                    })

                listenerDisposable = disposable
            }
        }
        return loadingModel to valueModel
    }

    private fun createLogLoadingModel(
        dataProviderModel: SingleValueModel<WorkflowRunLogsDataProvider?>,
        jobsSelectionHolder: JobListSelectionHolder,
        parentDisposable: Disposable,
    ): Pair<GHCompletableFutureLoadingModel<Map<String, String>>, SingleValueModel<String?>> {
        LOG.debug("Create log loading model")
        val valueModel = SingleValueModel<String?>(null)
        var jobName = jobsSelectionHolder.selection?.name
        val loadingModel = GHCompletableFutureLoadingModel<Map<String, String>>(parentDisposable).also {
            it.addStateChangeListener(object : GHLoadingModel.StateChangeListener {
                override fun onLoadingCompleted() {
                    if (it.resultAvailable) {
                        jobName = null
                        valueModel.value = "Pick a job to view logs"
                    }
                }

                override fun onReset() {
                    LOG.debug("onReset")
                    val key = (jobName ?: "").replace("<", "").replace(">", "").trim()
                    valueModel.value = if (it.result?.isEmpty() == true) {
                        NO_LOGS_MSG
                    } else {
                        it.result?.get(key) ?: "Job $jobName logs missing"
                    }
                }
            })
        }
        jobsSelectionHolder.addSelectionChangeListener(parentDisposable) {
            jobName = jobsSelectionHolder.selection?.name
            val key = (jobName ?: "").replace("<", "").replace(">", "").trim()
            valueModel.value = if (loadingModel.result?.isEmpty() == true) {
                NO_LOGS_MSG
            } else {
                loadingModel.result?.get(key) ?: "Job $jobName logs missing"
            }
        }
        var listenerDisposable: Disposable? = null

        dataProviderModel.addListener {
            LOG.debug("log loading model Value changed")
            val provider = dataProviderModel.value
            loadingModel.future = provider?.request

            listenerDisposable = listenerDisposable?.let {
                Disposer.dispose(it)
                null
            }

            if (provider != null) {
                val disposable = Disposer.newDisposable().apply {
                    Disposer.register(parentDisposable, this)
                }
                provider.addRunChangesListener(disposable,
                    object : DataProvider.WorkflowRunChangedListener {
                        override fun changed() {
                            LOG.debug("Log changed ${provider.request}")
                            loadingModel.future = provider.request
                        }
                    })

                listenerDisposable = disposable
            }
        }
        return loadingModel to valueModel
    }

    companion object {
        private const val NO_LOGS_MSG =
            "Can not fetch logs when workflow in progress, please try again when workflow is completed"
        val KEY = Key.create<WorkflowToolWindowTabController>("Github.Actions.ToolWindow.Tab.Controller")
        private val LOG = logger<WorkflowToolWindowTabController>()
    }
}