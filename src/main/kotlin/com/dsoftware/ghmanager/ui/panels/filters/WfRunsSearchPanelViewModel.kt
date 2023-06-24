package com.dsoftware.ghmanager.ui.panels.filters


import com.dsoftware.ghmanager.data.RepositoryCoordinates
import com.dsoftware.ghmanager.data.WorkflowRunSelectionContext
import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModelBase
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import org.jetbrains.plugins.github.api.GithubApiRequests
import org.jetbrains.plugins.github.api.data.GHUser
import org.jetbrains.plugins.github.api.util.GithubApiPagesLoader
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.util.LazyCancellableBackgroundProcessValue
import java.util.concurrent.CompletableFuture


internal class WfRunsSearchPanelViewModel(
    scope: CoroutineScope,
    val context: WorkflowRunSelectionContext,
) : ReviewListSearchPanelViewModelBase<WfRunsListSearchValue, WorkflowRunListQuickFilter>(
    scope,
    WfRunsSearchHistoryModel(context.project.service<WfRunsListPersistentSearchHistory>()),
    emptySearch = WfRunsListSearchValue.EMPTY,
    defaultQuickFilter = WorkflowRunListQuickFilter.StartedByYou(context.account)
) {

    private val repoCoordinates = RepositoryCoordinates(
        context.repositoryMapping.repository.serverPath, context.repositoryMapping.repository.repositoryPath
    )


    override fun WfRunsListSearchValue.withQuery(query: String?) = copy(searchQuery = query)

    override val quickFilters: List<WorkflowRunListQuickFilter> = listOf(
        WorkflowRunListQuickFilter.StartedByYou(context.account),
    )

    val branchFilterState = searchState.partialState(WfRunsListSearchValue::branch) {
        copy(branch = it)
    }

    val userFilterState = searchState.partialState(WfRunsListSearchValue::user) {
        copy(user = it)
    }
    val reviewStatusState = searchState.partialState(WfRunsListSearchValue::status) {
        copy(status = it)
    }


    private val collaborators: CompletableFuture<List<GHUser>>
        get() = LazyCancellableBackgroundProcessValue.create(ProgressManager.getInstance()) { indicator ->
            GithubApiPagesLoader.loadAll(
                context.requestExecutor, indicator, GithubApiRequests.Repos.Collaborators.pages(
                    repoCoordinates.serverPath,
                    repoCoordinates.repositoryPath.owner,
                    repoCoordinates.repositoryPath.repository
                )
            )
        }.value.thenApply { list ->
            list.map { GHUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) }
        }

    suspend fun getCollaborators(): List<GHUser> = collaborators.await()

    private val branches: CompletableFuture<List<String>>
        get() = LazyCancellableBackgroundProcessValue.create(ProgressManager.getInstance()) { indicator ->
            GithubApiPagesLoader.loadAll(
                context.requestExecutor, indicator, GithubApiRequests.Repos.Branches.pages(
                    repoCoordinates.serverPath,
                    repoCoordinates.repositoryPath.owner,
                    repoCoordinates.repositoryPath.repository
                )
            ).map { it -> it.name }
        }.value

    suspend fun getBranches(): List<String> = branches.await()

}

sealed class WorkflowRunListQuickFilter(user: GithubAccount) : ReviewListQuickFilter<WfRunsListSearchValue> {
    protected val userLogin = user.name

    data class StartedByYou(val user: GithubAccount) : WorkflowRunListQuickFilter(user) {
        override val filter = WfRunsListSearchValue(user = userLogin)
    }

}