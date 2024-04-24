package com.dsoftware.ghmanager.psi

import com.intellij.openapi.components.service
import com.intellij.psi.PsiManager
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.common.initTestApplication
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.junit5.RunInEdt
import com.intellij.testFramework.rules.ProjectModelExtension
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@RunInEdt(writeIntent = true)
class HighlightAnnotatorTest {

    @BeforeEach
    fun setUp() {

    }
    fun createTestFixture(testName:String): CodeInsightTestFixture {
        val fixtureFactory = IdeaTestFixtureFactory.getFixtureFactory()
        val projectFixture = fixtureFactory.createFixtureBuilder(testName)
        val codeInsightFixture = fixtureFactory.createCodeInsightFixture(projectFixture.fixture)
        codeInsightFixture.setUp()
        codeInsightFixture.testDataPath = "\$CONTENT_ROOT/src/test/resources/testproject"

        return codeInsightFixture
    }
    @Test
    fun testAnnotate() {
        IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
            IdeaTestFixtureFactory.getFixtureFactory().
        )
        val workflowContent = HighlightAnnotatorTest::class.java.getResource("/testproject/workflow.yaml")!!.readText()
        val workflowFile =
            projectRule.baseProjectDir.newVirtualFile(".github/workflows/workflow.yaml", workflowContent.toByteArray())
        val psiManager = projectRule.project.service<PsiManager>()
        val answer = """"""
        val psiFile = psiManager.findFile(workflowFile)

    }
}

//
//class FakePsiFile(project: Project, private val filename: String) :
//    MockPsiFile(LightVirtualFile(filename), MockPsiManager(project)) {
//    override fun getName(): String = filename
//
//    override fun getContainingFile(): PsiFile = this
//}