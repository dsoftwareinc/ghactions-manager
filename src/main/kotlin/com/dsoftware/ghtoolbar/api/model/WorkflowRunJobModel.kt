import com.fasterxml.jackson.annotation.JsonFormat
import java.util.Date

data class WorkflowRunJobs(
    val total_count: Int,
    val jobs: List<WorkflowRunJob>
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
data class WorkflowRunJob(

    /* The id of the job. */
    val id: Long,
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
    /* The time that the job started, in ISO 8601 format. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val startedAt: Date?,
    /* The time that the job finished, in ISO 8601 format. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    val completedAt: Date?,
    /* The name of the job. */
    val name: String,
    /* Steps in this job. */
    val steps: Array<WorkflowRunJobSteps>? = null,
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
)

/**
 *
 * @param status The phase of the lifecycle that the job is currently in.
 * @param conclusion The outcome of the job.
 * @param name The name of the job.
 * @param number
 * @param startedAt The time that the step started, in ISO 8601 format.
 * @param completedAt The time that the job finished, in ISO 8601 format.
 */
data class WorkflowRunJobSteps(
    /* The phase of the lifecycle that the job is currently in. */
    val status: WorkflowRunJobSteps.Status,
    /* The outcome of the job. */
    val conclusion: String?,
    /* The name of the job. */
    val name: String,
    val number: Int,
    /* The time that the step started, in ISO 8601 format. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val startedAt: Date? = null,
    /* The time that the job finished, in ISO 8601 format. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val completedAt: Date? = null
) {
    /**
     * The phase of the lifecycle that the job is currently in.
     * Values: QUEUED,INPROGRESS,COMPLETED
     */
    enum class Status(val value: String) {
        QUEUED("queued"),
        INPROGRESS("in_progress"),
        COMPLETED("completed");
    }
}