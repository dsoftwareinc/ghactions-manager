package com.dsoftware.ghmanager.ui.panels.wfruns.filters


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
    WfRunsSearchHistoryModel(context.toolWindow.project.service<WfRunsListPersistentSearchHistory>()),
    emptySearch = WfRunsListSearchValue.EMPTY,
    defaultQuickFilter = WorkflowRunListQuickFilter.CurrentBranch(context.currentBranchName),
) {
    val branches
        get() = context.runsListLoader.repoBranches
    val collaborators
        get() = context.runsListLoader.repoCollaborators
    val workflowTypes
        get() = context.runsListLoader.workflowTypes

    override fun WfRunsListSearchValue.withQuery(query: String?) = copy(searchQuery = query)

    override var quickFilters: List<WorkflowRunListQuickFilter> = listOf(
        WorkflowRunListQuickFilter.CurrentBranch(context.currentBranchName),
        WorkflowRunListQuickFilter.CurrentUser(context),
        WorkflowRunListQuickFilter.All(),
        WorkflowRunListQuickFilter.InProgres(),
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

    class InProgres : WorkflowRunListQuickFilter("Runs in progress") {
        override val filter = WfRunsListSearchValue(status = WfRunsListSearchValue.Status.IN_PROGRESS)
    }

    class CurrentUser(
        private val context: WorkflowRunSelectionContext
    ) : WorkflowRunListQuickFilter("Started by me") {
        override val filter
            get() = WfRunsListSearchValue(actor = context.getCurrentAccountGHUser())
    }

    class CurrentBranch(branch: String?) : WorkflowRunListQuickFilter("Runs for current branch") {
        override val filter = WfRunsListSearchValue(branch = branch, currentBranchFilter = true)
    }

}