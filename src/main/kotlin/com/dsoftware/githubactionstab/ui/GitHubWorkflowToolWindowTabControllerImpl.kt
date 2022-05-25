package com.dsoftware.githubactionstab.ui

import com.intellij.collaboration.auth.AccountsListener
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.ide.DataManager
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.ContextMenuPopupHandler
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ListUtil
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.content.Content
import com.intellij.util.ui.UIUtil
import com.dsoftware.githubactionstab.api.GitHubWorkflowRun
import com.dsoftware.githubactionstab.workflow.GitHubLoadingErrorHandler
import com.dsoftware.githubactionstab.workflow.GitHubWorkflowRunListSelectionHolder
import com.dsoftware.githubactionstab.workflow.GitHubWorkflowRunSelectionContext
import com.dsoftware.githubactionstab.workflow.action.GitHubWorkflowRunActionKeys
import com.dsoftware.githubactionstab.workflow.data.GitHubWorkflowDataContextRepository
import com.dsoftware.githubactionstab.workflow.data.GitHubWorkflowRunDataContext
import com.dsoftware.githubactionstab.workflow.data.GitHubWorkflowRunDataProvider
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.GithubApiRequestExecutorManager
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.i18n.GithubBundle
import org.jetbrains.plugins.github.pullrequest.ui.GHApiLoadingErrorHandler
import org.jetbrains.plugins.github.pullrequest.ui.GHCompletableFutureLoadingModel
import org.jetbrains.plugins.github.pullrequest.ui.GHLoadingModel
import org.jetbrains.plugins.github.pullrequest.ui.GHLoadingPanelFactory
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GHProjectRepositoriesManager
import java.awt.BorderLayout
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JComponent
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener
import javax.swing.event.ListSelectionEvent
import kotlin.properties.Delegates

private val LOG = logger<GitHubWorkflowToolWindowTabController>()

internal class GitHubWorkflowToolWindowTabControllerImpl(
    private val project: Project,
    private val authManager: GithubAuthenticationManager,
    private val repositoryManager: GHProjectRepositoriesManager,
    private val dataContextRepository: GitHubWorkflowDataContextRepository,
    private val tab: Content,
) : GitHubWorkflowToolWindowTabController {
    private var currentRepository: GHGitRepositoryMapping? = null
    private var currentAccount: GithubAccount? = null
    private val actionManager = ActionManager.getInstance()

    private var contentDisposable by Delegates.observable<Disposable?>(null) { _, oldValue, newValue ->
        if (oldValue != null) Disposer.dispose(oldValue)
        if (newValue != null) Disposer.register(tab.disposer!!, newValue)
    }

    init {
        authManager.addListener(tab.disposer!!, object : AccountsListener<GithubAccount> {
            override fun onAccountCredentialsChanged(account: GithubAccount) {
                ApplicationManager.getApplication().invokeLater({ Updater().update() }) {
                    Disposer.isDisposed(tab.disposer!!)
                }
            }
        })
        repositoryManager.addRepositoryListChangedListener(tab.disposer!!) {
            Updater().update()
        }
        Updater().update()
    }

    private inner class Updater {
        private val repos = repositoryManager.knownRepositories
        private val accounts = authManager.getAccounts()

        fun update() {
            LOG.debug("Updater.update()")
            guessAndSetRepoAndAccount()?.let { (repo, account) ->
                try {
                    val requestExecutor = GithubApiRequestExecutorManager.getInstance().getExecutor(account)
                    showWorkflowsComponent(repo, account, requestExecutor)
                } catch (e: Exception) {
                    null
                }
            }
        }

        private fun guessAndSetRepoAndAccount(): Pair<GHGitRepositoryMapping, GithubAccount>? {
            if (currentRepository == null && repos.size == 1) {
                currentRepository = repos.single()
            }

            val repo = currentRepository
            if (repo != null && currentAccount == null) {
                val matchingAccounts =
                    accounts.filter { it.server.equals(repo.ghRepositoryCoordinates.serverPath, true) }
                if (matchingAccounts.size == 1) {
                    currentAccount = matchingAccounts.single()
                }
            }
            val account = currentAccount
            LOG.debug("Updater.guessAndSetRepoAndAccount() => ${repo}, ${account}")
            return if (repo != null && account != null) repo to account else null
        }
    }

    private fun showWorkflowsComponent(
        repositoryMapping: GHGitRepositoryMapping,
        account: GithubAccount,
        requestExecutor: GithubApiRequestExecutor,
    ) {
        tab.displayName = "Workflows"
        val mainPanel = tab.component.apply {
            layout = BorderLayout()
            background = UIUtil.getListBackground()
        }
        LOG.debug("Updater.showWorkflowsComponent()")
        val repository = repositoryMapping.ghRepositoryCoordinates
        val remote = repositoryMapping.gitRemoteUrlCoordinates

        val disposable = Disposer.newDisposable()
        contentDisposable = Disposable {
            Disposer.dispose(disposable)
            dataContextRepository.clearContext(repository)
        }

        val loadingModel = GHCompletableFutureLoadingModel<GitHubWorkflowRunDataContext>(disposable).apply {
            future = dataContextRepository.acquireContext(repository, remote, account, requestExecutor)
        }

        val errorHandler = GHApiLoadingErrorHandler(project, account) {
            val contextRepository = dataContextRepository
            contextRepository.clearContext(repository)
            loadingModel.future = contextRepository.acquireContext(repository, remote, account, requestExecutor)
        }
        val panel = GHLoadingPanelFactory(
            loadingModel,
            null,
            GithubBundle.message("cannot.load.data.from.github"),
            errorHandler,
        ).create { parent, result ->
            LOG.debug("create content")
            val content = createContent(result, account, disposable)
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
        context: GitHubWorkflowRunDataContext,
        account: GithubAccount,
        disposable: Disposable,
    ): JComponent {
        val listSelectionHolder = GitHubWorkflowRunListSelectionHolder()
        val workflowRunsList = createWorkflowRunsListComponent(context, listSelectionHolder, disposable)

        val dataProviderModel = createDataProviderModel(context, listSelectionHolder, disposable)

        val logLoadingModel = createLogLoadingModel(dataProviderModel, disposable)
        val logModel = createValueModel(logLoadingModel)

        val errorHandler = GHApiLoadingErrorHandler(project, account) {
        }
        val logLoadingPanel = GHLoadingPanelFactory(
            logLoadingModel,
            "Can't load data from GitHub",
            GithubBundle.message("cannot.load.data.from.github"),
            errorHandler
        ).create { parent, result ->
            createLogPanel(logModel, disposable)
        }

        val selectionDataContext = GitHubWorkflowRunSelectionContext(context, listSelectionHolder)

        return OnePixelSplitter("GitHub.Workflows.Component", 0.5f).apply {
            background = UIUtil.getListBackground()
            isOpaque = true
            isFocusCycleRoot = true
            firstComponent = workflowRunsList
            secondComponent = logLoadingPanel
                .also {
                    (actionManager.getAction("Github.Workflow.Log.List.Reload") as RefreshAction).registerCustomShortcutSet(
                        it,
                        disposable
                    )
                }
        }.also {
            DataManager.registerDataProvider(it) { dataId ->
                if (Disposer.isDisposed(disposable)) null
                else when {
                    GitHubWorkflowRunActionKeys.ACTION_DATA_CONTEXT.`is`(dataId) -> selectionDataContext
                    else -> null
                }

            }
        }
    }

    private fun createLogPanel(logModel: SingleValueModel<String?>, disposable: Disposable): JBPanelWithEmptyText {
        LOG.debug("Create log panel")
        val console = GitHubWorkflowRunLogConsole(project, logModel, disposable)

        val panel = JBPanelWithEmptyText(BorderLayout()).apply {
            isOpaque = false
            add(console.component, BorderLayout.CENTER)

        }
        installLogPopup(console)

        val editor = console.editor

        val consoleActionsGroup = DefaultActionGroup()

        val reloadAction = actionManager.getAction("Github.Workflow.Log.List.Reload")
        consoleActionsGroup.add(reloadAction)
        consoleActionsGroup.add(object : ToggleUseSoftWrapsToolbarAction(SoftWrapAppliancePlaces.CONSOLE) {
            override fun getEditor(e: AnActionEvent): Editor? {
                return editor
            }
        })

        logModel.addListener {
            LOG.debug("Log model changed - call panel.validate()")
            panel.validate()
        }
        return panel
    }

    private fun createWorkflowRunsListComponent(
        context: GitHubWorkflowRunDataContext,
        listSelectionHolder: GitHubWorkflowRunListSelectionHolder,
        disposable: Disposable,
    ): JComponent {

        val list = GitHubWorkflowRunList(context.listModel).apply {
            emptyText.clear()
        }.also {
            it.addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent?) {
                    if (it.selectedIndex < 0 && !it.isEmpty) it.selectedIndex = 0
                }

                override fun focusLost(e: FocusEvent?) {}
            })

            installPopup(it)
            installWorkflowRunSelectionSaver(it, listSelectionHolder)
        }

        //Cannot seem to have context menu, when right click, why?
        val listReloadAction = actionManager.getAction("Github.Workflow.List.Reload") as RefreshAction

        return GitHubWorkflowRunListLoaderPanel(context.listLoader, listReloadAction, list).apply {
            errorHandler = GitHubLoadingErrorHandler {
                LOG.debug("Error on GitHub Workflow Run list loading, resetting the loader")
                context.listLoader.reset()
            }
        }.also {
            listReloadAction.registerCustomShortcutSet(it, disposable)

            val logActionsGroup = DefaultActionGroup()
            logActionsGroup.add(listReloadAction)
            val toolbar = ActionManager.getInstance().createActionToolbar(
                "WorkflowRuns", logActionsGroup,
                false
            )

            it.add(toolbar.component, BorderLayout.WEST)

            Disposer.register(disposable) {
                Disposer.dispose(it)
            }
        }
    }

    private fun installWorkflowRunSelectionSaver(
        list: GitHubWorkflowRunList,
        listSelectionHolder: GitHubWorkflowRunListSelectionHolder,
    ) {
        var savedSelection: GitHubWorkflowRun? = null

        list.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
            if (!e.valueIsAdjusting) {
                val selectedIndex = list.selectedIndex
                if (selectedIndex >= 0 && selectedIndex < list.model.size) {
                    listSelectionHolder.selection = list.model.getElementAt(selectedIndex)
                    savedSelection = null
                }
            }
        }

        list.model.addListDataListener(object : ListDataListener {
            override fun intervalAdded(e: ListDataEvent) {
                if (e.type == ListDataEvent.INTERVAL_ADDED)
                    (e.index0..e.index1).find { list.model.getElementAt(it) == savedSelection }
                        ?.run {
                            ApplicationManager.getApplication().invokeLater { ScrollingUtil.selectItem(list, this) }
                        }
            }

            override fun contentsChanged(e: ListDataEvent) {}
            override fun intervalRemoved(e: ListDataEvent) {
                if (e.type == ListDataEvent.INTERVAL_REMOVED) savedSelection = listSelectionHolder.selection
            }
        })
    }

    private fun installPopup(list: GitHubWorkflowRunList) {
        val popupHandler = object : PopupHandler() {
            override fun invokePopup(comp: java.awt.Component, x: Int, y: Int) {

                val popupMenu: ActionPopupMenu = if (ListUtil.isPointOnSelection(list, x, y)) {
                    actionManager
                        .createActionPopupMenu(
                            "GithubWorkflowListPopupSelected",
                            actionManager.getAction("Github.Workflow.ToolWindow.List.Popup.Selected") as ActionGroup
                        )
                } else {
                    actionManager
                        .createActionPopupMenu(
                            "GithubWorkflowListPopup",
                            actionManager.getAction("Github.Workflow.ToolWindow.List.Popup") as ActionGroup
                        )
                }
                popupMenu.setTargetComponent(list)
                popupMenu.component.show(comp, x, y)
            }
        }
        list.addMouseListener(popupHandler)
    }

    private fun installLogPopup(console: ConsoleViewImpl) {
        val actionGroup = actionManager.getAction("Github.Workflow.Log.ToolWindow.List.Popup") as ActionGroup
        val contextMenuPopupHandler = ContextMenuPopupHandler.Simple(actionGroup)

        (console.editor as EditorEx).installPopupHandler(contextMenuPopupHandler)
    }

    private fun createDataProviderModel(
        context: GitHubWorkflowRunDataContext,
        listSelectionHolder: GitHubWorkflowRunListSelectionHolder,
        parentDisposable: Disposable,
    ): SingleValueModel<GitHubWorkflowRunDataProvider?> {
        val model: SingleValueModel<GitHubWorkflowRunDataProvider?> = SingleValueModel(null)

        fun setNewProvider(provider: GitHubWorkflowRunDataProvider?) {
            LOG.debug("setNewProvider")
            val oldValue = model.value
            if (oldValue != null && provider != null && oldValue.url != provider.url) {
                model.value = null
            }
            model.value = provider
        }
        Disposer.register(parentDisposable, Disposable {
            model.value = null
        })

        listSelectionHolder.addSelectionChangeListener(parentDisposable) {
            LOG.debug("selection change listener")
            val provider = listSelectionHolder.selection?.let { context.dataLoader.getDataProvider(it.logs_url) }
            setNewProvider(provider)
        }

        context.dataLoader.addInvalidationListener(parentDisposable) {
            LOG.debug("invalidation listener")
            val selection = listSelectionHolder.selection
            if (selection != null && selection.logs_url == it) {
                setNewProvider(context.dataLoader.getDataProvider(selection.logs_url))
            }
        }

        return model
    }

    private fun createLogLoadingModel(
        dataProviderModel: SingleValueModel<GitHubWorkflowRunDataProvider?>,
        parentDisposable: Disposable,
    ): GHCompletableFutureLoadingModel<String> {
        LOG.debug("Create log loading model")
        val model = GHCompletableFutureLoadingModel<String>(parentDisposable)

        var listenerDisposable: Disposable? = null

        dataProviderModel.addListener {
            LOG.debug("Value changed")
            val provider = dataProviderModel.value
            model.future = provider?.logRequest

            listenerDisposable = listenerDisposable?.let {
                Disposer.dispose(it)
                null
            }

            if (provider != null) {
                val disposable = Disposer.newDisposable().apply {
                    Disposer.register(parentDisposable, this)
                }
                provider.addRunChangesListener(disposable,
                    object : GitHubWorkflowRunDataProvider.WorkflowRunChangedListener {
                        override fun logChanged() {
                            LOG.debug("Log changed")
                            model.future = provider.logRequest
                        }
                    })

                listenerDisposable = disposable
            }
        }
        return model
    }

    private fun <T> createValueModel(loadingModel: GHCompletableFutureLoadingModel<T>): SingleValueModel<T?> {
        val model = SingleValueModel<T?>(null)
        loadingModel.addStateChangeListener(object : GHLoadingModel.StateChangeListener {
            override fun onLoadingCompleted() {
                LOG.debug("onLoadingCompleted")
                model.value = loadingModel.result
            }

            override fun onReset() {
                LOG.debug("onReset")
                model.value = loadingModel.result
            }
        })
        return model
    }

}