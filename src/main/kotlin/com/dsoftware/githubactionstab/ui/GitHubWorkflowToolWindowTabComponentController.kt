package com.dsoftware.githubactionstab.ui

import com.intellij.openapi.util.Key

interface GitHubWorkflowToolWindowTabComponentController {

    fun viewList(requestFocus: Boolean = true)

    fun refreshList()

    companion object {
        val KEY =
            Key.create<GitHubWorkflowToolWindowTabComponentController>("Github.PullRequests.Toolwindow.Controller")
    }
}