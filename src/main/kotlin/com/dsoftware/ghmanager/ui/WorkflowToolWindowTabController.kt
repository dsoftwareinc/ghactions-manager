package com.dsoftware.ghmanager.ui


import com.dsoftware.ghmanager.actions.ActionKeys
import com.dsoftware.ghmanager.data.DataProvider
import com.dsoftware.ghmanager.data.LogLoadingModelListener
import com.dsoftware.ghmanager.data.WorkflowDataContextRepository
import com.dsoftware.ghmanager.data.WorkflowRunJobsDataProvider
import com.dsoftware.ghmanager.data.WorkflowRunSelectionContext
import com.dsoftware.ghmanager.ui.panels.JobListComponent
import com.dsoftware.ghmanager.ui.panels.WorkflowRunListLoaderPanel
import com.dsoftware.ghmanager.ui.panels.createLogConsolePanel
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.ide.DataManager
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.ClientProperty
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.util.ui.UIUtil
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
    repositoryMapping: GHGitRepositoryMapping,
    private val ghAccount: GithubAccount,
    private val dataContextRepository: WorkflowDataContextRepository,
    parentDisposable: Disposable,
    toolWindow: ToolWindow,
) {
    val loadingModel: GHCompletableFutureLoadingModel<WorkflowRunSelectionContext>
    private val settingsService = GhActionsSettingsService.getInstance(project)
    private val actionManager = ActionManager.getInstance()
    private val disposable = Disposer.newDisposable("")
    val panel: JComponent
    private var contentDisposable by Delegates.observable<Disposable?>(null) { _, oldValue, newValue ->
        if (oldValue != null) Disposer.dispose(oldValue)
        if (newValue != null) Disposer.register(parentDisposable, newValue)
    }

    init {
        Disposer.register(parentDisposable, disposable)
        contentDisposable = Disposable {
            Disposer.dispose(disposable)
            dataContextRepository.clearContext(repositoryMapping)
        }
        loadingModel = GHCompletableFutureLoadingModel<WorkflowRunSelectionContext>(disposable).apply {
            future = dataContextRepository.acquireContext(
                disposable,
                repositoryMapping,
                ghAccount,
                toolWindow
            )
        }

        val errorHandler = GHApiLoadingErrorHandler(project, ghAccount) {
            val contextRepository = dataContextRepository
            contextRepository.clearContext(repositoryMapping)
            loadingModel.future =
                contextRepository.acquireContext(
                    disposable,
                    repositoryMapping,
                    ghAccount,
                    toolWindow
                )
        }
        panel = GHLoadingPanelFactory(
            loadingModel,
            "Not loading workflow runs",
            GithubBundle.message("cannot.load.data.from.github"),
            errorHandler,
        ).create { _, result ->
            val content = createContent(result)
            ClientProperty.put(content, AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)
            content
        }
    }

    private fun createContent(
        selectedRunContext: WorkflowRunSelectionContext,
    ): JComponent {

        val workflowRunsList = WorkflowRunListLoaderPanel
            .createWorkflowRunsListComponent(selectedRunContext, disposable)

        val jobLoadingPanel = createJobsPanel(selectedRunContext)

        val logLoadingPanel = createLogPanel(selectedRunContext)

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
                        .registerCustomShortcutSet(it, disposable)
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
                    ActionKeys.ACTION_DATA_CONTEXT.`is`(dataId) -> selectedRunContext
                    else -> null
                }
            }
        }
    }


    private fun createLogPanel(selectedRunContext: WorkflowRunSelectionContext): JComponent {
        val model = LogLoadingModelListener(
            disposable,
            selectedRunContext.logDataProviderLoadModel,
            selectedRunContext.jobSelectionHolder
        )
        LOG.debug("Create log panel")

        val errorHandler = GHApiLoadingErrorHandler(project, ghAccount) {
        }
        val panel = GHLoadingPanelFactory(
            model.logsLoadingModel,
            "Select a job to show logs",
            GithubBundle.message("cannot.load.data.from.github"),
            errorHandler
        ).create { _, _ ->

            createLogConsolePanel(project, model.logModel, disposable)
        }
        return panel
    }

    private fun createJobsPanel(selectedRunContext: WorkflowRunSelectionContext): JComponent {
        val (jobLoadingModel, jobModel) = createJobsLoadingModel(selectedRunContext.jobDataProviderLoadModel)

        val errorHandler = GHApiLoadingErrorHandler(project, ghAccount) {
        }
        val jobLoadingPanel = GHLoadingPanelFactory(
            jobLoadingModel,
            "Select a workflow to show list of jobs",
            GithubBundle.message("cannot.load.data.from.github"),
            errorHandler
        ).create { _, _ ->
            val jobListPanel = JobListComponent.createJobsListComponent(
                jobModel, selectedRunContext,
                infoInNewLine = !settingsService.state.jobListAboveLogs,
            )

            val panel = JBPanelWithEmptyText(BorderLayout()).apply {
                isOpaque = false
                add(jobListPanel, BorderLayout.CENTER)
            }
            panel
        }
        return jobLoadingPanel
    }

    private fun createJobsLoadingModel(
        dataProviderModel: SingleValueModel<WorkflowRunJobsDataProvider?>,
    ): Pair<GHCompletableFutureLoadingModel<com.dsoftware.ghmanager.api.model.JobsList>, SingleValueModel<com.dsoftware.ghmanager.api.model.JobsList?>> {
        LOG.debug("createJobsDataProviderModel Create jobs loading model")
        val valueModel = SingleValueModel<com.dsoftware.ghmanager.api.model.JobsList?>(null)

        val loadingModel =
            GHCompletableFutureLoadingModel<com.dsoftware.ghmanager.api.model.JobsList>(disposable).also {
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
            loadingModel.future = null
            loadingModel.future = provider?.request

            listenerDisposable = listenerDisposable?.let {
                Disposer.dispose(it)
                null
            }

            if (provider != null) {
                val disposable = Disposer.newDisposable().apply {
                    Disposer.register(disposable, this)
                }
                provider.addRunChangesListener(disposable,
                    object : DataProvider.DataProviderChangeListener {
                        override fun changed() {
                            loadingModel.future = provider.request
                        }
                    })

                listenerDisposable = disposable
            }
        }
        return loadingModel to valueModel
    }


    companion object {
        val KEY = Key.create<WorkflowToolWindowTabController>("Github.Actions.ToolWindow.Tab.Controller")
        private val LOG = logger<WorkflowToolWindowTabController>()
    }
}