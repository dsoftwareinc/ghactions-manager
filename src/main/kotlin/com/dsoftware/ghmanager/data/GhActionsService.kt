package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.intellij.collaboration.auth.Account
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.remote.GitRemoteUrlCoordinates
import git4idea.remote.hosting.knownRepositories
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GHHostedRepositoriesManager
import org.jetbrains.plugins.github.util.LazyCancellableBackgroundProcessValue

@Service(Service.Level.PROJECT)
class GhActionsService(project: Project) {

    val repositoriesManager = project.service<GHHostedRepositoriesManager>()
    val accountManager = service<GHAccountManager>()

    val knownRepositoriesState: kotlinx.coroutines.flow.StateFlow<Set<org.jetbrains.plugins.github.util.GHGitRepositoryMapping>>
        get() = repositoriesManager.knownRepositoriesState
    val knownRepositories: Set<GHGitRepositoryMapping>
        get() = repositoriesManager.knownRepositories
    val accountsState: StateFlow<Collection<GithubAccount>>
        get() = accountManager.accountsState
}