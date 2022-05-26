package com.dsoftware.githubactionstab.api

import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiResponse

class DownloadUrlWorkflowRunLogGet(url: String) : GithubApiRequest.Get<String>(url) {
    override fun extractResult(response: GithubApiResponse): String {
        return response.handleBody {
            LogExtractor().extractFromStream(it) ?: "Logs are unavailable"
        }
    }
}