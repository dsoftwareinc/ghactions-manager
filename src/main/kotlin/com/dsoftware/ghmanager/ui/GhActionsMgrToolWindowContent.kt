package com.dsoftware.ghmanager.ui

import ai.grazie.utils.applyIf
import com.dsoftware.ghmanager.data.GhActionsService
import com.dsoftware.ghmanager.data.WorkflowDataContextService
import com.dsoftware.ghmanager.i18n.MessagesBundle
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.dsoftware.ghmanager.ui.settings.GithubActionsManagerSettings
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.ui.UIUtil
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import java.awt.BorderLayout
import javax.swing.JPanel

enum class GhActionsMgrToolWindowState {
    UNINITIALIZED,
    NO_GITHUB_ACCOUNT,
    NO_REPOS_IN_PROJECT,
    NO_REPOS_CONFIGURED,
    REPOS,
}

class GhActionsMgrToolWindowContent(val toolWindow: ToolWindow) {
    private val settingsService: GhActionsSettingsService
    private val ghActionsService: GhActionsService

    private var state: GhActionsMgrToolWindowState = GhActionsMgrToolWindowState.UNINITIALIZED
    private var currentReposWithPanels: Set<GHGitRepositoryMapping> = emptySet()

    init {
        val project = toolWindow.project
        ghActionsService = project.service<GhActionsService>()
        settingsService = project.service<GhActionsSettingsService>()
    }

    fun init() {
        ghActionsService.registerToolWindow(this)
    }

    fun createContent() {
        ApplicationManager.getApplication().invokeLater {
            val projectRepos = ghActionsService.knownRepositories
            val countRepos = projectRepos.count {
                settingsService.state.customRepos[it.remote.url]?.included ?: false
            }
            val nextState = when {
                (ghActionsService.gitHubAccounts.isEmpty() && settingsService.state.useGitHubSettings)
                    || (!settingsService.state.useGitHubSettings && settingsService.state.apiToken == "") -> GhActionsMgrToolWindowState.NO_GITHUB_ACCOUNT

                projectRepos.isEmpty() -> GhActionsMgrToolWindowState.NO_REPOS_IN_PROJECT
                settingsService.state.useCustomRepos && countRepos == 0 -> GhActionsMgrToolWindowState.NO_REPOS_CONFIGURED
                else -> GhActionsMgrToolWindowState.REPOS
            }
            if (state == nextState && nextState != GhActionsMgrToolWindowState.REPOS) {
                return@invokeLater
            }
            state = nextState
            currentReposWithPanels = emptySet()
            if (state == GhActionsMgrToolWindowState.NO_GITHUB_ACCOUNT) {
                addEmptyTextTabToWindow(
                    toolWindow,
                    MessagesBundle.message("factory.empty-panel.no-account-configured"),
                    showGithubSettings = true,
                    showGhmanagerSettings = true,
                )
            } else if (projectRepos.isEmpty()) {
                addEmptyTextTabToWindow(
                    toolWindow,
                    MessagesBundle.message("factory.empty-panel.no-repos-in-project"),
                    showGithubSettings = false,
                    showGhmanagerSettings = false,
                )
            } else if (settingsService.state.useCustomRepos && countRepos == 0) {
                addEmptyTextTabToWindow(
                    toolWindow,
                    MessagesBundle.message("factory.empty-panel.no-repos-configured"),
                    showGithubSettings = false,
                    showGhmanagerSettings = true,
                )
            } else {
                createRepoWorkflowsPanels(toolWindow)
            }
        }
    }

    private fun createRepoWorkflowsPanels(toolWindow: ToolWindow) {
        val actionManager = ActionManager.getInstance()
        toolWindow.setAdditionalGearActions(DefaultActionGroup(actionManager.getAction("Github.Actions.Manager.Settings.Open")))
        val dataContextRepository = toolWindow.project.service<WorkflowDataContextService>()
        val reposToHavePanel = ghActionsService.knownRepositories.filter {
            !settingsService.state.useCustomRepos
                || (settingsService.state.customRepos[it.remote.url]?.included ?: false)
        }.toSet()
        if (currentReposWithPanels == reposToHavePanel) {
            return
        }
        currentReposWithPanels = reposToHavePanel
        toolWindow.contentManager.removeAllContents(true)
        currentReposWithPanels.forEach { repo ->
            val ghAccount = ghActionsService.guessAccountForRepository(repo)
            if (ghAccount != null) {
                LOG.info("adding panel for repo: ${repo.repositoryPath}, ${ghAccount.name}")
                val repoSettings =
                    settingsService.state.customRepos.getOrPut(repo.remote.url) { GithubActionsManagerSettings.RepoSettings() }
                val tab = toolWindow.contentManager.factory.createContent(
                    JPanel(null), repo.repositoryPath, false
                ).apply {
                    isCloseable = false
                    val disposable = Disposer.newDisposable("gha-manager ${repo.repositoryPath} tab disposable")
                    Disposer.register(toolWindow.disposable, disposable)
                    setDisposer(disposable)
                    displayName = repoSettings.customName.ifEmpty { repo.repositoryPath }
                }
                val controller = RepoTabController(
                    repo, ghAccount, dataContextRepository, tab.disposer!!, toolWindow,
                )
                tab.component.apply {
                    layout = BorderLayout()
                    background = UIUtil.getListBackground()
                    removeAll()
                    add(controller.panel, BorderLayout.CENTER)
                    revalidate()
                    repaint()
                }
                tab.putUserData(RepoTabController.KEY, controller)
                toolWindow.contentManager.addContent(tab)
            } else {
                addEmptyTextTabToWindow(
                    toolWindow,
                    MessagesBundle.message("factory.empty-panel.no-account-for-repo", repo.repository),
                    showGithubSettings = true,
                    showGhmanagerSettings = true
                )
            }
        }
        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                val content = event.content
                val controller = content.getUserData(RepoTabController.KEY)
                LOG.debug("Got selectionChanged event: ${content.displayName}: controller=${controller != null}, isSelected=${content.isSelected}")
                controller?.apply {
                    this.loadingModel.result?.runsListLoader?.refreshRuns = content.isSelected
                }
            }
        })
        ToolbarUtil.executeTaskAtCustomFrequency(toolWindow.project, 5) {
            toolWindow.contentManager.contents.forEach {
                val controller = it.getUserData(RepoTabController.KEY)
                controller?.apply {
                    this.loadingModel.result?.runsListLoader?.refreshRuns = it.isSelected
                }
            }
        }
    }


    companion object {
        private val LOG = logger<GhActionsMgrToolWindowContent>()
        private fun addEmptyTextTabToWindow(
            toolWindow: ToolWindow,
            text: String,
            showGithubSettings: Boolean,
            showGhmanagerSettings: Boolean,
        ) = with(toolWindow.contentManager) {
            toolWindow.contentManager.removeAllContents(true)
            val disposable = Disposer.newDisposable("GitHubWorkflow tab disposable")
            Disposer.register(toolWindow.disposable, disposable)

            LOG.debug("Adding empty text tab to window: $text with githubSettings=$showGithubSettings, ghmanagerSettings=$showGhmanagerSettings")
            val emptyTextPanel = JBPanelWithEmptyText()
            emptyTextPanel.apply {
                emptyText.appendText(text)
            }.applyIf(showGithubSettings) {
                emptyTextPanel.emptyText.appendLine(
                    MessagesBundle.message("factory.go.to.github-settings"),
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    ActionUtil.createActionListener(
                        "ShowGithubSettings",
                        emptyTextPanel,
                        ActionPlaces.UNKNOWN
                    )
                )
            }.applyIf(showGhmanagerSettings) {
                emptyTextPanel.emptyText.appendLine(
                    MessagesBundle.message("factory.go.to.ghmanager-settings"),
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    ActionUtil.createActionListener(
                        "Github.Actions.Manager.Settings.Open",
                        emptyTextPanel,
                        ActionPlaces.UNKNOWN
                    )
                )
            }.apply {
                addContent(
                    factory.createContent(emptyTextPanel, MessagesBundle.message("factory.default-tab-title"), false)
                        .apply {
                            isCloseable = false
                            setDisposer(disposable)
                        }
                )
            }
        }
    }
}