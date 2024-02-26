package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.api.WorkflowRunFilter
import com.dsoftware.ghmanager.data.providers.SingleRunDataLoader
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.remote.GitRemoteUrlCoordinates
import org.jetbrains.plugins.github.api.GHRepositoryPath
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.GithubServerPath
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.exceptions.GithubMissingTokenException
import org.jetbrains.plugins.github.util.GHCompatibilityUtil
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GithubUrlUtil
import org.jetbrains.plugins.github.util.LazyCancellableBackgroundProcessValue
import java.io.IOException
import java.util.concurrent.CompletableFuture

data class RepositoryCoordinates(val serverPath: GithubServerPath, val repositoryPath: GHRepositoryPath)

@Service(Service.Level.PROJECT)
class WorkflowDataContextService(private val project: Project) {
    private val settingsService = project.service<GhActionsSettingsService>()
    val repositories =
        mutableMapOf<GitRemoteUrlCoordinates, LazyCancellableBackgroundProcessValue<WorkflowRunSelectionContext>>()

    @RequiresEdt
    fun clearContext(repositoryMapping: GHGitRepositoryMapping) {
        repositories.remove(repositoryMapping.remote)?.drop()
    }

    @RequiresEdt
    fun acquireContext(
        disposable: CheckedDisposable,
        repositoryMapping: GHGitRepositoryMapping,
        account: GithubAccount,
        toolWindow: ToolWindow,
    ): CompletableFuture<WorkflowRunSelectionContext> {
        return repositories.getOrPut(repositoryMapping.remote) {
            LazyCancellableBackgroundProcessValue.create { indicator ->
                ProgressManager.getInstance().submitIOTask(indicator) {
                    try {
                        getContext(disposable, account, repositoryMapping, toolWindow)
                    } catch (e: Exception) {
                        if (e !is ProcessCanceledException)
                            LOG.warn("Error occurred while creating data context", e)
                        throw e
                    }
                }.successOnEdt { ctx ->
                    Disposer.register(disposable, ctx)
                    ctx
                }
            }
        }.value
    }

    @RequiresBackgroundThread
    @Throws(IOException::class)
    private fun getContext(
        checkedDisposable: CheckedDisposable,
        account: GithubAccount,
        repositoryMapping: GHGitRepositoryMapping,
        toolWindow: ToolWindow,
    ): WorkflowRunSelectionContext {
        val fullPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(repositoryMapping.remote.url)
            ?: throw IllegalArgumentException(
                "Invalid GitHub Repository URL - ${repositoryMapping.remote.url} is not a GitHub repository"
            )
        val repositoryCoordinates = RepositoryCoordinates(account.server, fullPath)
        val token = if (settingsService.state.useGitHubSettings) {
            GHCompatibilityUtil.getOrRequestToken(account, toolWindow.project)
                ?: throw GithubMissingTokenException(account)
        } else {
            settingsService.state.apiToken
        }

        val requestExecutor = GithubApiRequestExecutor.Factory.Companion.getInstance().create(token = token)
        val singleRunDataLoader = SingleRunDataLoader(requestExecutor)
        requestExecutor.addListener(singleRunDataLoader) {
            singleRunDataLoader.invalidateAllData()
        }
        val runsLoader = WorkflowRunListLoader(
            project,
            checkedDisposable,
            requestExecutor,
            repositoryCoordinates,
            WorkflowRunFilter(),
        )

        return WorkflowRunSelectionContext(
            checkedDisposable,
            toolWindow.project,
            account,
            singleRunDataLoader,
            runsLoader,
            repositoryMapping,
            requestExecutor,
        )
    }

    companion object {
        private val LOG = logger<WorkflowDataContextService>()
        fun getInstance(project: Project) = project.service<WorkflowDataContextService>()
    }
}