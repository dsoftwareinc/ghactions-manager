//package com.dsoftware.ghmanager.psi
//
//import com.intellij.openapi.components.service
//import com.intellij.psi.PsiManager
//import com.intellij.testFramework.EditorTestUtil
//import com.intellij.testFramework.common.initTestApplication
//import com.intellij.testFramework.junit5.RunInEdt
//import com.intellij.testFramework.rules.ProjectModelExtension
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.extension.RegisterExtension
//
//@RunInEdt(writeIntent = true)
//class HighlightAnnotatorTest {
//    init {
//        initTestApplication()
//    }
//
//    @JvmField
//    @RegisterExtension
//    protected val projectRule: ProjectModelExtension = ProjectModelExtension()
//
//    @BeforeEach
//    fun setUp() {
//
//    }
//
//    @Test
//    fun testAnnotate() {
//        val workflowContent = HighlightAnnotatorTest::class.java.getResource("/testproject/workflow.yaml")!!.readText()
//        val workflowFile =
//            projectRule.baseProjectDir.newVirtualFile(".github/workflows/workflow.yaml", workflowContent.toByteArray())
//        val psiManager = projectRule.project.service<PsiManager>()
//        val answer = """"""
//        val psiFile = psiManager.findFile(workflowFile)
//        EditorTestUtil.testFileSyntaxHighlighting(psiFile!!, true, answer)
//    }
//}
//
////
////class FakePsiFile(project: Project, private val filename: String) :
////    MockPsiFile(LightVirtualFile(filename), MockPsiManager(project)) {
////    override fun getName(): String = filename
////
////    override fun getContainingFile(): PsiFile = this
////}