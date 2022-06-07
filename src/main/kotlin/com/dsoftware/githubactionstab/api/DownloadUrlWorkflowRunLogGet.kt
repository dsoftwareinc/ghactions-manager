package com.dsoftware.githubactionstab.api

import com.intellij.openapi.diagnostic.thisLogger
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiResponse

class DownloadUrlWorkflowRunLogGet(url: String) : GithubApiRequest.Get<String>(url) {
    override fun extractResult(response: GithubApiResponse): String {
        LOG.info("extracting result for $url")
        return response.handleBody {
            LogExtractor().extractFromStream(it) ?: "Logs are unavailable"
        }
    }

    companion object {
        private val LOG = thisLogger()
    }
}