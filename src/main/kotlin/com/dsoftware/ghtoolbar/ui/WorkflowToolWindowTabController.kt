package com.dsoftware.ghtoolbar.ui

import com.dsoftware.ghtoolbar.ui.consolepanel.WorkflowRunLogConsole
import com.dsoftware.ghtoolbar.ui.wfpanel.WorkflowRunListLoaderPanel
import com.dsoftware.ghtoolbar.workflow.WorkflowRunListSelectionHolder
import com.dsoftware.ghtoolbar.workflow.WorkflowRunSelectionContext
import com.dsoftware.ghtoolbar.workflow.action.ActionKeys
import com.dsoftware.ghtoolbar.workflow.data.WorkflowDataContextRepository
import com.dsoftware.ghtoolbar.workflow.data.WorkflowRunDataContext
import com.dsoftware.ghtoolbar.workflow.data.WorkflowRunDataProvider
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

    var isDisposed: Boolean = false
    fun dispose() {
        isDisposed = true
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
            future = dataContextRepository.acquireContext(repository, remote, ghAccount, ghRequestExecutor)
        }

        val errorHandler = GHApiLoadingErrorHandler(project, ghAccount) {
            val contextRepository = dataContextRepository
            contextRepository.clearContext(repository)
            loadingModel.future = contextRepository.acquireContext(repository, remote, ghAccount, ghRequestExecutor)
        }
        val panel = GHLoadingPanelFactory(
            loadingModel,
            null,
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
        val listSelectionHolder = WorkflowRunListSelectionHolder()
        val workflowRunsList = WorkflowRunListLoaderPanel
            .createWorkflowRunsListComponent(context, listSelectionHolder, disposable)

        val dataProviderModel = createDataProviderModel(context, listSelectionHolder, disposable)

        val (logLoadingModel, logModel) = createLogLoadingModel(dataProviderModel, disposable)

        val errorHandler = GHApiLoadingErrorHandler(project, account) {
        }
        val logLoadingPanel = GHLoadingPanelFactory(
            logLoadingModel,
            "Select a workflow to show logs",
            GithubBundle.message("cannot.load.data.from.github"),
            errorHandler
        ).create { _, _ ->
            createLogPanel(logModel, disposable)
        }

        val selectionDataContext = WorkflowRunSelectionContext(context, listSelectionHolder)

        return OnePixelSplitter("GitHub.Workflows.Component", 0.5f).apply {
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
                if (Disposer.isDisposed(disposable)) null
                else when {
                    ActionKeys.ACTION_DATA_CONTEXT.`is`(dataId) -> selectionDataContext
                    else -> null
                }

            }
        }
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

    private fun createDataProviderModel(
        context: WorkflowRunDataContext,
        listSelectionHolder: WorkflowRunListSelectionHolder,
        parentDisposable: Disposable,
    ): SingleValueModel<WorkflowRunDataProvider?> {
        val model: SingleValueModel<WorkflowRunDataProvider?> = SingleValueModel(null)

        fun setNewProvider(provider: WorkflowRunDataProvider?) {
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

        listSelectionHolder.addSelectionChangeListener(parentDisposable) {
            LOG.info("selection change listener")
            val provider = listSelectionHolder.selection?.let { context.dataLoader.getDataProvider(it.logs_url) }
            setNewProvider(provider)
        }

        context.dataLoader.addInvalidationListener(parentDisposable) {
            LOG.info("invalidation listener")
            val selection = listSelectionHolder.selection
            if (selection != null && selection.logs_url == it) {
                setNewProvider(context.dataLoader.getDataProvider(selection.logs_url))
            }
        }

        return model
    }

    private fun createLogLoadingModel(
        dataProviderModel: SingleValueModel<WorkflowRunDataProvider?>,
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
            loadingModel.future = provider?.logRequest

            listenerDisposable = listenerDisposable?.let {
                Disposer.dispose(it)
                null
            }

            if (provider != null) {
                val disposable = Disposer.newDisposable().apply {
                    Disposer.register(parentDisposable, this)
                }
                provider.addRunChangesListener(disposable,
                    object : WorkflowRunDataProvider.WorkflowRunChangedListener {
                        override fun logChanged() {
                            LOG.info("Log changed ${provider.logRequest}")
                            loadingModel.future = provider.logRequest
                        }
                    })

                listenerDisposable = disposable
            }
        }
        return loadingModel to valueModel
    }

    companion object {
        val KEY = Key.create<WorkflowToolWindowTabController>("Github.PullRequests.ToolWindow.Tab.Controller")
        private val LOG = logger<WorkflowToolWindowTabController>()
    }
}