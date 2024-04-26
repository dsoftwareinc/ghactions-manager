package com.dsoftware.ghmanager.psi

import com.dsoftware.ghmanager.api.GhApiRequestExecutor
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.waitUntil
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(MockKExtension::class)
//@RunInEdt(writeIntent = true)
class OutdatedVersionAnnotatorTest {
    @MockK
    lateinit var executorMock: GhApiRequestExecutor

    @BeforeEach
    fun setUp() {
        val node = JsonNodeFactory.instance.textNode("v4.0.0")

        executorMock.apply {
            every {
                execute<Any>(any())
            } returns node
        }
    }

    fun createTestFixture(testName: String): CodeInsightTestFixture {
        val fixtureFactory = IdeaTestFixtureFactory.getFixtureFactory()
        val tempDirFixture = fixtureFactory.createTempDirTestFixture()
        val workflowContent =
            OutdatedVersionAnnotatorTest::class.java.getResource("/testData/testAnnotate.yaml")!!.readText()
        val projectFixture = fixtureFactory.createFixtureBuilder(testName, true)
        val codeInsightFixture = fixtureFactory.createCodeInsightFixture(projectFixture.fixture, tempDirFixture)
//        val codeInsightFixture = fixtureFactory.createCodeInsightFixture(projectFixture.fixture)
        codeInsightFixture.setUp()
        val workflowFile = tempDirFixture.createFile(".github/workflows/workflow.yaml", workflowContent)
        codeInsightFixture.testDataPath = "/testData"


        return codeInsightFixture
    }


    @Test
    fun testAnnotator() {
        val fixture = createTestFixture("testAnnotate")
        val psiFile = fixture.configureByFile(".github/workflows/workflow.yaml")
        val gitHubActionDataService = fixture.project.service<GitHubActionDataService>()
        gitHubActionDataService.requestExecutor = executorMock
        gitHubActionDataService.actionsToResolve.add("actions/checkout")
        fixture.checkHighlighting(true, false, false, true)
        runBlocking { waitUntil { gitHubActionDataService.actionsToResolve.isEmpty() } }
        gitHubActionDataService.whenActionsLoaded {
            val results = fixture.doHighlighting()
            println(results)
        }
    }
}
//class FakePsiFile(project: Project, private val filename: String) :
//    MockPsiFile(LightVirtualFile(filename), MockPsiManager(project)) {
//    override fun getName(): String = filename
//
//    override fun getContainingFile(): PsiFile = this
//}