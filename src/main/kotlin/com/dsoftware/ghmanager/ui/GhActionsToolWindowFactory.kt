package com.dsoftware.ghmanager.ui

import com.dsoftware.ghmanager.data.WorkflowDataContextRepository
import com.dsoftware.ghmanager.ui.settings.GhActionsManagerConfigurable
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.dsoftware.ghmanager.ui.settings.GithubActionsManagerSettings
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.plugins.github.authentication.GHAccountsUtil
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.pullrequest.data.provider.GHPRDataOperationsListener
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GHHostedRepositoriesManager
import java.awt.BorderLayout
import java.util.concurrent.TimeUnit
import javax.swing.JPanel

internal class ProjectRepositories(val toolWindow: ToolWindow) {
    var knownRepositories: Set<GHGitRepositoryMapping> = emptySet()
}

class GhActionsToolWindowFactory : ToolWindowFactory, DumbAware {
    private lateinit var settingsService: GhActionsSettingsService

    //    private val authManager = GithubAuthenticationManager.getInstance()
    private val projectReposMap = mutableMapOf<Project, ProjectRepositories>()
    private val scope = CoroutineScope(SupervisorJob())

    override fun init(toolWindow: ToolWindow) {
        val project = toolWindow.project
        if (!projectReposMap.containsKey(toolWindow.project)) {
            projectReposMap[toolWindow.project] = ProjectRepositories(toolWindow)
        }
        settingsService = GhActionsSettingsService.getInstance(toolWindow.project)
        val bus = ApplicationManager.getApplication().messageBus.connect(toolWindow.disposable)
        bus.subscribe(
            GhActionsManagerConfigurable.SETTINGS_CHANGED,
            object : GhActionsManagerConfigurable.SettingsChangedListener {
                override fun settingsChanged() {
                    createToolWindowContent(toolWindow.project, toolWindow)
                }
            })

        val repositoriesManager = GHHostedRepositoriesManager(toolWindow.project)
        scope.launch {
            repositoriesManager.knownRepositoriesState.collect {
                LOG.debug("Repos updated, new list has ${it.size} repos")
                val ghActionToolWindow = projectReposMap[project]
                if (ghActionToolWindow != null) {
                    ghActionToolWindow.knownRepositories = it
                    ghActionToolWindow.knownRepositories.forEach { repo ->
                        settingsService.state.customRepos.putIfAbsent(
                            repo.remote.url,
                            GithubActionsManagerSettings.RepoSettings()
                        )
                    }
                    createToolWindowContent(project, toolWindow)
                }
            }
        }

        bus.subscribe(
            GHPRDataOperationsListener.TOPIC,
            object : GHPRDataOperationsListener {

            }
        )

//        authManager.addListener(toolWindow.disposable, object : AccountsListener<GithubAccount> {
//            override fun onAccountListChanged(old: Collection<GithubAccount>, new: Collection<GithubAccount>) =
//                scheduleUpdate()
//
//            override fun onAccountCredentialsChanged(account: GithubAccount) = scheduleUpdate()
//
//            private fun scheduleUpdate() = createToolWindowContent(toolWindow.project, toolWindow)
//
//        })
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return true;
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val projectRepos = projectReposMap[toolWindow.project] ?: return
        ApplicationManager.getApplication().invokeLater {
            toolWindow.contentManager.removeAllContents(true)
            if (GHAccountsUtil.accounts.isEmpty()) {
                noGitHubAccountPanel(projectRepos)
            } else if (projectRepos.knownRepositories.isEmpty()) {
                noRepositories(projectRepos)
            } else {
                val countRepos = projectRepos.knownRepositories.count {
                    settingsService.state.customRepos[it.remote.url]?.included ?: false
                }
                if (settingsService.state.useCustomRepos && countRepos == 0) {
                    noActiveRepositories(projectRepos)
                } else {
                    ghAccountAndReposConfigured(project, projectRepos)
                }
            }
        }
    }

    private fun noActiveRepositories(projectRepositories: ProjectRepositories) =
        with(projectRepositories.toolWindow.contentManager) {
            LOG.debug("No active repositories in project")
            val emptyTextPanel = JBPanelWithEmptyText()
            emptyTextPanel.emptyText
                .appendText("No repositories configured for GitHub-Actions-Manager")
                .appendSecondaryText(
                    "Go to GitHub-Actions-Manager settings",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    ActionUtil.createActionListener(
                        "Github.Actions.Manager.Settings.Open",
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

    private fun noGitHubAccountPanel(projectRepositories: ProjectRepositories) =
        with(projectRepositories.toolWindow.contentManager) {
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

    private fun noRepositories(projectRepositories: ProjectRepositories) =
        with(projectRepositories.toolWindow.contentManager) {
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
        val accounts = GHAccountsUtil.accounts
        return accounts.firstOrNull { it.server.equals(repo.repository.serverPath, true) }
    }

    private fun ghAccountAndReposConfigured(
        project: Project,
        projectRepositories: ProjectRepositories
    ) =
        with(projectRepositories) {
            val actionManager = ActionManager.getInstance()
            toolWindow.setAdditionalGearActions(DefaultActionGroup(actionManager.getAction("Github.Actions.Manager.Settings.Open")))
            val dataContextRepository = WorkflowDataContextRepository.getInstance(project)
            knownRepositories.filter {
                !settingsService.state.useCustomRepos || (settingsService.state.customRepos[it.gitRemoteUrlCoordinates.url]?.included
                    ?: false)
            }.forEach { repo ->
                val ghAccount = guessAccountForRepository(repo)
                if (ghAccount != null) {
                    LOG.info("adding panel for repo: ${repo.repositoryPath}, ${ghAccount.name}")
                    val repoSettings = settingsService.state.customRepos[repo.remote.url]!!
                    val tab = toolWindow.contentManager.factory.createContent(
                        JPanel(null), repo.repositoryPath, false
                    ).apply {
                        isCloseable = false
                        val disposable = Disposer.newDisposable("gha-manager ${repo.repositoryPath} tab disposable")
                        setDisposer(disposable)
                        displayName = repoSettings.customName.ifEmpty { repo.repositoryPath }
                        val controller = WorkflowToolWindowTabController(
                            project,
                            repo,
                            ghAccount,
                            dataContextRepository,
                            this.disposer!!,
                            toolWindow
                        )
                        component.apply {
                            layout = BorderLayout()
                            background = UIUtil.getListBackground()
                            removeAll()
                            add(controller.panel, BorderLayout.CENTER)
                            revalidate()
                            repaint()
                        }
                        putUserData(WorkflowToolWindowTabController.KEY, controller)
                    }

                    toolWindow.contentManager.addContent(tab)
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
                        toolWindow.contentManager.factory.createContent(emptyTextPanel, repo.repositoryPath, false)
                    )
                }
            }
            toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
                override fun selectionChanged(event: ContentManagerEvent) {
                    val content = event.content
                    val controller = content.getUserData(WorkflowToolWindowTabController.KEY)
                    LOG.debug("Got selectionChanged event: ${content.displayName}: controller=${controller != null}, isSelected=${content.isSelected}")
                    controller?.apply {
                        this.loadingModel.result?.runsListLoader?.refreshRuns = content.isSelected
                    }
                }
            })
            val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
            scheduler.schedule({
                toolWindow.contentManager.contents.forEach {
                    val controller = it.getUserData(WorkflowToolWindowTabController.KEY)
                    controller?.apply {
                        this.loadingModel.result?.runsListLoader?.refreshRuns = it.isSelected
                    }
                }
            }, 5, TimeUnit.SECONDS)

        }

    companion object {
        private val LOG = logger<GhActionsToolWindowFactory>()
    }
}