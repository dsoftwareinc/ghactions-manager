package com.dsoftware.ghmanager.data

import com.dsoftware.ghmanager.ui.GhActionsMgrToolWindowContent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import git4idea.remote.hosting.knownRepositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GHHostedRepositoriesManager

interface GhActionsService {
    val coroutineScope: CoroutineScope
    val knownRepositoriesState: StateFlow<Set<GHGitRepositoryMapping>>
    val knownRepositories: Set<GHGitRepositoryMapping>
    val gitHubAccounts: Set<GithubAccount>
    val accountsState: StateFlow<Collection<GithubAccount>>
    val toolWindows: MutableSet<ToolWindow>
    fun guessAccountForRepository(repo: GHGitRepositoryMapping): GithubAccount? {
        return gitHubAccounts.firstOrNull { it.server.equals(repo.repository.serverPath, true) }
    }

    fun registerToolWindow(toolWindowContent: GhActionsMgrToolWindowContent) {
        if (toolWindows.contains(toolWindowContent.toolWindow)) {
            return
        }
        toolWindows.add(toolWindowContent.toolWindow)
        coroutineScope.launch {
            knownRepositoriesState.collect { toolWindowContent.createContent() }
        }
        coroutineScope.launch {
            accountsState.collect { toolWindowContent.createContent() }
        }
    }
}

open class GhActionsServiceImpl(project: Project, override val coroutineScope: CoroutineScope) : GhActionsService {
    private val repositoriesManager = project.service<GHHostedRepositoriesManager>()
    private val accountManager = service<GHAccountManager>()


    override val gitHubAccounts: Set<GithubAccount>
        get() = accountManager.accountsState.value
    override val knownRepositoriesState: StateFlow<Set<GHGitRepositoryMapping>>
        get() = repositoriesManager.knownRepositoriesState
    override val knownRepositories: Set<GHGitRepositoryMapping>
        get() = repositoriesManager.knownRepositories
    override val accountsState: StateFlow<Collection<GithubAccount>>
        get() = accountManager.accountsState
    override val toolWindows: MutableSet<ToolWindow> = mutableSetOf()

}