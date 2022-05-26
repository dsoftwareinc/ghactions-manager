package com.dsoftware.githubactionstab.api

import com.dsoftware.githubactionstab.workflow.GitHubRepositoryCoordinates
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequest.Get
import org.jetbrains.plugins.github.api.GithubApiRequest.Get.Companion.json
import org.jetbrains.plugins.github.api.GithubApiRequests
import org.jetbrains.plugins.github.api.data.request.GithubRequestPagination
import org.jetbrains.plugins.github.api.util.GithubApiSearchQueryBuilder
import org.jetbrains.plugins.github.api.util.GithubApiUrlQueryBuilder
import java.util.*

data class GitHubWorkflow(
    val id: Long,
    val node_id: String,
    val name: String,
    val path: String,
    val state: String,
    val completed_at: String?,
    val updated_at: String?,
    val url: String,
    val html_url: String,
    val badge_url: String
)

data class GitHubWorkflowRuns(
    val total_count: Int,
    val workflow_runs: List<GitHubWorkflowRun> = emptyList()
)

data class GitHubWorkflowRun(
    val id: Long,
    val node_id: String,
    val head_branch: String,
    val head_sha: String,
    val run_number: Int,
    val event: String,
    val status: String,
    val conclusion: String?,
    val url: String,
    val html_url: String,
    val created_at: Date?,
    val updated_at: Date?,
    val jobs_url: String,
    val logs_url: String,
    val check_suite_url: String,
    val artifacts_url: String,
    val cancel_url: String,
    val rerun_url: String,
    val workflow_url: String,
    var workflowName: String?,
    val head_commit: GitHubHeadCommit
)

data class GitHubHeadCommit(
    val id: String,
    val message: String,
    val author: GitHubAuthor
)

data class GitHubAuthor(
    val name: String,
    val email: String
)

val LOG = logger<Workflows>()

object Workflows : GithubApiRequests.Entity("/repos") {

    fun getWorkflowByUrl(url: String) = Get.Json(url, GitHubWorkflow::class.java, null)
        .withOperationName("Get Workflow Description By URL")

    fun getDownloadUrlForWorkflowLog(url: String) = DownloadUrlWorkflowRunLogGet(url)
        .withOperationName("Download Workflow log")

    fun getWorkflowRuns(
        coordinates: GitHubRepositoryCoordinates,
        event: String? = null,
        status: String? = null,
        branch: String? = null,
        actor: String? = null,
        pagination: GithubRequestPagination? = null
    ): GithubApiRequest<GitHubWorkflowRuns> {
        val url = GithubApiRequests.getUrl(coordinates.serverPath,
            urlSuffix,
            "/${coordinates.repositoryPath}",
            "/actions",
            "/runs",
            GithubApiUrlQueryBuilder.urlQuery {
                param("q", GithubApiSearchQueryBuilder.searchQuery {
                    qualifier("event", event)
                    qualifier("status", status)
                    qualifier("branch", branch)
                    qualifier("actor", actor)
                })
                param(pagination)
            })
        LOG.info("Workflows.getWorkflowRuns() url=${url}")
        return get(url)
    }

    fun get(url: String) = json<GitHubWorkflowRuns>(url)
        .withOperationName("search workflow runs")
}
