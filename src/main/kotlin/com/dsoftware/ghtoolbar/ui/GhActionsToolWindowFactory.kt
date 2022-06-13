package com.dsoftware.ghtoolbar.ui

import com.dsoftware.ghtoolbar.workflow.data.WorkflowDataContextRepository
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
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

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) =
        with(toolWindow.contentManager) {
            removeAllContents(true)
            val ghAccount = authManager.getSingleOrDefaultAccount(project)
            if (ghAccount == null) {
                addContent(factory.createContent(JPanel(), "Workflows", false)
                    .apply {
                        isCloseable = false
                        setDisposer(Disposer.newDisposable("GitHubWorkflow tab disposable"))
                    }.also {
                        //TODO Link to github settings
                    }
                )
                return
            }

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
                    }.also {
                        it.putUserData(
                            WorkflowToolWindowTabController.KEY,
                            WorkflowToolWindowTabController(
                                project,
                                repo,
                                ghAccount,
                                dataContextRepository,
                                it
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