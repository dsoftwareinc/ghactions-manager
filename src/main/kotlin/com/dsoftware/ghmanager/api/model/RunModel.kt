package com.dsoftware.ghmanager.api.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.util.*


data class WorkflowRuns(
    val total_count: Int,
    val workflow_runs: List<WorkflowRun> = emptyList()
)

data class PullRequest(
    val id: Int,
    val number: Int,
    val url: String,
)

data class WorkflowRun(
    val id: Long,
    val path: String?,
    val node_id: String,
    val head_branch: String?,
    val head_sha: String?,
    val run_number: Int,
    val event: String,
    val status: String,
    val conclusion: String?,
    val url: String,
    val html_url: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val created_at: Date?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    val updated_at: Date?,
    val jobs_url: String,
    val logs_url: String,
    val check_suite_url: String,
    val artifacts_url: String,
    val cancel_url: String,
    val rerun_url: String,
    val workflow_url: String,
    val name: String,
    val head_commit: GitHubHeadCommit,
    val repository: GitHubRepository,
    val pull_requests: List<PullRequest>? = emptyList(),
) : Comparable<WorkflowRun> {

    /**
     * Compare workflows by their updated_at, or created_at (the newest first), or by id run_number both dates are null
     * @param other The other workflow to compare to
     */
    override fun compareTo(other: WorkflowRun): Int {
        return other.updated_at?.compareTo(this.updated_at)
            ?: other.created_at?.compareTo(this.created_at)
            ?: other.run_number.compareTo(this.run_number)
    }
}

data class GitHubRepository(
    val id: Int,
    val pulls_url: String,
    val html_url: String,
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