package com.dsoftware.ghmanager.psi

import com.dsoftware.ghmanager.api.GhApiRequestExecutor
import com.dsoftware.ghmanager.i18n.MessagesBundle.message
import com.dsoftware.ghmanager.toolwindow.executeSomeCoroutineTasksAndDispatchAllInvocationEvents
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.runInEdtAndWait
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


//@RunInEdt(writeIntent = true)
@ExtendWith(MockKExtension::class)
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

        val projectFixture = fixtureFactory.createFixtureBuilder(testName, true)
        val codeInsightFixture = fixtureFactory.createCodeInsightFixture(projectFixture.fixture, tempDirFixture)
//        val codeInsightFixture = fixtureFactory.createCodeInsightFixture(projectFixture.fixture)
        codeInsightFixture.setUp()
        codeInsightFixture.testDataPath = "/testData"
        return codeInsightFixture
    }


    @Test
    fun testAnnotator() {
        var actionLoaded = false
        val fixture = createTestFixture("testAnnotate")
        val psiFile = fixture.configureByText(
            ".github/workflows/workflow1.yaml",
            """           
            jobs:
              build:
                name: Build
                runs-on: ubuntu-latest   
                steps:
                  - name: Fetch Sources
                    uses: actions/checkout@v2
            """.trimIndent()
        )
        val virtualFile = fixture.createFile(
            ".github/workflows/workflow.yaml",
            """
            jobs:
              build:
                name: Build
                runs-on: ubuntu-latest   
                steps:
                  - name: Fetch Sources
                    uses: actions/checkout@<warning descr="v2 is outdated. Latest version is v4.0.0">v2</warning>
            """.trimIndent()
        )
        val gitHubActionDataService = fixture.project.service<GitHubActionDataService>()
        gitHubActionDataService.requestExecutor = executorMock
        gitHubActionDataService.actionsToResolve.add("actions/checkout")
        gitHubActionDataService.whenActionsLoaded { actionLoaded = true }
        runInEdtAndWait {
            while (!actionLoaded) {
                executeSomeCoroutineTasksAndDispatchAllInvocationEvents(fixture.project)
            }
            fixture.testHighlighting(true, true, true, virtualFile)
            executeSomeCoroutineTasksAndDispatchAllInvocationEvents(fixture.project)
            val quickFixes = fixture.getAllQuickFixes(psiFile.name)
            Assertions.assertEquals(1, quickFixes.size)
            Assertions.assertEquals(
                //Update actions/checkout action to version v4
                message("ghmanager.update.action.version.fix.family.name", "actions/checkout", "v4"),
                quickFixes.first().text
            )
        }

    }
}