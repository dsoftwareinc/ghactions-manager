package com.dsoftware.ghtoolbar.ui

import com.intellij.openapi.util.Key

interface WorkflowToolWindowTabController {

    companion object {
        val KEY = Key.create<WorkflowToolWindowTabController>("Github.PullRequests.ToolWindow.Tab.Controller")
    }
}