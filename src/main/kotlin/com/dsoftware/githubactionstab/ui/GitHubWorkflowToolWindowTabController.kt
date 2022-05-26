package com.dsoftware.githubactionstab.ui

import com.intellij.openapi.util.Key

interface GitHubWorkflowToolWindowTabController {

    companion object {
        val KEY = Key.create<GitHubWorkflowToolWindowTabController>("Github.PullRequests.ToolWindow.Tab.Controller")
    }
}