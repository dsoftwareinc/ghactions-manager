package com.dsoftware.ghtoolbar.ui

import com.dsoftware.ghtoolbar.workflow.data.WorkflowDataContextRepository
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GHProjectRepositoriesManager
import javax.swing.JPanel


class ToolWindowFactory : ToolWindowFactory {

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
            addContent(factory.createContent(JPanel(null), null, false)
                .apply {
                    isCloseable = false
                    setDisposer(Disposer.newDisposable("GitHubWorkflow tab disposable"))
                }.also {
                    val authManager = GithubAuthenticationManager.getInstance()
                    val repositoryManager = project.service<GHProjectRepositoriesManager>()
                    val dataContextRepository = WorkflowDataContextRepository.getInstance(project)
                    it.putUserData(
                        WorkflowToolWindowTabController.KEY,
                        WorkflowToolWindowTabControllerImpl(
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