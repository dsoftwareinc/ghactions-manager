package com.dsoftware.ghmanager

import com.dsoftware.ghmanager.data.GhActionsService
import com.dsoftware.ghmanager.ui.GhActionsMgrToolWindowContent
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.dsoftware.ghmanager.ui.settings.GithubActionsManagerSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.common.initTestApplication
import com.intellij.testFramework.junit5.RunInEdt
import com.intellij.testFramework.registerServiceInstance
import com.intellij.testFramework.rules.ProjectModelExtension
import com.intellij.testFramework.waitUntil
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.UIUtil
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.jetbrains.plugins.github.api.GithubServerPath
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.RegisterExtension

@RunInEdt(writeIntent = true)
abstract class GitHubActionsManagerBaseTest {
    init {
        initTestApplication()
    }

    private val host: GithubServerPath = GithubServerPath.from("github.com")
    private lateinit var testInfo: TestInfo

    @JvmField
    @RegisterExtension
    protected val projectRule: ProjectModelExtension = ProjectModelExtension()

    protected lateinit var toolWindow: ToolWindow
    protected lateinit var toolWindowContent: GhActionsMgrToolWindowContent

    @BeforeEach
    open fun setUp(testInfo: TestInfo) {
        this.testInfo = testInfo
        val toolWindowManager = ToolWindowHeadlessManagerImpl(projectRule.project)
        toolWindow = toolWindowManager.doRegisterToolWindow("GitHub Actions")
        Disposer.register(projectRule.project, toolWindow.disposable)
    }

    @AfterEach
    open fun tearDown() {
        val toolWindowManager = ToolWindowHeadlessManagerImpl(projectRule.project)
        toolWindowManager.unregisterToolWindow("GitHub Actions")
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)
        TestApplicationManager.tearDownProjectAndApp(projectRule.project)
    }


    fun mockGhActionsService(repoUrls: Set<String>, accountNames: Collection<String>) {
        val accounts = accountNames.map { GHAccountManager.createAccount(it, host) }.toSet()
        val repos: Set<GHGitRepositoryMapping> = repoUrls.map {
            mockk<GHGitRepositoryMapping>().apply {
                every { remote.url } returns it
                every { repository.serverPath } returns host
                every { repositoryPath } returns it.replace("http://github.com/", "")
            }
        }.toSet()

        projectRule.project.registerServiceInstance(GhActionsService::class.java, object : GhActionsService {
            override val coroutineScope: CoroutineScope
                get() = CoroutineScope(Dispatchers.Default)
            override val knownRepositoriesState: StateFlow<Set<GHGitRepositoryMapping>>
                get() = MutableStateFlow(repos)
            override val knownRepositories: Set<GHGitRepositoryMapping>
                get() = repos
            override val gitHubAccounts: Set<GithubAccount>
                get() = accounts
            override val accountsState: StateFlow<Collection<GithubAccount>>
                get() = MutableStateFlow(accounts)
            override val toolWindowsJobMap: MutableMap<ToolWindow, Job>
                get() = mutableMapOf()
        })
    }

    fun mockSettingsService(settings: GithubActionsManagerSettings) {
        val settingsService = mockk<GhActionsSettingsService> {
            every { state } returns settings
        }
        projectRule.project.registerServiceInstance(GhActionsSettingsService::class.java, settingsService)
    }

}

@RequiresEdt
fun executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project: Project) {
    repeat(3) {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        runWithModalProgressBlocking(project, "") {
            yield()
        }
        UIUtil.dispatchAllInvocationEvents()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
}