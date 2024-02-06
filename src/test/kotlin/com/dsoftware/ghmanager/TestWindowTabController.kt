package com.dsoftware.ghmanager

import com.dsoftware.ghmanager.api.model.WorkflowRuns
import com.dsoftware.ghmanager.api.model.WorkflowType
import com.dsoftware.ghmanager.api.model.WorkflowTypes
import com.dsoftware.ghmanager.data.WorkflowDataContextService
import com.dsoftware.ghmanager.data.WorkflowRunSelectionContext
import com.intellij.openapi.components.service
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import junit.framework.TestCase
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.data.GithubBranch
import org.jetbrains.plugins.github.api.data.GithubResponsePage
import org.jetbrains.plugins.github.api.data.GithubUserWithPermissions
import org.jetbrains.plugins.github.util.GHCompatibilityUtil
import javax.swing.JPanel

class TestWindowTabController : GitHubActionsManagerBaseTest() {
    private lateinit var workflowDataContextService: WorkflowDataContextService
    override fun setUp() {
        super.setUp()
        mockGhActionsService(setOf("http://github.com/owner/repo"), setOf("account1"))
        mockkStatic(GHCompatibilityUtil::class)
        every { GHCompatibilityUtil.getOrRequestToken(any(), any()) } returns "token"
        factory.init(toolWindow)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)
        workflowDataContextService = project.service<WorkflowDataContextService>()
    }

    fun testNoWorkflowRunsInRepo() {

        mockGithubApiRequestExecutor(WorkflowRuns(0, emptyList()))

        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)

        TestCase.assertEquals(1, toolWindow.contentManager.contentCount)
        val content = toolWindow.contentManager.contents[0]
        TestCase.assertEquals("owner/repo", content.displayName)
        TestCase.assertTrue(content.component is JPanel)
        val panel = content.component as JPanel
        TestCase.assertEquals(1, panel.componentCount)
        TestCase.assertEquals(1, workflowDataContextService.repositories.size)
        val workflowRunSelectionContext: WorkflowRunSelectionContext =
            workflowDataContextService.repositories.values.first().lastLoadedValue!!
        TestCase.assertEquals(0, workflowRunSelectionContext.runsListModel.size)
    }

    private fun mockGithubApiRequestExecutor(
        workflowRuns: WorkflowRuns,
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

        mockkObject(GithubApiRequestExecutor.Factory)
        every { GithubApiRequestExecutor.Factory.getInstance() } returns mockk<GithubApiRequestExecutor.Factory> {
            every { create(token = any()) } returns mockk<GithubApiRequestExecutor>(relaxed = true) {
                every { execute(any(), any<GithubApiRequest<WorkflowRuns>>()) } returns workflowRuns
                every {// collaborators
                    execute(
                        any(),
                        any<GithubApiRequest<GithubResponsePage<GithubUserWithPermissions>>>()
                    )
                } returns collaboratorsResponse
                every { // branches
                    execute(
                        any(),
                        any<GithubApiRequest<GithubResponsePage<GithubBranch>>>()
                    )
                } returns branchesResponse
                every { // branches
                    execute(
                        any(),
                        any<GithubApiRequest<WorkflowTypes>>()
                    )
                } returns workflowTypesResponse
            }
        }
    }
}