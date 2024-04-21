package com.dsoftware.ghmanager.toolwindow

import com.dsoftware.ghmanager.api.GhApiRequestExecutor
import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import com.dsoftware.ghmanager.api.model.WorkflowRuns
import com.dsoftware.ghmanager.api.model.WorkflowType
import com.dsoftware.ghmanager.api.model.WorkflowTypes
import com.dsoftware.ghmanager.createJob
import com.dsoftware.ghmanager.createWorkflowRun
import com.dsoftware.ghmanager.data.WorkflowDataContextService
import com.dsoftware.ghmanager.i18n.MessagesBundle.message
import com.dsoftware.ghmanager.ui.GhActionsMgrToolWindowContent
import com.dsoftware.ghmanager.ui.panels.JobListComponent
import com.dsoftware.ghmanager.ui.panels.JobsListPanel
import com.dsoftware.ghmanager.ui.panels.LogConsolePanel
import com.dsoftware.ghmanager.ui.panels.wfruns.WorkflowRunsListPanel
import com.intellij.openapi.components.service
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBScrollPane
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.data.GithubBranch
import org.jetbrains.plugins.github.api.data.GithubResponsePage
import org.jetbrains.plugins.github.api.data.GithubUserWithPermissions
import org.jetbrains.plugins.github.exceptions.GithubStatusCodeException
import org.jetbrains.plugins.github.ui.HtmlInfoPanel
import org.jetbrains.plugins.github.util.GHCompatibilityUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import javax.swing.JPanel
import javax.swing.JTextPane

@ExtendWith(MockKExtension::class)
class TestRepoTabControllerWorkflowRunsPanel : GhActionsMgrBaseTest() {

    @MockK
    lateinit var executorMock: GhApiRequestExecutor

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
        val (workflowRunsListPanel, selectedRunPanel) = assertTabsAndPanels()
        workflowRunsListPanel.runListComponent.emptyText.apply {
            Assertions.assertEquals(message("panel.workflow-runs.no-runs"), text)
            Assertions.assertEquals(2, wrappedFragmentsIterable.count())
            val fragments = wrappedFragmentsIterable.toList()
            Assertions.assertEquals(message("panel.workflow-runs.no-runs"), fragments[0].toString())
            Assertions.assertEquals(message("panel.workflow-runs.no-runs.refresh"), fragments[1].toString())
        }
        Assertions.assertEquals(workflowRunsList.size, workflowRunsListPanel.runListComponent.model.size)
        // assert workflow run list selection
        workflowRunsListPanel.runListComponent.setSelectedValue(
            workflowRunsListPanel.runListComponent.model.getElementAt(0),
            false
        )
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)
        Assertions.assertEquals(
            workflowRunsListPanel.runListComponent.selectedValue,
            workflowRunsListPanel.runListComponent.model.getElementAt(0)
        )
    }

    @Test
    fun `test selecting workflow-run shows jobs`() {
        val workflowRunsList = listOf(
            createWorkflowRun(id = 1, status = "in_progress"),
            createWorkflowRun(id = 2, status = "completed"),
            createWorkflowRun(id = 2, status = "queued"),
        )
        val jobsList = listOf(
            createJob(id = 1, runId = 2, name = "job1"),
        )
        mockGithubApiRequestExecutor(workflowRunsList = workflowRunsList, jobs = jobsList)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // act
        toolWindowContent.createContent()
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // assert wf-runs loaded
        val (workflowRunsListPanel, selectedRunPanel) = assertTabsAndPanels()
        Assertions.assertEquals(workflowRunsList.size, workflowRunsListPanel.runListComponent.model.size)

        val jobsPanelEmptyText = (selectedRunPanel.firstComponent.components[0] as JPanel).components[0] as JTextPane
        Assertions.assertTrue(jobsPanelEmptyText.text.contains(message("panel.jobs.not-loading")))

        // assert workflow run selected => jobs listed
        workflowRunsListPanel.runListComponent.setSelectedValue(
            workflowRunsListPanel.runListComponent.model.getElementAt(0),
            false
        )
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)
        Assertions.assertEquals(
            workflowRunsListPanel.runListComponent.selectedValue,
            workflowRunsListPanel.runListComponent.model.getElementAt(0)
        )
        Assertions.assertNotEquals(
            jobsPanelEmptyText,
            (selectedRunPanel.firstComponent.components[0] as JPanel).components[0]
        )
        val jobsPanel = (selectedRunPanel.firstComponent.components[0] as JobsListPanel)
        Assertions.assertEquals(2, jobsPanel.componentCount)
        val jobsInfoPanel = ((jobsPanel.components[0] as HtmlInfoPanel).components[0] as JTextPane)
        Assertions.assertTrue(jobsInfoPanel.text.contains("1 completed (1 successful)"))
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)
        Assertions.assertEquals(jobsList.size, jobsPanel.jobsListModel.size)

        val jobsListPanel = (jobsPanel.components[1] as JBScrollPane).viewport.view as JobListComponent
        Assertions.assertEquals(jobsList.size, jobsListPanel.model.size)

        // assert log is not loaded
        val logPanelEmptyText = (selectedRunPanel.secondComponent.components[0] as JPanel).components[0] as JTextPane
        Assertions.assertTrue(logPanelEmptyText.text.contains(message("panel.log.not-loading")))
    }

    @Test
    fun `test selecting job shows log for job`() {
        val workflowRunsList = listOf(
            createWorkflowRun(id = 1, status = "in_progress"),
            createWorkflowRun(id = 2, status = "completed"),
            createWorkflowRun(id = 2, status = "queued"),
        )
        val jobsList = listOf(
            createJob(id = 1, runId = 2, name = "job1"),
        )
        val log = "log for job1"
        mockGithubApiRequestExecutor(workflowRunsList = workflowRunsList, jobs = jobsList, log = log)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // act
        toolWindowContent.createContent()
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // assert wf-runs loaded
        val (workflowRunsListPanel, selectedRunPanel) = assertTabsAndPanels()
        Assertions.assertEquals(workflowRunsList.size, workflowRunsListPanel.runListComponent.model.size)

        val jobsPanelEmptyText = (selectedRunPanel.firstComponent.components[0] as JPanel).components[0] as JTextPane
        Assertions.assertTrue(jobsPanelEmptyText.text.contains(message("panel.jobs.not-loading")))

        // act workflow run selected => jobs listed
        workflowRunsListPanel.runListComponent.setSelectedValue(
            workflowRunsListPanel.runListComponent.model.getElementAt(0),
            false
        )
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // assert workflow run selected => jobs listed
        Assertions.assertEquals(
            workflowRunsListPanel.runListComponent.selectedValue,
            workflowRunsListPanel.runListComponent.model.getElementAt(0)
        )
        Assertions.assertNotEquals(
            jobsPanelEmptyText,
            (selectedRunPanel.firstComponent.components[0] as JPanel).components[0]
        )
        val jobsPanel = (selectedRunPanel.firstComponent.components[0] as JobsListPanel)
        Assertions.assertEquals(2, jobsPanel.componentCount)
        val jobsInfoPanel = ((jobsPanel.components[0] as HtmlInfoPanel).components[0] as JTextPane)
        Assertions.assertTrue(jobsInfoPanel.text.contains("1 completed (1 successful)"))
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)
        Assertions.assertEquals(jobsList.size, jobsPanel.jobsListModel.size)

        val jobsListPanel = (jobsPanel.components[1] as JBScrollPane).viewport.view as JobListComponent
        Assertions.assertEquals(jobsList.size, jobsListPanel.model.size)

        // assert log is not loaded
        val logPanelEmptyText = (selectedRunPanel.secondComponent.components[0] as JPanel).components[0] as JTextPane
        Assertions.assertTrue(logPanelEmptyText.text.contains(message("panel.log.not-loading")))

        // act job selected => log loaded
        jobsListPanel.setSelectedValue(jobsListPanel.model.getElementAt(0), false)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        Assertions.assertNotEquals(
            logPanelEmptyText,
            (selectedRunPanel.secondComponent.components[0] as JPanel).components[0]
        )
        val logPanel =
            ((selectedRunPanel.secondComponent.components[0] as JBPanelWithEmptyText).components[0] as LogConsolePanel)
        Assertions.assertEquals(log, logPanel.editor.document.text)

    }


    @Test
    fun `test repo without workflow-runs`() {
        mockGithubApiRequestExecutor(emptyList())
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // act
        toolWindowContent.createContent()
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // assert
        val (workflowRunsListPanel, selectedRunPanel) = assertTabsAndPanels()

        workflowRunsListPanel.runListComponent.emptyText.apply {
            Assertions.assertEquals(message("panel.workflow-runs.no-runs"), text)
            Assertions.assertEquals(2, wrappedFragmentsIterable.count())
            val fragments = wrappedFragmentsIterable.toList()
            Assertions.assertEquals(message("panel.workflow-runs.no-runs"), fragments[0].toString())
            Assertions.assertEquals(message("panel.workflow-runs.no-runs.refresh"), fragments[1].toString())
        }
        Assertions.assertEquals(0, workflowRunsListPanel.runListComponent.model.size)
    }

    @Test
    fun `test not able to get repo wf-runs - http404`() {
        executorMock.apply {
            every {// workflow runs
                execute(any(), matchApiRequestUrl<WorkflowRuns>("/actions/runs")).hint(WorkflowRuns::class)
            } throws GithubStatusCodeException("Not Found", 404)
        }
        mockkObject(GhApiRequestExecutor)
        every { GhApiRequestExecutor.create(token = any()) } returns executorMock
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // act
        toolWindowContent.createContent()
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(projectRule.project)

        // assert
        val (workflowRunsListPanel, selectedRunPanel) = assertTabsAndPanels()
        workflowRunsListPanel.runListComponent.emptyText.apply {
            Assertions.assertEquals(3, wrappedFragmentsIterable.count())
            val fragments = wrappedFragmentsIterable.toList()
            Assertions.assertEquals(message("panel.workflow-runs.error"), fragments[0].toString())
            Assertions.assertEquals("Not Found.", fragments[1].toString())
            Assertions.assertEquals(message("factory.go.to.github-settings"), fragments[2].toString())
        }
        Assertions.assertEquals(0, workflowRunsListPanel.runListComponent.model.size)
    }


    fun mockGithubApiRequestExecutor(
        workflowRunsList: Collection<WorkflowRun>,
        collaborators: Collection<String> = emptyList(),
        branches: Collection<String> = emptyList(),
        workflowTypes: Collection<WorkflowType> = emptyList(),
        jobs: Collection<Job> = emptyList(),
        log: String = "",
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
            every {
                execute(any(), matchApiRequestUrl<String>("/logs")).hint(String::class)
            } returns log
            every {
                execute(any(), matchApiRequestUrl<WorkflowRunJobs>("/jobs")).hint(WorkflowRunJobs::class)
            } returns WorkflowRunJobs(jobs.size, jobs = jobs.toList())
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
        mockkObject(GhApiRequestExecutor)
        every { GhApiRequestExecutor.create(token = any()) } returns executorMock
    }

    fun assertTabsAndPanels(): Pair<WorkflowRunsListPanel, OnePixelSplitter> {
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
        Assertions.assertEquals(1, (tabWrapPanel.components[0] as JPanel).componentCount)
        Assertions.assertTrue(
            (tabWrapPanel.components[0] as JPanel).components[0] is OnePixelSplitter,
            "Expected tab to have OnePixelSplitter"
        )
        val splitterComponent = ((tabWrapPanel.components[0] as JPanel).components[0] as OnePixelSplitter)
        Assertions.assertEquals(3, splitterComponent.componentCount)
        Assertions.assertTrue(splitterComponent.firstComponent is WorkflowRunsListPanel)
        Assertions.assertTrue(splitterComponent.secondComponent is OnePixelSplitter)
        val workflowRunsListPanel = splitterComponent.firstComponent as WorkflowRunsListPanel
        val jobsListPanel = (splitterComponent.secondComponent as OnePixelSplitter).firstComponent
        val logPanel = (splitterComponent.secondComponent as OnePixelSplitter).secondComponent
        Assertions.assertEquals(1, jobsListPanel.componentCount)
        Assertions.assertEquals(1, (jobsListPanel.components[0] as JPanel).componentCount)
        ((jobsListPanel.components[0] as JPanel).components[0] as JTextPane).apply {
            Assertions.assertTrue(text.contains(message("panel.jobs.not-loading")))
        }

        Assertions.assertEquals(1, logPanel.componentCount)
        Assertions.assertEquals(1, (logPanel.components[0] as JPanel).componentCount)
        ((logPanel.components[0] as JPanel).components[0] as JTextPane).apply {
            Assertions.assertTrue(text.contains(message("panel.log.not-loading")))
        }
        return Pair(workflowRunsListPanel, splitterComponent.secondComponent as OnePixelSplitter)
    }

    private fun <T> MockKMatcherScope.matchApiRequestUrl(url: String) =
        match<GithubApiRequest<T>> {
            it.url.split('?')[0].endsWith(url)
        }

}
