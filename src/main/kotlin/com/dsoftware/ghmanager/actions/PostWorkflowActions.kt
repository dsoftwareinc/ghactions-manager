package com.dsoftware.ghmanager.actions

import com.dsoftware.ghmanager.api.GithubApi
import com.dsoftware.ghmanager.api.model.WorkflowType
import com.dsoftware.ghmanager.data.RepositoryCoordinates
import com.dsoftware.ghmanager.data.WorkflowRunSelectionContext
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.plugins.github.api.GithubApiRequests
import org.jetbrains.plugins.github.util.GithubUrlUtil
import javax.swing.Icon

abstract class PostUrlAction(
    private val text: String, description: String?, icon: Icon,
) : DumbAwareAction(text, description, icon) {
    private var context: WorkflowRunSelectionContext? = null
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val url = getUrl(e.dataContext)
        e.presentation.isEnabledAndVisible = url != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.dataContext.getData(CommonDataKeys.PROJECT) ?: return
        getUrl(e.dataContext)?.let {
            val request = GithubApi.postUrl(text, it, getData(e.dataContext))
            val context = e.getRequiredData(ActionKeys.ACTION_DATA_CONTEXT)
            val future = context.dataLoader.createDataProvider(request).request
            future.thenApply {
                afterPostUrl()
            }
        }
    }

    abstract fun getUrl(dataContext: DataContext): String?
    open fun getData(dataContext: DataContext): Any = Object()
    open fun afterPostUrl() {
        context.let { it?.resetAllData() }
    }
}

class CancelWorkflowAction : PostUrlAction("Cancel Workflow", null, AllIcons.Actions.Cancel) {
    override fun update(e: AnActionEvent) {
        val context = e.dataContext.getData(ActionKeys.SELECTED_WORKFLOW_RUN)
        val url = context?.cancelUrl
        val status = context?.status
        e.presentation.isEnabledAndVisible = (url != null) && (status == "in_progress" || status == "queued")
    }

    override fun getUrl(dataContext: DataContext): String? {
        dataContext.getData(CommonDataKeys.PROJECT) ?: return null
        return dataContext.getData(ActionKeys.SELECTED_WORKFLOW_RUN)?.cancelUrl
    }
}

class RerunWorkflowAction : PostUrlAction("Rerun Workflow", null, AllIcons.Actions.Rerun) {
    override fun getUrl(dataContext: DataContext): String? {
        dataContext.getData(CommonDataKeys.PROJECT) ?: return null
        return dataContext.getData(ActionKeys.SELECTED_WORKFLOW_RUN)?.rerunUrl
    }
}

class WorkflowDispatchAction(private val workflowType: WorkflowType) :
    PostUrlAction(workflowType.name, "Select workflow to dispatch", AllIcons.Actions.Execute) {
    override fun getUrl(dataContext: DataContext): String? {
        val context = dataContext.getData(ActionKeys.ACTION_DATA_CONTEXT) ?: return null
        val fullPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(context.repositoryMapping.remote.url)
            ?: throw IllegalArgumentException(
                "Invalid GitHub Repository URL - ${context.repositoryMapping.remote.url} is not a GitHub repository"
            )
        val repositoryCoordinates = RepositoryCoordinates(context.account.server, fullPath)
        return GithubApiRequests.getUrl(
            repositoryCoordinates.serverPath,
            GithubApi.urlSuffix,
            "/${repositoryCoordinates.repositoryPath}",
            "/actions",
            "/workflows",
            "/${workflowType.id}",
            "/dispatches",
        )
    }

    override fun getData(dataContext: DataContext): Any {
        val context = dataContext.getData(ActionKeys.ACTION_DATA_CONTEXT) ?: return Object()
        return mapOf("ref" to context.repositoryMapping.gitRepository.currentBranch?.name)
    }

    override fun afterPostUrl() {
    }
}