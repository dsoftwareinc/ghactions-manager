package com.dsoftware.ghmanager.api.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import kotlinx.datetime.Instant

data class WorkflowRunJobs(
    val totalCount: Int,
    val jobs: List<Job>
)

data class Job(
    val id: Long,
    val workflowName: String?,
    val headBranch: String?,
    val runId: Long,
    val runUrl: String,
    val runAttempt: Int = 1,
    val nodeId: String,
    val headSha: String,
    val url: String,
    val htmlUrl: String,
    val status: String,
    val conclusion: String?,//  "success", "failure", "neutral", "cancelled", "skipped", "timed_out", "action_required", null
    @JsonDeserialize(using = InstantDeserializer::class)
    val createdAt: Instant?,
    @JsonDeserialize(using = InstantDeserializer::class)
    val startedAt: Instant?,
    @JsonDeserialize(using = InstantDeserializer::class)
    val completedAt: Instant?,
    val name: String,
    val steps: List<JobStep> = emptyList(),
    val checkRunUrl: String,
    val labels: Array<String>,
    val runnerId: Long,
    val runnerName: String?,
    val runnerGroupId: Long?,
    val runnerGroupName: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Job

        if (id != other.id) return false
        if (runId != other.runId) return false
        if (runAttempt != other.runAttempt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + runId.hashCode()
        result = 31 * result + runAttempt
        return result
    }
}

data class JobStep(
    val status: String,
    val conclusion: String?,
    val name: String,
    val number: Int,
    @JsonDeserialize(using = InstantDeserializer::class)
    val startedAt: Instant? = null,
    @JsonDeserialize(using = InstantDeserializer::class)
    val completedAt: Instant? = null
)