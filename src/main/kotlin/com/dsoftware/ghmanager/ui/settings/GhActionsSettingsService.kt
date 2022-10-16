package com.dsoftware.ghmanager.ui.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

data class GithubActionsManagerSettings(
    var useCustomRepos: Boolean = true,
    var customRepos: MutableMap<String, RepoSettings> = mutableMapOf(),
    var jobListAboveLogs: Boolean = true,
    var frequency: Long = 30,
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
class GhActionsSettingsService : PersistentStateComponent<GithubActionsManagerSettings> {
    private var state = GithubActionsManagerSettings()

    override fun getState(): GithubActionsManagerSettings {
        return state
    }

    override fun loadState(state: GithubActionsManagerSettings) {
        this.state = state
    }

    companion object {
        fun getInstance(project: Project): GhActionsSettingsService = project.service()
    }
}