package com.dsoftware.ghmanager.ui.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

data class ToolbarSettings(
    var useCustomRepos: Boolean = true,
    var customRepos: MutableMap<String, RepoSettings> = mutableMapOf()
) {
    data class RepoSettings(
        var included: Boolean = true,
        var customName: String = ""
    )
}

@Service
@State(
    name = "GhActionsManagerSettings",
    storages = [
        Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)
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
        fun getInstance(project: Project): GhActionsSettingsService = project.service()
    }
}