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
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.yield
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.GithubServerPath
import org.jetbrains.plugins.github.authentication.GHAccountsUtil
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GHHostedRepositoriesManager


/**
 *
 * The base class for JUnit platform tests of the github plugin.<br></br>
 * Extend this test to write a test on GitHub which has the following features/limitations:
 *
 *  * This is a "platform test case", which means that IDEA "almost" production platform is set up before the test starts.
 *  * Project base directory is the root of everything.
 *
 *
 * All tests inherited from this class are required to have a token to access the Github server.
 * They are set up in Environment variables: <br></br>
 * `idea_test_github_host<br></br>
 * idea_test_github_token1<br></br> // token test user
 * idea_test_github_token2` // token user with configured test repositories
 *
 */
abstract class GitHubActionsManagerBaseTest : BasePlatformTestCase() {
    private lateinit var ghRepositoryManager: GHHostedRepositoriesManager

    protected lateinit var myProject: Project
    protected lateinit var factory: GhActionsToolWindowFactory
    protected lateinit var toolWindow: ToolWindow

    protected val host: GithubServerPath = GithubServerPath.from("github.com")
    override fun setUp() {
        super.setUp()
        Dotenv.configure().load()
        val dotenv = dotenv()
        myProject = project
        factory = GhActionsToolWindowFactory()
        toolWindow = MockToolWindow(myProject)
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

    protected open fun setCurrentAccount(accountData: AccountData?) {
        GHAccountsUtil.setDefaultAccount(myProject, accountData?.account)
    }

    protected data class AccountData(
        val token: String,
        val account: GithubAccount,
        val username: String,
        val executor: GithubApiRequestExecutor,
        val repos: Set<String>,
    )


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