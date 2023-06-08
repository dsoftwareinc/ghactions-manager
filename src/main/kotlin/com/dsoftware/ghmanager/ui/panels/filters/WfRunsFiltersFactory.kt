package com.dsoftware.ghmanager.ui.panels.filters

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory
import kotlinx.coroutines.CoroutineScope
import javax.swing.JComponent

internal class WfRunsFiltersFactory(vm: WfRunsSearchPanelViewModel) :
    ReviewListSearchPanelFactory<WfRunsListSearchValue, WorkflowRunListQuickFilter, WfRunsSearchPanelViewModel>(vm) {
    override fun createFilters(viewScope: CoroutineScope): List<JComponent> {
        TODO("Not yet implemented")
    }

    override fun WorkflowRunListQuickFilter.getQuickFilterTitle(): String {
        TODO("Not yet implemented")
    }

    override fun getShortText(searchValue: WfRunsListSearchValue): String {
        TODO("Not yet implemented")
    }
}