package com.dsoftware.ghtoolbar.ui.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

data class ToolbarState(
    var useCustomRepos: Boolean = true,
    var customRepos: Map<String, RepoSettings> = mapOf()
) {
    data class RepoSettings(private var included: Boolean = true) {
        fun setIncluded(v: Boolean) {
            included = v
        }

        fun getIncluded(): Boolean = included
    }

}

/**
 * Supports storing the application settings in a persistent way.
 * The [ToolbarState] and [Storage] annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */

@Service
@State(
    name = "GhActionsToolbarSettings",
    storages = [
        Storage("ghactions-toolbar.xml")
    ],
    reportStatistic = false,
)
class ToolbarSettings : PersistentStateComponent<ToolbarState> {
    private var state = ToolbarState()

    override fun getState(): ToolbarState {
        return state
    }

    override fun loadState(state: ToolbarState) {
        this.state = state
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ToolbarSettings {
            return project.getService(ToolbarSettings::class.java)
        }
    }
}