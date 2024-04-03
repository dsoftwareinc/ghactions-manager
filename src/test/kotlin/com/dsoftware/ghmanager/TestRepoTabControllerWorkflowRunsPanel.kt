package com.dsoftware.ghmanager

import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.dsoftware.ghmanager.api.model.WorkflowRuns
import com.dsoftware.ghmanager.api.model.WorkflowType
import com.dsoftware.ghmanager.api.model.WorkflowTypes
import com.dsoftware.ghmanager.data.WorkflowDataContextService
import com.dsoftware.ghmanager.ui.GhActionsMgrToolWindowContent
import com.dsoftware.ghmanager.ui.panels.wfruns.WorkflowRunsListPanel
import com.intellij.openapi.components.service
import com.intellij.testFramework.waitUntil
import com.intellij.ui.OnePixelSplitter
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.data.GithubBranch
import org.jetbrains.plugins.github.api.data.GithubResponsePage
import org.jetbrains.plugins.github.api.data.GithubUserWithPermissions
import org.jetbrains.plugins.github.util.GHCompatibilityUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import javax.swing.JPanel

@ExtendWith(MockKExtension::class)
class TestRepoTabControllerWorkflowRunsPanel : GitHubActionsManagerBaseTest() {
    @MockK
    lateinit var executorMock: GithubApiRequestExecutor

    init {
        mockkStatic(GHCompatibilityUtil::class)
        every { GHCompatibilityUtil.getOrRequestToken(any(), any()) } returns "token"
    }

    @BeforeEach
    override fun setUp(testInfo: TestInfo) {
        super.setUp(testInfo)
        mockGhActionsService(setOf("http://github.com/owner/repo"), setOf("account1"))
        toolWindowContent = GhActionsMgrToolWindowContent(toolWindow)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)
    }

    @Test
    fun `test repo with different workflow-runs`() {
        val workflowRunsList = listOf(
            createWorkflowRun(id = 1, status = "in_progress"),
            createWorkflowRun(id = 2, status = "completed"),
            createWorkflowRun(id = 2, status = "queued"),
        )
        mockGithubApiRequestExecutor(workflowRunsList)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // act
        toolWindowContent.createContent()
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // assert
        val workflowRunsListPanel = assertTabsAndPanels()
        Assertions.assertEquals(workflowRunsList.size, workflowRunsListPanel.runListComponent.model.size)
    }

    @Test
    fun `test repo without workflow-runs`() {
        mockGithubApiRequestExecutor(emptyList())
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // act
        toolWindowContent.createContent()
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // assert
        val workflowRunsListPanel = assertTabsAndPanels()
        Assertions.assertEquals(0, workflowRunsListPanel.runListComponent.model.size)
    }


    fun mockGithubApiRequestExecutor(
        workflowRunsList: Collection<WorkflowRun>,
        collaborators: Collection<String> = emptyList(),
        branches: Collection<String> = emptyList(),
        workflowTypes: Collection<WorkflowType> = emptyList(),
    ) {
        val collaboratorsResponse = GithubResponsePage(collaborators.map {
            val user = mockk<GithubUserWithPermissions> {
                every { login }.returns(it)
            }
            user
        })
        val branchesResponse = GithubResponsePage(branches.map {
            val branch = mockk<GithubBranch> {
                every { name }.returns(it)
            }
            branch
        })
        val workflowTypesResponse = WorkflowTypes(workflowTypes.size, workflowTypes.toList())
        executorMock.apply {
            every {// workflow runs
                execute(any(), matchApiRequestUrl<WorkflowRuns>("/actions/runs")).hint(WorkflowRuns::class)
            } returns WorkflowRuns(workflowRunsList.size, workflowRunsList.toList())
            every {// collaborators
                execute(any(), matchApiRequestUrl<GithubResponsePage<GithubUserWithPermissions>>("/collaborators"))
            } returns collaboratorsResponse
            every { // branches
                execute(any(), matchApiRequestUrl<GithubResponsePage<GithubBranch>>("/branches"))
            } returns branchesResponse
            every { // workflow types
                execute(any(), matchApiRequestUrl<WorkflowTypes>("/actions/workflows"))
            } returns workflowTypesResponse
        }
        mockkObject(GithubApiRequestExecutor.Factory)
        every { GithubApiRequestExecutor.Factory.getInstance() } returns mockk<GithubApiRequestExecutor.Factory> {
            every { create(token = any()) } returns executorMock
        }
    }

    fun assertTabsAndPanels(): WorkflowRunsListPanel {
        Assertions.assertEquals(1, toolWindow.contentManager.contentCount)
        val content = toolWindow.contentManager.contents[0]
        Assertions.assertEquals("owner/repo", content.displayName)
        Assertions.assertTrue(content.component is JPanel)
        val tabWrapPanel = content.component as JPanel
        Assertions.assertEquals(1, tabWrapPanel.componentCount)
        val workflowDataContextService = projectRule.project.service<WorkflowDataContextService>()
        Assertions.assertEquals(1, workflowDataContextService.repositories.size)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)
        verify(atLeast = 1, timeout = 3000) {
            executorMock.execute(any(), matchApiRequestUrl<WorkflowRuns>("/actions/runs")).hint(WorkflowRuns::class)
            executorMock.execute(
                any(), matchApiRequestUrl<GithubResponsePage<GithubUserWithPermissions>>("/collaborators")
            )
            executorMock.execute(any(), matchApiRequestUrl<GithubResponsePage<GithubBranch>>("/branches"))
            executorMock.execute(any(), matchApiRequestUrl<WorkflowTypes>("/actions/workflows"))
        }
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)
        Assertions.assertEquals(1, (tabWrapPanel.components[0] as JPanel).componentCount)
        Assertions.assertTrue((tabWrapPanel.components[0] as JPanel).components[0] is OnePixelSplitter, "Expected tab to have OnePixelSplitter")
        val splitterComponent = ((tabWrapPanel.components[0] as JPanel).components[0] as OnePixelSplitter)
        Assertions.assertEquals(3, splitterComponent.componentCount)
        Assertions.assertTrue(splitterComponent.firstComponent is WorkflowRunsListPanel)
        Assertions.assertTrue(splitterComponent.secondComponent is OnePixelSplitter)
        return splitterComponent.firstComponent as WorkflowRunsListPanel
    }

    private fun <T> MockKMatcherScope.matchApiRequestUrl(url: String) =
        match<GithubApiRequest<T>> { it.url.contains(url) }

}
