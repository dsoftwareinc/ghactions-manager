package com.dsoftware.ghmanager.psi

import com.intellij.openapi.components.service
import com.intellij.testFramework.common.initTestApplication
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.junit5.RunInEdt
import com.intellij.testFramework.rules.ProjectModelExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.io.path.Path

@RunInEdt(writeIntent = true)
class OutdatedVersionAnnotatorTest {
    init {
        initTestApplication()
    }

    @JvmField
    @RegisterExtension
    protected val projectRule: ProjectModelExtension = ProjectModelExtension()

    @BeforeEach
    fun setUp() {

    }

    fun createTestFixture(testName: String): CodeInsightTestFixture {
        val fixtureFactory = IdeaTestFixtureFactory.getFixtureFactory()
        val projectPath = Path("testData")
        val projectFixture = fixtureFactory.createFixtureBuilder(testName, projectPath, true)
        val codeInsightFixture = fixtureFactory.createCodeInsightFixture(projectFixture.fixture)
        codeInsightFixture.setUp()
        codeInsightFixture.testDataPath = "\$CONTENT_ROOT/testData"
        codeInsightFixture.configureByFile("$testName.yml")

        return codeInsightFixture
    }

    @Test
    fun testAnnotator() {
        val fixture = createTestFixture("testAnnotate")
        fixture.checkHighlighting(true, false, false, true)
        val gitHubActionDataService = fixture.project.service<GitHubActionDataService>()
        gitHubActionDataService.whenActionsLoaded {
            val results = fixture.doHighlighting()
            println(results)
        }
    }
}

//
//class FakePsiFile(project: Project, private val filename: String) :
//    MockPsiFile(LightVirtualFile(filename), MockPsiManager(project)) {
//    override fun getName(): String = filename
//
//    override fun getContainingFile(): PsiFile = this
//}