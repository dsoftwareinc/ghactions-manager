package com.dsoftware.ghmanager

import com.dsoftware.ghmanager.ui.GhActionsToolWindowFactory
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl.MockToolWindow
import com.intellij.util.ThrowableRunnable
import com.intellij.util.concurrency.annotations.RequiresEdt
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.GithubApiRequests
import org.jetbrains.plugins.github.api.GithubServerPath
import org.jetbrains.plugins.github.authentication.GHAccountsUtil
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
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
    protected lateinit var organisation: String
    protected lateinit var executor: GithubApiRequestExecutor
    protected lateinit var accountManager: GHAccountManager
    protected lateinit var repositoriesManager: GHHostedRepositoriesManager
    protected lateinit var mainAccount: AccountData

    protected lateinit var myProject: Project
    protected lateinit var factory: GhActionsToolWindowFactory
    protected lateinit var toolWindow: ToolWindow

    protected lateinit var host: GithubServerPath
    override fun setUp() {
        super.setUp()
        Dotenv.configure().load()
        val dotenv= dotenv()
        myProject = project
        factory = GhActionsToolWindowFactory()
        toolWindow = MockToolWindow(myProject)
        host = GithubServerPath.from(dotenv.get("idea_test_github_host") ?: dotenv.get("idea.test.github.host"))

        val token1 = dotenv.get("idea_test_github_token1") ?: dotenv.get("idea.test.github.token1")

        assertNotNull(token1)
        executor = service<GithubApiRequestExecutor.Factory>().create(token1)
        accountManager = service()
        repositoriesManager = project.service()

        organisation = dotenv.get("idea_test_github_org") ?: dotenv.get("idea.test.github.org")
        assertNotNull(organisation)
        mainAccount = createAccountData(token1)
        setCurrentAccount(mainAccount)
    }

    protected fun createAccountData(token: String): AccountData {
        val account = GHAccountManager.createAccount("token", host)
        val username = executor.execute(GithubApiRequests.CurrentUser.get(account.server)).login
        val repos = executor.execute(GithubApiRequests.CurrentUser.Repos.get(account.server))

        return AccountData(token, account, username, executor, repos.items.map { it.name }.toSet())
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


    @Throws(Exception::class)
    override fun tearDown() {
        RunAll(
            ThrowableRunnable { setCurrentAccount(null) },
            ThrowableRunnable { if (::accountManager.isInitialized) runBlocking { accountManager.updateAccounts(emptyMap()) } },
            ThrowableRunnable { super.tearDown() }
        ).run()
    }

//
//    private fun deleteRepos(account: AccountData, repos: Collection<String>) {
//        setCurrentAccount(account)
//        for (repo in repos) {
//            retry(LOG, true) {
//                account.executor.execute(GithubApiRequests.Repos.delete(repo))
//                val info = account.executor.execute(GithubApiRequests.Repos.get(repo))
//                check(info == null) { "Repository still exists" }
//            }
//        }
//    }

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