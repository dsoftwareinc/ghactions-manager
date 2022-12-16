package com.dsoftware.ghmanager.api

import com.dsoftware.ghmanager.api.model.JobsList
import com.dsoftware.ghmanager.api.model.WorkflowRuns
import com.dsoftware.ghmanager.data.RepositoryCoordinates
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequest.Get.Companion.json
import org.jetbrains.plugins.github.api.GithubApiRequests
import org.jetbrains.plugins.github.api.data.request.GithubRequestPagination
import org.jetbrains.plugins.github.api.util.GithubApiSearchQueryBuilder
import org.jetbrains.plugins.github.api.util.GithubApiUrlQueryBuilder

data class WorkflowRunFilter(
    val branch: String? = null,
    val status: String? = null,
    val actor: String? = null,
    val event: String? = null,
)

object Workflows : GithubApiRequests.Entity("/repos") {
    private val LOG = logger<Workflows>()

    fun getDownloadUrlForWorkflowLog(url: String) = GetRunLogRequest(url)
        .withOperationName("Download Workflow log")

    fun postRerunWorkflow(url: String) = GithubApiRequest.Post.Json(url, Object(), Object::class.java, null)
        .withOperationName("Rerun workflow")

    fun getBranches(coordinates: RepositoryCoordinates) =
        GithubApiRequests.Repos.Branches.get(
            coordinates.serverPath,
            coordinates.repositoryPath.owner,
            coordinates.repositoryPath.repository,
        )

    fun getWorkflowRuns(
        coordinates: RepositoryCoordinates,
        filter: WorkflowRunFilter,
        pagination: GithubRequestPagination? = null
    ): GithubApiRequest<WorkflowRuns> {
        val url = GithubApiRequests.getUrl(coordinates.serverPath,
            urlSuffix,
            "/${coordinates.repositoryPath}",
            "/actions",
            "/runs",
            GithubApiUrlQueryBuilder.urlQuery {
                param("q", GithubApiSearchQueryBuilder.searchQuery {
                    qualifier("event", filter.event)
                    qualifier("status", filter.status)
                    qualifier("branch", filter.branch)
                    qualifier("actor", filter.actor)
                })
                param(pagination)
            })
        LOG.info("Executing url $url")
        return get(url)
    }

    fun getWorkflowRunJobs(url: String) = json<JobsList>(url)
        .withOperationName("Get workflow-run jobs")

    fun get(url: String) = json<WorkflowRuns>(url)
        .withOperationName("search workflow runs")


}
