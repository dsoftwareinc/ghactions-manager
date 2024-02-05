package com.dsoftware.ghmanager

import com.dsoftware.ghmanager.data.GhActionsService
import com.dsoftware.ghmanager.ui.GhActionsToolWindowFactory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.registerServiceInstance
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl.MockToolWindow
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.yield
import org.jetbrains.plugins.github.api.GithubServerPath
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping


abstract class GitHubActionsManagerBaseTest : BasePlatformTestCase() {
    protected lateinit var factory: GhActionsToolWindowFactory
    protected lateinit var toolWindow: ToolWindow

    protected val host: GithubServerPath = GithubServerPath.from("github.com")
    override fun setUp() {
        super.setUp()
        factory = GhActionsToolWindowFactory()
        toolWindow = MockToolWindow(project)
    }

    fun mockGhActionsService(repoUrls: Set<String>, accountNames: Collection<String>) {
        val accounts = accountNames.map { GHAccountManager.createAccount(it, host) }
        val repos: Set<GHGitRepositoryMapping> = emptySet()

        project.registerServiceInstance(GhActionsService::class.java, object : GhActionsService {
            override val knownRepositoriesState: StateFlow<Set<GHGitRepositoryMapping>>
                get() = MutableStateFlow(repos)
            override val knownRepositories: Set<GHGitRepositoryMapping>
                get() = repos
            override val accountsState: StateFlow<Collection<GithubAccount>>
                get() = MutableStateFlow(accounts)
        })
    }


    companion object {
        private const val RETRIES = 3

        internal fun retry(LOG: Logger, exception: Boolean = true, action: () -> Unit) {
            for (i in 1..RETRIES) {
                try {
                    LOG.debug("Attempt #$i")
                    return action()
                } catch (e: Throwable) {
                    if (i == RETRIES) {
                        if (exception) throw e
                        else {
                            LOG.error(e)
                            return
                        }
                    }
                    Thread.sleep(1000L)
                }
            }
        }
    }
}

@RequiresEdt
fun executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project: Project) {
    repeat(3) {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        runWithModalProgressBlocking(project, "") {
            yield()
        }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
}