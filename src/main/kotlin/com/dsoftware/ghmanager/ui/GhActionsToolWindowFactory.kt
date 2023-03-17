package com.dsoftware.ghmanager.ui

import com.dsoftware.ghmanager.data.WorkflowDataContextRepository
import com.dsoftware.ghmanager.ui.settings.GhActionsManagerConfigurable
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.dsoftware.ghmanager.ui.settings.GithubActionsManagerSettings
import com.intellij.collaboration.async.collectWithPrevious
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
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
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
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
    private val accountManager = service<GHAccountManager>()
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

        val repositoriesManager = project.service<GHHostedRepositoriesManager>()
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
            accountManager.accountsState.collectWithPrevious(setOf()) { prev, current ->
                createToolWindowContent(toolWindow.project, toolWindow)
            }
        }

    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return true
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val projectRepos = projectReposMap[toolWindow.project] ?: return
        val disposable = Disposer.newDisposable("GitHubWorkflow tab disposable")
        Disposer.register(toolWindow.disposable, disposable)
        ApplicationManager.getApplication().invokeLater {
            toolWindow.contentManager.removeAllContents(true)
            if ((GHAccountsUtil.accounts.isEmpty() && settingsService.state.useGitHubSettings)
                || (!settingsService.state.useGitHubSettings && settingsService.state.apiToken == "")
            ) {
                noGitHubAccountPanel(disposable, projectRepos)
            } else if (projectRepos.knownRepositories.isEmpty()) {
                noRepositories(disposable, projectRepos)
            } else {
                val countRepos = projectRepos.knownRepositories.count {
                    settingsService.state.customRepos[it.remote.url]?.included ?: false
                }
                if (settingsService.state.useCustomRepos && countRepos == 0) {
                    noActiveRepositories(disposable, projectRepos)
                } else {
                    ghAccountAndReposConfigured(disposable, project, projectRepos)
                }
            }
        }
    }

    private fun noActiveRepositories(
        disposable: Disposable,
        projectRepositories: ProjectRepositories
    ) =
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

            addContent(
                factory.createContent(emptyTextPanel, "Workflows", false)
                    .apply {
                        isCloseable = false
                        setDisposer(disposable)
                    }
            )
        }

    private fun noGitHubAccountPanel(
        disposable: Disposable,
        projectRepositories: ProjectRepositories
    ) = with(projectRepositories.toolWindow.contentManager) {
        LOG.debug("No GitHub account configured")
        val emptyTextPanel = JBPanelWithEmptyText()
        emptyTextPanel.emptyText
            .appendText("GitHub account not configured and no API Token")
            .appendLine(
                "Go to github Settings",
                SimpleTextAttributes.LINK_ATTRIBUTES,
                ActionUtil.createActionListener(
                    "ShowGithubSettings",
                    emptyTextPanel,
                    ActionPlaces.UNKNOWN
                )
            )
            .appendLine(
                "Go to ghactions-manager Settings",
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
                setDisposer(disposable)
            }
        )
    }

    private fun noRepositories(
        disposable: Disposable,
        projectRepositories: ProjectRepositories
    ) = with(projectRepositories.toolWindow.contentManager) {
        LOG.debug("No git repositories in project")
        val emptyTextPanel = JBPanelWithEmptyText()
            .withEmptyText("No git repositories in project")

        addContent(factory.createContent(emptyTextPanel, "Workflows", false)
            .apply {
                isCloseable = false
                setDisposer(disposable)
            }
        )
    }

    private fun guessAccountForRepository(repo: GHGitRepositoryMapping): GithubAccount? {
        val accounts = GHAccountsUtil.accounts
        return accounts.firstOrNull { it.server.equals(repo.repository.serverPath, true) }
    }

    private fun ghAccountAndReposConfigured(
        parentDisposable: Disposable,
        project: Project,
        projectRepositories: ProjectRepositories
    ) =
        with(projectRepositories) {
            val actionManager = ActionManager.getInstance()
            toolWindow.setAdditionalGearActions(DefaultActionGroup(actionManager.getAction("Github.Actions.Manager.Settings.Open")))
            val dataContextRepository = WorkflowDataContextRepository.getInstance(project)
            knownRepositories.filter {
                !settingsService.state.useCustomRepos
                    || (settingsService.state.customRepos[it.remote.url]?.included
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
                        Disposer.register(parentDisposable, disposable)
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
                        .appendText("GitHub account not configured for ${repo.repository}, go to settings to fix")
                        .appendLine(
                            "Go to github accounts Settings",
                            SimpleTextAttributes.LINK_ATTRIBUTES,
                            ActionUtil.createActionListener(
                                "ShowGithubSettings",
                                emptyTextPanel,
                                ActionPlaces.UNKNOWN
                            )
                        )
                        .appendLine(
                            "Go to ghactions-manager Settings",
                            SimpleTextAttributes.LINK_ATTRIBUTES,
                            ActionUtil.createActionListener(
                                "Github.Actions.Manager.Settings.Open",
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