package com.dsoftware.ghmanager

import com.intellij.ui.components.JBPanelWithEmptyText
import junit.framework.TestCase


class ToolWindowFactoryTest : GitHubActionsManagerBaseTest() {
    fun testNoGitHubAccountPanel() {
        factory.init(toolWindow)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)

        TestCase.assertEquals(1, toolWindow.contentManager.contentCount)
        val panel = toolWindow.contentManager.contents[0].component
        TestCase.assertTrue(panel is JBPanelWithEmptyText)
    }
}