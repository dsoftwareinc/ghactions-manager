package com.dsoftware.ghmanager.ui.panels.filters


import com.dsoftware.ghmanager.data.WorkflowRunSelectionContext
import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModelBase
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount


internal class WfRunsSearchPanelViewModel(
    scope: CoroutineScope,
    context: WorkflowRunSelectionContext,
) : ReviewListSearchPanelViewModelBase<WfRunsListSearchValue, WorkflowRunListQuickFilter>(
    scope, WfRunsSearchHistoryModel(context.project.service<WfRunsListPersistentSearchHistory>()),
    emptySearch = WfRunsListSearchValue.EMPTY,
    defaultQuickFilter = WorkflowRunListQuickFilter.StartedByYou(context.account)
) {

    override fun WfRunsListSearchValue.withQuery(query: String?) = copy(searchQuery = query)

    override val quickFilters: List<WorkflowRunListQuickFilter> = listOf(
        WorkflowRunListQuickFilter.StartedByYou(context.account),
    )

}

sealed class WorkflowRunListQuickFilter(user: GithubAccount) : ReviewListQuickFilter<WfRunsListSearchValue> {
    protected val userLogin = user.name

    data class StartedByYou(val user: GithubAccount) : WorkflowRunListQuickFilter(user) {
        override val filter = WfRunsListSearchValue(user = userLogin)
    }

}