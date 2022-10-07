package com.dsoftware.ghtoolbar.ui

import com.dsoftware.ghtoolbar.data.WorkflowDataContextRepository
import com.dsoftware.ghtoolbar.ui.settings.GhActionsSettingsService
import com.dsoftware.ghtoolbar.ui.settings.GhActionsToolbarConfigurable
import com.dsoftware.ghtoolbar.ui.settings.ToolbarSettings
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.pullrequest.data.provider.GHPRDataOperationsListener
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GHProjectRepositoriesManager
import javax.swing.JPanel

internal class GhActionToolWindow(
    val toolWindow: ToolWindow,
) {
    var knownRepositories: Set<GHGitRepositoryMapping> = emptySet()

}

class GhActionsToolWindowFactory : ToolWindowFactory {
    private lateinit var settingsService: GhActionsSettingsService
    private val authManager = GithubAuthenticationManager.getInstance()
    private val toolWindowsMap = mutableMapOf<Project, GhActionToolWindow>()

    override fun init(toolWindow: ToolWindow) {
        if (!toolWindowsMap.containsKey(toolWindow.project)) {
            toolWindowsMap[toolWindow.project] = GhActionToolWindow(toolWindow)
        }
        settingsService = GhActionsSettingsService.getInstance(toolWindow.project)
        val bus = ApplicationManager.getApplication().messageBus.connect(toolWindow.disposable)
        bus.subscribe(
            GhActionsToolbarConfigurable.SETTINGS_CHANGED,
            object : GhActionsToolbarConfigurable.SettingsChangedListener {
                override fun settingsChanged() {
                    createToolWindowContent(toolWindow.project, toolWindow)
                }
            })
        bus.subscribe(
            GHProjectRepositoriesManager.LIST_CHANGES_TOPIC,
            object : GHProjectRepositoriesManager.ListChangeListener {
                override fun repositoryListChanged(newList: Set<GHGitRepositoryMapping>, project: Project) {
                    LOG.debug("Repos updated, new list has ${newList.size} repos")
                    val ghActionToolWindow = toolWindowsMap[project] ?: return
                    ghActionToolWindow.toolWindow.isAvailable = newList.isNotEmpty()
                    ghActionToolWindow.knownRepositories = newList
                    ghActionToolWindow.knownRepositories.forEach { repo ->
                        settingsService.state.customRepos.putIfAbsent(
                            repo.gitRemoteUrlCoordinates.url,
                            ToolbarSettings.RepoSettings()
                        )
                    }
                    createToolWindowContent(project, toolWindow)
                }
            })
        bus.subscribe(
            GHPRDataOperationsListener.TOPIC,
            object : GHPRDataOperationsListener {

            }
        )
        authManager.addListener(toolWindow.disposable, object : AccountsListener<GithubAccount> {
            override fun onAccountListChanged(old: Collection<GithubAccount>, new: Collection<GithubAccount>) =
                scheduleUpdate()

            override fun onAccountCredentialsChanged(account: GithubAccount) = scheduleUpdate()

            private fun scheduleUpdate() = createToolWindowContent(toolWindow.project, toolWindow)

        })
    }


    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val ghActionToolWindow = toolWindowsMap[project] ?: return
        toolWindow.contentManager.removeAllContents(true)
        with(ghActionToolWindow) {
            if (!authManager.hasAccounts()) {
                noGitHubAccountPanel(ghActionToolWindow)
                return
            }
            if (knownRepositories.isEmpty()) {
                noRepositories(ghActionToolWindow)
                return
            }
            val countRepos = knownRepositories.count {
                settingsService.state.customRepos[it.gitRemoteUrlCoordinates.url]?.included ?: false
            }
            if (settingsService.state.useCustomRepos && countRepos == 0) {
                noActiveRepositories(ghActionToolWindow)
                return
            }
            ghAccountAndReposConfigured(project, ghActionToolWindow)
        }
    }

    private fun noActiveRepositories(ghActionToolWindow: GhActionToolWindow) =
        with(ghActionToolWindow.toolWindow.contentManager) {
            LOG.debug("No active repositories in project")
            val emptyTextPanel = JBPanelWithEmptyText()
            emptyTextPanel.emptyText
                .appendText("No Repositories Configured For GitHub Actions Toolbar")
                .appendSecondaryText(
                    "Go To Toolbar Settings",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    ActionUtil.createActionListener(
                        "ShowGhActionsToolbarSettings",
                        emptyTextPanel,
                        ActionPlaces.UNKNOWN
                    )
                )

            addContent(factory.createContent(emptyTextPanel, "Workflows", false)
                .apply {
                    isCloseable = false
                    setDisposer(Disposer.newDisposable("GitHubWorkflow tab disposable"))
                }
            )
        }

    private fun noGitHubAccountPanel(ghActionToolWindow: GhActionToolWindow) =
        with(ghActionToolWindow.toolWindow.contentManager) {
            LOG.debug("No GitHub account configured")
            val emptyTextPanel = JBPanelWithEmptyText()
            emptyTextPanel.emptyText
                .appendText("GitHub account not configured, go to settings to fix")
                .appendSecondaryText(
                    "Go to Settings",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    ActionUtil.createActionListener(
                        "ShowGithubSettings",
                        emptyTextPanel,
                        ActionPlaces.UNKNOWN
                    )
                )

            addContent(factory.createContent(emptyTextPanel, "Workflows", false)
                .apply {
                    isCloseable = false
                    setDisposer(Disposer.newDisposable("GitHubWorkflow tab disposable"))
                }
            )
        }

    private fun noRepositories(ghActionToolWindow: GhActionToolWindow) =
        with(ghActionToolWindow.toolWindow.contentManager) {
            LOG.debug("No git repositories in project")
            val emptyTextPanel = JBPanelWithEmptyText()
                .withEmptyText("No git repositories in project")

            addContent(factory.createContent(emptyTextPanel, "Workflows", false)
                .apply {
                    isCloseable = false
                    setDisposer(Disposer.newDisposable("GitHubWorkflow tab disposable"))
                }
            )
        }

    private fun guessAccountForRepository(repo: GHGitRepositoryMapping): GithubAccount? {
        val accounts = authManager.getAccounts()
        return accounts.firstOrNull { it.server.equals(repo.ghRepositoryCoordinates.serverPath, true) }
    }

    private fun ghAccountAndReposConfigured(
        project: Project,
        ghActionToolWindow: GhActionToolWindow
    ) =
        with(ghActionToolWindow) {
            val actionManager = ActionManager.getInstance()
            toolWindow.setTitleActions(listOf(actionManager.getAction("ShowGhActionsToolbarSettings")))
            toolWindow.setAdditionalGearActions(DefaultActionGroup())
            val dataContextRepository = WorkflowDataContextRepository.getInstance(project)
            knownRepositories
                .filter { settingsService.state.customRepos[it.gitRemoteUrlCoordinates.url]?.included ?: false }
                .forEach { repo ->
                    val ghAccount = guessAccountForRepository(repo)
                    if (ghAccount != null) {
                        LOG.debug("adding panel for repo: ${repo.repositoryPath}, ${ghAccount.name}")
                        toolWindow.contentManager.addContent(
                            toolWindow.contentManager.factory.createContent(JPanel(null), repo.repositoryPath, false)
                                .apply {
                                    isCloseable = false
                                    setDisposer(Disposer.newDisposable("GitHubWorkflow tab disposable"))

                                    this.putUserData(
                                        WorkflowToolWindowTabController.KEY,
                                        WorkflowToolWindowTabController(
                                            project,
                                            settingsService.state.customRepos[repo.gitRemoteUrlCoordinates.url]!!,
                                            repo,
                                            ghAccount,
                                            dataContextRepository,
                                            this
                                        )
                                    )
                                })
                    } else {
                        val emptyTextPanel = JBPanelWithEmptyText()
                        emptyTextPanel.emptyText
                            .appendText("GitHub account not configured for $repo, go to settings to fix")
                            .appendSecondaryText(
                                "Go to Settings",
                                SimpleTextAttributes.LINK_ATTRIBUTES,
                                ActionUtil.createActionListener(
                                    "ShowGithubSettings",
                                    emptyTextPanel,
                                    ActionPlaces.UNKNOWN
                                )
                            )
                        toolWindow.contentManager.addContent(
                            toolWindow.contentManager.factory.createContent(emptyTextPanel, repo.repositoryPath, false))
                    }
                }
        }

    companion object {
        //        const val ID = "GitHub Workflows"
        private val LOG = logger<ToolWindowFactory>()
    }
}