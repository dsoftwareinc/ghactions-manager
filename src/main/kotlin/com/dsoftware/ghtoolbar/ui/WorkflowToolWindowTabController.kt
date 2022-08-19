package com.dsoftware.ghtoolbar.ui

import JobsConsole
import WorkflowRunJobs
import com.dsoftware.ghtoolbar.actions.ActionKeys
import com.dsoftware.ghtoolbar.data.DataProvider
import com.dsoftware.ghtoolbar.data.WorkflowDataContextRepository
import com.dsoftware.ghtoolbar.data.WorkflowRunJobsDataProvider
import com.dsoftware.ghtoolbar.data.WorkflowRunLogsDataProvider
import com.dsoftware.ghtoolbar.ui.consolepanel.WorkflowRunLogConsole
import com.dsoftware.ghtoolbar.ui.wfpanel.WorkflowRunListLoaderPanel
import com.dsoftware.ghtoolbar.workflow.WorkflowRunDataContext
import com.dsoftware.ghtoolbar.workflow.WorkflowRunListSelectionHolder
import com.dsoftware.ghtoolbar.workflow.WorkflowRunSelectionContext
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
    repositoryMapping: GHGitRepositoryMapping,
    ghAccount: GithubAccount,
    private val dataContextRepository: WorkflowDataContextRepository,
    private val tab: Content,
) {
    private val actionManager = ActionManager.getInstance()
    private val mainPanel: JComponent
    private val ghRequestExecutor = GithubApiRequestExecutorManager.getInstance().getExecutor(ghAccount)

    private var contentDisposable by Delegates.observable<Disposable?>(null) { _, oldValue, newValue ->
        if (oldValue != null) Disposer.dispose(oldValue)
        if (newValue != null) Disposer.register(tab.disposer!!, newValue)
    }

    init {
        tab.displayName = repositoryMapping.repositoryPath
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
            LOG.info("create content")
            val content = createContent(result, ghAccount, disposable)
            LOG.info("done creating content")

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
        val selectionDataContext = WorkflowRunSelectionContext(context, runsSelectionHolder)

        val dataProviderModel = createJobsDataProviderModel(context, runsSelectionHolder, disposable)

        val (jobLoadingModel, jobModel) = createJobsLoadingModel(dataProviderModel, disposable)
        val workflowRunsList = WorkflowRunListLoaderPanel
            .createWorkflowRunsListComponent(selectionDataContext, disposable)
        val errorHandler = GHApiLoadingErrorHandler(project, account) {
        }
        val logLoadingPanel = GHLoadingPanelFactory(
            jobLoadingModel,
            "Select a workflow to show logs",
            GithubBundle.message("cannot.load.data.from.github"),
            errorHandler
        ).create { _, _ ->
            createJobPanel(jobModel, disposable)
        }
//        val dataProviderModel = createLogsDataProviderModel(context, runsSelectionHolder, disposable)
//
//        val (logLoadingModel, logModel) = createLogLoadingModel(dataProviderModel, disposable)
//        val workflowRunsList = WorkflowRunListLoaderPanel
//            .createWorkflowRunsListComponent(selectionDataContext, disposable)
//        val errorHandler = GHApiLoadingErrorHandler(project, account) {
//        }
//        val logLoadingPanel = GHLoadingPanelFactory(
//            logLoadingModel,
//            "Select a workflow to show logs",
//            GithubBundle.message("cannot.load.data.from.github"),
//            errorHandler
//        ).create { _, _ ->
//            createLogPanel(logModel, disposable)
//        }


        return OnePixelSplitter("GitHub.Workflows.Component", 0.3f).apply {
            background = UIUtil.getListBackground()
            isOpaque = true
            isFocusCycleRoot = true
            firstComponent = workflowRunsList
            secondComponent = logLoadingPanel
                .also {
                    (actionManager.getAction("Github.Workflow.Log.List.Reload") as RefreshAction)
                        .registerCustomShortcutSet(
                            it,
                            disposable
                        )
                }
        }.also {
            DataManager.registerDataProvider(it) { dataId ->
                when {
                    ActionKeys.ACTION_DATA_CONTEXT.`is`(dataId) -> selectionDataContext
                    else -> null
                }
            }
        }
    }

    private fun createJobPanel(jobModel: SingleValueModel<WorkflowRunJobs?>, disposable: Disposable): JComponent {
        val console = JobsConsole(project, jobModel, disposable)

        val panel = JBPanelWithEmptyText(BorderLayout()).apply {
            isOpaque = false
            add(console.component, BorderLayout.CENTER)
        }
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

    private fun createLogPanel(logModel: SingleValueModel<String?>, disposable: Disposable): JComponent {
        LOG.info("Create log panel")
        val console = WorkflowRunLogConsole(project, logModel, disposable)

        val panel = JBPanelWithEmptyText(BorderLayout()).apply {
            isOpaque = false
            add(console.component, BorderLayout.CENTER)
        }
        LOG.info("Adding popup actions")
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
        context: WorkflowRunDataContext,
        runsSelectionHolder: WorkflowRunListSelectionHolder,
        parentDisposable: Disposable,
    ): SingleValueModel<WorkflowRunLogsDataProvider?> {
        val model: SingleValueModel<WorkflowRunLogsDataProvider?> = SingleValueModel(null)

        fun setNewProvider(provider: WorkflowRunLogsDataProvider?) {
            LOG.info("setNewProvider")
            val oldValue = model.value
            if (oldValue != null && provider != null && oldValue.url != provider.url) {
                model.value = null
            }
            model.value = provider
        }
        Disposer.register(parentDisposable) {
            model.value = null
        }

        runsSelectionHolder.addSelectionChangeListener(parentDisposable) {
            LOG.info("selection change listener")
            val provider = runsSelectionHolder.selection?.let { context.dataLoader.getLogsDataProvider(it.logs_url) }
            setNewProvider(provider)
        }

        context.dataLoader.addInvalidationListener(parentDisposable) {
            LOG.info("invalidation listener")
            val selection = runsSelectionHolder.selection
            if (selection != null && selection.logs_url == it) {
                setNewProvider(context.dataLoader.getLogsDataProvider(selection.logs_url))
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
            LOG.info("createJobsDataProviderModel setNewProvider")
            val oldValue = model.value
            if (oldValue != null && provider != null && oldValue.url != provider.url) {
                model.value = null
            }
            model.value = provider
        }
        Disposer.register(parentDisposable) {
            model.value = null
        }

        runsSelectionHolder.addSelectionChangeListener(parentDisposable) {
            LOG.info("createJobsDataProviderModel selection change listener")
            val provider = runsSelectionHolder.selection?.let { context.dataLoader.getJobsDataProvider(it.jobs_url) }
            setNewProvider(provider)
        }

        context.dataLoader.addInvalidationListener(parentDisposable) {
            LOG.info("invalidation listener")
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
        LOG.info("createJobsDataProviderModel Create log loading model")
        val valueModel = SingleValueModel<WorkflowRunJobs?>(null)

        val loadingModel = GHCompletableFutureLoadingModel<WorkflowRunJobs>(parentDisposable).also {
            it.addStateChangeListener(object : GHLoadingModel.StateChangeListener {
                override fun onLoadingCompleted() {
                    if (it.resultAvailable) {
                        valueModel.value = it.result
                    }
                }

                override fun onReset() {
                    LOG.info("onReset")
                    valueModel.value = it.result
                }
            })
        }

        var listenerDisposable: Disposable? = null

        dataProviderModel.addListener {
            LOG.info("log loading model Value changed")
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
        parentDisposable: Disposable,
    ): Pair<GHCompletableFutureLoadingModel<String>, SingleValueModel<String?>> {
        LOG.info("Create log loading model")
        val valueModel = SingleValueModel<String?>(null)

        val loadingModel = GHCompletableFutureLoadingModel<String>(parentDisposable).also {
            it.addStateChangeListener(object : GHLoadingModel.StateChangeListener {
                override fun onLoadingCompleted() {
                    if (it.resultAvailable) {
                        LOG.info("result available ${it.result?.length}")
                        valueModel.value = it.result
                    }
                }

                override fun onReset() {
                    LOG.info("onReset")
                    valueModel.value = it.result
                }
            })
        }

        var listenerDisposable: Disposable? = null

        dataProviderModel.addListener {
            LOG.info("log loading model Value changed")
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
                            LOG.info("Log changed ${provider.request}")
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