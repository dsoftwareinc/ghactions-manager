package com.dsoftware.githubactionstab.workflow.data

import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.dsoftware.githubactionstab.api.GitHubWorkflowRun
import com.dsoftware.githubactionstab.workflow.GitHubRepositoryCoordinates
import org.jetbrains.plugins.github.api.GHRepositoryCoordinates
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.pullrequest.data.GHListLoader
import org.jetbrains.plugins.github.util.GitRemoteUrlCoordinates
import org.jetbrains.plugins.github.util.GithubUrlUtil
import org.jetbrains.plugins.github.util.LazyCancellableBackgroundProcessValue
import java.io.IOException
import java.util.concurrent.CompletableFuture

@Service
internal class GitHubWorkflowDataContextRepository {

    private val repositories =
        mutableMapOf<GHRepositoryCoordinates, LazyCancellableBackgroundProcessValue<GitHubWorkflowRunDataContext>>()

    @RequiresBackgroundThread
    @Throws(IOException::class)
    fun getContext(
        disposable: Disposable,
        account: GithubAccount,
        requestExecutor: GithubApiRequestExecutor,
        gitRemoteCoordinates: GitRemoteUrlCoordinates,
    ): GitHubWorkflowRunDataContext {
        LOG.debug("Get GitHubWorkflowRunDataContext")
        LOG.debug("Get User and  repository")
        val fullPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(gitRemoteCoordinates.url)
            ?: throw IllegalArgumentException(
                "Invalid GitHub Repository URL - ${gitRemoteCoordinates.url} is not a GitHub repository"
            )

        val repositoryCoordinates = GitHubRepositoryCoordinates(account.server, fullPath)

        LOG.debug("Create GitHubWorkflowDataLoader")
        val githubWorkflowDataLoader = GitHubWorkflowDataLoader {
            GitHubWorkflowRunDataProvider(ProgressManager.getInstance(), requestExecutor, it)
        }

        requestExecutor.addListener(githubWorkflowDataLoader) {
            githubWorkflowDataLoader.invalidateAllData()
        }

        LOG.debug("Create CollectionListModel<GitHubWorkflowRun>() and loader")
        val listModel = CollectionListModel<GitHubWorkflowRun>()

        val listLoader = GitHubWorkflowRunListLoader(
            ProgressManager.getInstance(), requestExecutor,
            repositoryCoordinates,
            listModel
        )

        listLoader.addDataListener(disposable, object : GHListLoader.ListDataListener {
            override fun onDataAdded(startIdx: Int) {
                val loadedData = listLoader.loadedData
                listModel.add(loadedData.subList(startIdx, loadedData.size))
            }
        })

        return GitHubWorkflowRunDataContext(
            repositoryCoordinates,
            listModel,
            githubWorkflowDataLoader,
            listLoader,
            account
        )
    }

    @RequiresEdt
    fun acquireContext(
        repository: GHRepositoryCoordinates, remote: GitRemoteUrlCoordinates,
        account: GithubAccount, requestExecutor: GithubApiRequestExecutor,
    ): CompletableFuture<GitHubWorkflowRunDataContext> {
        return repositories.getOrPut(repository) {
            val contextDisposable = Disposer.newDisposable()
            LazyCancellableBackgroundProcessValue.create { indicator ->
                ProgressManager.getInstance().submitIOTask(indicator) {
                    try {
                        getContext(contextDisposable, account, requestExecutor, remote)
                    } catch (e: Exception) {
                        if (e !is ProcessCanceledException) LOG.info("Error occurred while creating data context", e)
                        throw e
                    }
                }.successOnEdt { ctx ->
                    if (Disposer.isDisposed(contextDisposable)) {
                        Disposer.dispose(ctx)
                    } else {
                        Disposer.register(contextDisposable, ctx)
                    }
                    ctx
                }
            }.also {
                it.addDropEventListener {
                    Disposer.dispose(contextDisposable)
                }
            }
        }.value
    }

    @RequiresEdt
    fun clearContext(repository: GHRepositoryCoordinates) {
        repositories.remove(repository)?.drop()
    }

    companion object {
        private val LOG = Logger.getInstance("com.dsoftware.githubactionstab")

        fun getInstance(project: Project) = project.service<GitHubWorkflowDataContextRepository>()
    }
}