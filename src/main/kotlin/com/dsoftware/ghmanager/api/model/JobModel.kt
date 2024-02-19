package com.dsoftware.ghmanager.api.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import kotlinx.datetime.Instant

data class WorkflowRunJobs(
    val totalCount: Int,
    val jobs: List<Job>
)

/**
 * Information of a job execution in a workflow run
 * @param id The id of the job.
 * @param runId The id of the associated workflow run.
 * @param runUrl
 * @param runAttempt Attempt number of the associated workflow run, 1 for first attempt and higher if the workflow was re-run.
 * @param nodeId
 * @param headSha The SHA of the commit that is being run.
 * @param url
 * @param htmlUrl
 * @param status The phase of the lifecycle that the job is currently in.
 * @param conclusion The outcome of the job.
 * @param startedAt The time that the job started, in ISO 8601 format.
 * @param completedAt The time that the job finished, in ISO 8601 format.
 * @param name The name of the job.
 * @param steps Steps in this job.
 * @param checkRunUrl
 * @param labels Labels for the workflow job. Specified by the \"runs_on\" attribute in the action's workflow file.
 * @param runnerId The ID of the runner to which this job has been assigned. (If a runner hasn't yet been assigned, this will be null.)
 * @param runnerName The name of the runner to which this job has been assigned. (If a runner hasn't yet been assigned, this will be null.)
 * @param runnerGroupId The ID of the runner group to which this job has been assigned. (If a runner hasn't yet been assigned, this will be null.)
 * @param runnerGroupName The name of the runner group to which this job has been assigned. (If a runner hasn't yet been assigned, this will be null.)
 */
data class Job(

    /* The id of the job. */
    val id: Long,
    val workflowName: String?,
    val headBranch: String?,
    /* The id of the associated workflow run. */
    val runId: Long,
    val runUrl: String,
    /* Attempt number of the associated workflow run, 1 for first attempt and higher if the workflow was re-run. */
    val runAttempt: Int? = null,
    val nodeId: String,
    /* The SHA of the commit that is being run. */
    val headSha: String,
    val url: String,
    val htmlUrl: String,
    /* The phase of the lifecycle that the job is currently in. */
    val status: String,
    /* The outcome of the job. */
    val conclusion: String?,
    @JsonDeserialize(using = InstantDeserializer::class)
    val createdAt: Instant?,
    @JsonDeserialize(using = InstantDeserializer::class)
    val startedAt: Instant?,
    @JsonDeserialize(using = InstantDeserializer::class)
    val completedAt: Instant?,
    /* The name of the job. */
    val name: String,
    /* Steps in this job. */
    val steps: List<JobStep>? = emptyList(),
    val checkRunUrl: String,
    /* Labels for the workflow job. Specified by the \"runs_on\" attribute in the action's workflow file. */
    val labels: Array<String>,
    /* The ID of the runner to which this job has been assigned. (If a runner hasn't yet been assigned, this will be null.) */
    val runnerId: Long,
    /* The name of the runner to which this job has been assigned. (If a runner hasn't yet been assigned, this will be null.) */
    val runnerName: String?,
    /* The ID of the runner group to which this job has been assigned. (If a runner hasn't yet been assigned, this will be null.) */
    val runnerGroupId: Long?,
    /* The name of the runner group to which this job has been assigned. (If a runner hasn't yet been assigned, this will be null.) */
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
        result = 31 * result + (runAttempt ?: 0)
        return result
    }
}

/**
 *
 * @param status The phase of the lifecycle that the job is currently in.
 * @param conclusion The outcome of the job.
 * @param name The name of the job.
 * @param number
 * @param startedAt The time that the step started, in ISO 8601 format.
 * @param completedAt The time that the job finished, in ISO 8601 format.
 */
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