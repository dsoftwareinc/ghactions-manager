package com.dsoftware.githubactionstab.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.dsoftware.githubactionstab.workflow.data.GitHubWorkflowDataContextRepository
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GHProjectRepositoriesManager
import javax.swing.JPanel


class GithubActionsToolWindowFactory : ToolWindowFactory {

    private val basePanel: JPanel? = null

    override fun init(toolWindow: ToolWindow) {
        ApplicationManager.getApplication().messageBus.connect(toolWindow.disposable)
            .subscribe(
                GHProjectRepositoriesManager.LIST_CHANGES_TOPIC,
                object : GHProjectRepositoriesManager.ListChangeListener {
                    override fun repositoryListChanged(newList: Set<GHGitRepositoryMapping>, project: Project) {
                        toolWindow.isAvailable = newList.isNotEmpty()
                    }
                })
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) = with(toolWindow as ToolWindowEx) {
        with(contentManager) {
            addContent(factory.createContent(JPanel(null), null, false).apply {
                isCloseable = false
                setDisposer(Disposer.newDisposable("GitHubWorkflow tab disposable"))
            }.also {
                val authManager = GithubAuthenticationManager.getInstance()
                val repositoryManager = project.service<GHProjectRepositoriesManager>()
                val dataContextRepository = GitHubWorkflowDataContextRepository.getInstance(project)
                it.putUserData(
                    GitHubWorkflowToolWindowTabController.KEY,
                    GitHubWorkflowToolWindowTabControllerImpl(
                        project,
                        authManager,
                        repositoryManager,
                        dataContextRepository,
                        it
                    )
                )
            })
        }
    }
}