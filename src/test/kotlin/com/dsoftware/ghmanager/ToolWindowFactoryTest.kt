package com.dsoftware.ghmanager

import com.intellij.ui.components.JBPanelWithEmptyText
import git4idea.remote.hosting.findKnownRepositories
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking


class ToolWindowFactoryTest : GitHubActionsManagerBaseTest() {
    fun testNoGitHubAccountPanel() {
        factory.init(toolWindow)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)

        TestCase.assertEquals(1, toolWindow.contentManager.contentCount)
        val component = toolWindow.contentManager.contents[0].component
        TestCase.assertTrue(component is JBPanelWithEmptyText)
        val panel = component as JBPanelWithEmptyText
        TestCase.assertEquals("GitHub account not configured and no API Token", panel.emptyText.text)
    }

    fun testGitHubAccountNoReposPanel() {
        runBlocking { accountManager.updateAccount(mainAccount.account, mainAccount.token) }

        factory.init(toolWindow)
        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)

        TestCase.assertEquals(1, toolWindow.contentManager.contentCount)
        val component = toolWindow.contentManager.contents[0].component
        TestCase.assertTrue(component is JBPanelWithEmptyText)
        val panel = component as JBPanelWithEmptyText
        TestCase.assertEquals("No git repositories in project", panel.emptyText.text)
    }

//    fun testGitHubAccountWithReposPanel() {
//        runBlocking { accountManager.updateAccount(mainAccount.account, mainAccount.token) }
//        factory.init(toolWindow)
//        executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)
//
//        TestCase.assertEquals(1, toolWindow.contentManager.contentCount)
//        val component = toolWindow.contentManager.contents[0].component
//        TestCase.assertTrue(component is JBPanelWithEmptyText)
//        val panel = component as JBPanelWithEmptyText
//        TestCase.assertEquals("No git repositories in project", panel.emptyText.text)
//    }
}