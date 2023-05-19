package com.dsoftware.ghmanager.api

import com.dsoftware.ghmanager.api.model.JobsList
import com.dsoftware.ghmanager.api.model.WorkflowRuns
import com.dsoftware.ghmanager.data.RepositoryCoordinates
import com.intellij.openapi.diagnostic.thisLogger
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiRequest.Get.Companion.json
import org.jetbrains.plugins.github.api.GithubApiRequests
import org.jetbrains.plugins.github.api.data.request.GithubRequestPagination
import org.jetbrains.plugins.github.api.util.GithubApiUrlQueryBuilder

data class WorkflowRunFilter(
    val branch: String? = null,
    val status: String? = null,
    val actor: String? = null,
    val event: String? = null,
)

object Workflows : GithubApiRequests.Entity("/repos") {
    private val LOG = thisLogger()
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
        val url = GithubApiRequests.getUrl(
            coordinates.serverPath,
            urlSuffix,
            "/${coordinates.repositoryPath}",
            "/actions",
            "/runs",
            GithubApiUrlQueryBuilder.urlQuery {
                param("event", filter.event)
                param("status", filter.status)
                param("actor", filter.actor)
                param("branch", filter.branch)
                param(pagination)
            })
        return get<WorkflowRuns>(url, "search workflow runs", pagination)
    }

    fun getWorkflowRunJobs(url: String) = get<JobsList>(
        url, "Get workflow-run jobs", pagination = GithubRequestPagination(1))


    private inline fun <reified T> get(
        url: String, opName: String,
        pagination: GithubRequestPagination? = null
    ): GithubApiRequest<T> {
        val urlWithPagination = url + GithubApiUrlQueryBuilder.urlQuery {
            param(pagination)
        }
        LOG.debug("Creating op: $opName, url $urlWithPagination")
        return json<T>(urlWithPagination).withOperationName(opName)
    }
}
