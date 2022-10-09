package com.dsoftware.ghtoolbar.api.model

import java.util.Date


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
) {
    companion object {
        fun empty() = GitHubWorkflow(0, "", "", "", "", null, null, "", "", "")

    }
}

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
    val name: String,
    val head_commit: GitHubHeadCommit
) {
    override fun equals(other: Any?): Boolean = other != null && (other is GitHubWorkflowRun) && this.id == other.id
}

data class GitHubHeadCommit(
    val id: String,
    val message: String,
    val author: GitHubAuthor
)

data class GitHubAuthor(
    val name: String,
    val email: String
)