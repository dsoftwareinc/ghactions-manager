package com.dsoftware.ghmanager.ui.panels.filters


import com.dsoftware.ghmanager.data.WorkflowRunSelectionContext
import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModelBase
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope


internal class WfRunsSearchPanelViewModel(
    scope: CoroutineScope,
    val context: WorkflowRunSelectionContext,
) : ReviewListSearchPanelViewModelBase<WfRunsListSearchValue, WorkflowRunListQuickFilter>(
    scope,
    WfRunsSearchHistoryModel(context.project.service<WfRunsListPersistentSearchHistory>()),
    emptySearch = WfRunsListSearchValue.EMPTY,
    defaultQuickFilter = WorkflowRunListQuickFilter.All(),
) {

    val branches
        get() = context.runsListLoader.repoBranches
    val collaborators
        get() = context.runsListLoader.repoCollaborators
    val workflowTypes
        get() = context.runsListLoader.workflowTypes
    override fun WfRunsListSearchValue.withQuery(query: String?) = copy(searchQuery = query)

    override val quickFilters: List<WorkflowRunListQuickFilter> = listOf(
        WorkflowRunListQuickFilter.All(),
    )

    val branchFilterState = searchState.partialState(WfRunsListSearchValue::branch) { copy(branch = it) }
    val eventFilterState = searchState.partialState(WfRunsListSearchValue::event) { copy(event = it) }
    val userFilterState = searchState.partialState(WfRunsListSearchValue::actor) { copy(actor = it) }
    val statusState = searchState.partialState(WfRunsListSearchValue::status) { copy(status = it) }
    val workflowType = searchState.partialState(WfRunsListSearchValue::workflowType) { copy(workflowType = it) }
}

sealed class WorkflowRunListQuickFilter(val title: String) : ReviewListQuickFilter<WfRunsListSearchValue> {

    class All : WorkflowRunListQuickFilter("All workflow runs") {
        override val filter = WfRunsListSearchValue()
    }
}