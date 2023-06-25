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

    val branches
        get() = context.runsListLoader.repoBranches
    val collaborators
        get() = context.runsListLoader.repoCollaborators

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
    val statusState = searchState.partialState(WfRunsListSearchValue::status) {
        copy(status = it)
    }

}

sealed class WorkflowRunListQuickFilter(user: GithubAccount) : ReviewListQuickFilter<WfRunsListSearchValue> {
    protected val userLogin = user.name

    data class StartedByYou(val user: GithubAccount) : WorkflowRunListQuickFilter(user) {
        override val filter = WfRunsListSearchValue(user = userLogin)
    }

}