package com.dsoftware.ghmanager.ui.panels.wfruns.filters

import com.intellij.collaboration.ui.codereview.list.search.PersistingReviewListSearchHistoryModel

internal class WfRunsSearchHistoryModel(private val persistentHistoryComponent: WfRunsListPersistentSearchHistory) :
    PersistingReviewListSearchHistoryModel<WfRunsListSearchValue>() {

    override var lastFilter: WfRunsListSearchValue?
        get() = persistentHistoryComponent.lastFilter
        set(value) {
            persistentHistoryComponent.lastFilter = value
        }

    override var persistentHistory: List<WfRunsListSearchValue>
        get() = persistentHistoryComponent.history
        set(value) {
            persistentHistoryComponent.history = value
        }
}