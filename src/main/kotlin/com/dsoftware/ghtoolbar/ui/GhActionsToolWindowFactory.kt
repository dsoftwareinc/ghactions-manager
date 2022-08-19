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
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GHProjectRepositoriesManager
import javax.swing.JPanel

internal class GhActionToolWindow(
    private val project: Project,
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
            toolWindowsMap[toolWindow.project] = GhActionToolWindow(toolWindow.project, toolWindow)
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
                    LOG.info("Repos updated, new list has ${newList.size} repos")
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
            val ghAccount = authManager.getSingleOrDefaultAccount(project)
            if (ghAccount == null) {
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
            ghAccountAndReposConfigured(project, ghActionToolWindow, ghAccount)
        }
    }

    private fun noActiveRepositories(ghActionToolWindow: GhActionToolWindow) =
        with(ghActionToolWindow.toolWindow.contentManager) {
            LOG.info("No active repositories in project")
            val emptyTextPanel = JBPanelWithEmptyText()
            emptyTextPanel.emptyText
                .appendText("No repositories configured for Github Actions Toolbar")
                .appendSecondaryText(
                    "Go to Toolbar Settings",
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
            LOG.info("No GitHub account configured")
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
            LOG.info("No git repositories in project")
            val emptyTextPanel = JBPanelWithEmptyText()
                .withEmptyText("No git repositories in project")

            addContent(factory.createContent(emptyTextPanel, "Workflows", false)
                .apply {
                    isCloseable = false
                    setDisposer(Disposer.newDisposable("GitHubWorkflow tab disposable"))
                }
            )
        }

    private fun ghAccountAndReposConfigured(
        project: Project,
        ghActionToolWindow: GhActionToolWindow,
        ghAccount: GithubAccount
    ) =
        with(ghActionToolWindow) {
            val actionManager = ActionManager.getInstance()
            toolWindow.setTitleActions(listOf(actionManager.getAction("ShowGhActionsToolbarSettings")))
            toolWindow.setAdditionalGearActions(DefaultActionGroup())
            val dataContextRepository = WorkflowDataContextRepository.getInstance(project)
            LOG.info(
                "createToolWindowContent github account: ${ghAccount.name}, " +
                    "${knownRepositories.size} repositories"
            )
            knownRepositories
                .filter { settingsService.state.customRepos[it.gitRemoteUrlCoordinates.url]?.included ?: false }
                .forEach {
                    LOG.info("adding panel for repo: ${it.repositoryPath}")
                    toolWindow.contentManager.addContent(
                        toolWindow.contentManager.factory.createContent(JPanel(null), it.repositoryPath, false)
                            .apply {
                                isCloseable = false
                                setDisposer(Disposer.newDisposable("GitHubWorkflow tab disposable"))

                                this.putUserData(
                                    WorkflowToolWindowTabController.KEY,
                                    WorkflowToolWindowTabController(
                                        project,
                                        it,
                                        ghAccount,
                                        dataContextRepository,
                                        this
                                    )
                                )
                            })
                }
        }

    companion object {
        const val ID = "GitHub Workflows"
        private val LOG = logger<ToolWindowFactory>()
    }
}