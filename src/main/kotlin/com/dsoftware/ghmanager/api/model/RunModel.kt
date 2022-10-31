package com.dsoftware.ghmanager.api.model

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
    val head_commit: GitHubHeadCommit,
    val pull_requests: List<PullRequest>? = emptyList(),
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