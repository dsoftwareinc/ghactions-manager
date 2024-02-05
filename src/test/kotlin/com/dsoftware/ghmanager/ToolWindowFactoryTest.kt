package com.dsoftware.ghmanager

import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.components.JBPanelWithEmptyText
import junit.framework.TestCase
import javax.swing.JPanel


class ToolWindowFactoryTest : GitHubActionsManagerBaseTest() {
    fun testNoGitHubAccountPanel() {
        mockGhActionsService(emptySet(), emptySet())

        factory.init(toolWindow)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)

        TestCase.assertEquals(1, toolWindow.contentManager.contentCount)
        val component = toolWindow.contentManager.contents[0].component
        TestCase.assertTrue(component is JBPanelWithEmptyText)
        val panel = component as JBPanelWithEmptyText

        TestCase.assertEquals("GitHub account not configured and no API Token", panel.emptyText.text)
        val subComponents = panel.emptyText.wrappedFragmentsIterable.map { it as SimpleColoredComponent }.toList()
        TestCase.assertEquals("GitHub account not configured and no API Token", subComponents[0].getCharSequence(true))
        TestCase.assertEquals("Go to github Settings", subComponents[1].getCharSequence(true))
        TestCase.assertEquals("Go to ghactions-manager Settings", subComponents[2].getCharSequence(true))
    }

    fun testGitHubAccountNoReposPanel() {
        mockGhActionsService(emptySet(), setOf("account1"))

        factory.init(toolWindow)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)

        TestCase.assertEquals(1, toolWindow.contentManager.contentCount)
        val component = toolWindow.contentManager.contents[0].component
        TestCase.assertTrue(component is JBPanelWithEmptyText)
        val panel = component as JBPanelWithEmptyText
        TestCase.assertEquals("No git repositories in project", panel.emptyText.text)
    }

    fun testGitHubAccountWithReposPanel() {
        mockGhActionsService(setOf("http://github.com/owner/repo"), setOf("account1"))

        factory.init(toolWindow)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)

        TestCase.assertEquals(1, toolWindow.contentManager.contentCount)
        val content = toolWindow.contentManager.contents[0]
        TestCase.assertEquals("owner/repo", content.displayName)
        TestCase.assertTrue(content.component is JPanel)
        val panel = content.component as JPanel
        TestCase.assertEquals(1, panel.componentCount)
    }
}