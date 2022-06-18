package com.dsoftware.ghtoolbar.ui

import com.dsoftware.ghtoolbar.workflow.data.WorkflowDataContextRepository
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanelWithEmptyText
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GHProjectRepositoriesManager
import javax.swing.JPanel


class GhActionsToolWindowFactory : ToolWindowFactory {
    private val authManager = GithubAuthenticationManager.getInstance()
    private val basePanel: JPanel? = null
    private var knownRepositories: Set<GHGitRepositoryMapping> = emptySet()

    override fun init(toolWindow: ToolWindow) {
        ApplicationManager.getApplication().messageBus.connect(toolWindow.disposable)
            .subscribe(
                GHProjectRepositoriesManager.LIST_CHANGES_TOPIC,
                object : GHProjectRepositoriesManager.ListChangeListener {
                    override fun repositoryListChanged(newList: Set<GHGitRepositoryMapping>, project: Project) {
                        LOG.info("Repos updated, new list has ${newList.size} repos")
                        toolWindow.isAvailable = newList.isNotEmpty()
                        knownRepositories = newList
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

    fun noGitHubAccountView(toolWindow: ToolWindow) =
        with(toolWindow.contentManager) {
            LOG.info("No GitHub account configured")
            val emptyTextPanel = JBPanelWithEmptyText()
                .withEmptyText("GitHub account not configured, go to settings to fix")

            addContent(factory.createContent(emptyTextPanel, "Workflows", false)
                .apply {
                    isCloseable = false
                    setDisposer(Disposer.newDisposable("GitHubWorkflow tab disposable"))
                }
            )
        }

    fun noRepositories(toolWindow: ToolWindow) =
        with(toolWindow.contentManager) {
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

    fun ghAccountAndReposConfigured(project: Project, toolWindow: ToolWindow, ghAccount: GithubAccount) =
        with(toolWindow.contentManager) {
            val dataContextRepository = WorkflowDataContextRepository.getInstance(project)
            LOG.info(
                "createToolWindowContent github account: ${ghAccount.name}, " +
                    "${knownRepositories.size} repositories"
            )
            knownRepositories.forEach { repo ->
                LOG.info("adding panel for repo: ${repo.repositoryPath}")
                addContent(factory.createContent(JPanel(null), repo.repositoryPath, false)
                    .apply {
                        isCloseable = false
                        setDisposer(Disposer.newDisposable("GitHubWorkflow tab disposable"))

                        this.putUserData(
                            WorkflowToolWindowTabController.KEY,
                            WorkflowToolWindowTabController(
                                project,
                                repo,
                                ghAccount,
                                dataContextRepository,
                                this
                            )
                        )
                    })
            }
        }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) =
        with(toolWindow.contentManager) {
            removeAllContents(true)
            val ghAccount = authManager.getSingleOrDefaultAccount(project)
            if (ghAccount == null) {
                noGitHubAccountView(toolWindow)
            } else if (knownRepositories.isEmpty()) {
                noRepositories(toolWindow)
            } else {
                ghAccountAndReposConfigured(project, toolWindow, ghAccount)
            }
        }

    companion object {
        const val ID = "GitHub Workflows"
        private val LOG = logger<ToolWindowFactory>()
    }
}