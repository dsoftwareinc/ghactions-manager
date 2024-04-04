package com.dsoftware.ghmanager.ui.panels.wfruns.filters

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import kotlinx.serialization.Serializable

@Service(Service.Level.PROJECT)
@State(
    name = "WfRunsListSearchHistory",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
    reportStatistic = false
)
internal class WfRunsListPersistentSearchHistory :
    SerializablePersistentStateComponent<WfRunsListPersistentSearchHistory.HistoryState>(
        HistoryState()
    ) {

    @Serializable
    data class HistoryState(
        val history: List<WfRunsListSearchValue> = mutableListOf(),
        val lastFilter: WfRunsListSearchValue? = null
    )

    var lastFilter: WfRunsListSearchValue?
        get() = state.lastFilter
        set(value) {
            updateState {
                it.copy(lastFilter = value, history = it.history + listOfNotNull(value))
            }
        }

    var history: List<WfRunsListSearchValue>
        get() = state.history.toList()
        set(value) {
            updateState {
                it.copy(history = value)
            }
        }
}
