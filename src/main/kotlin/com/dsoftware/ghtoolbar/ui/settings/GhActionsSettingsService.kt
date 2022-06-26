package com.dsoftware.ghtoolbar.ui.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

data class ToolbarSettings(
    var useCustomRepos: Boolean = true,
    var customRepos: MutableMap<String, RepoSettings> = mutableMapOf()
) {
    data class RepoSettings(var included: Boolean = true)
}

/**
 * Supports storing the application settings in a persistent way.
 * The [ToolbarSettings] and [Storage] annotations define the name of the data and the file name where
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
class GhActionsSettingsService : PersistentStateComponent<ToolbarSettings> {
    private var state = ToolbarSettings()

    override fun getState(): ToolbarSettings {
        return state
    }

    override fun loadState(state: ToolbarSettings) {
        this.state = state
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): GhActionsSettingsService {
            return project.getService(GhActionsSettingsService::class.java)
        }
    }
}