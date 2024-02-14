package com.dsoftware.ghmanager

import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.JobStep
import java.util.Date

fun Job.withStep(number: Int, conclusion: String, startedAt: Date = Date(), completedAt: Date = Date()): Job {
    val jobStep = JobStep(
        number = number,
        name = "step $number",
        status = "completed",
        conclusion = conclusion,
        startedAt = startedAt,
        completedAt = completedAt
    )
    return this.copy(steps = (this.steps ?: emptyList()).plus(jobStep))
}

fun createJob(
    id: Long = 21454796844, runId: Long = 7863783013,
    owner: String = "cunla",
    repo: String = "fakeredis-py"
): Job {
    return Job(
        id = id,
        runId = runId,
        workflowName = "Push on master",
        headBranch = "master",
        runUrl = "https://api.github.com/repos/$owner/$repo/actions/runs/$runId",
        runAttempt = 1,
        nodeId = "CR_kwDOHLrRQs8AAAAE_s44LA",
        headSha = "a0576c489ba7cad8cad4ba7e14a7fe30ef9959a1",
        url = "https://api.github.com/repos/$owner/$repo/actions/jobs/$id",
        htmlUrl = "https://github.com/$owner/$repo/actions/runs/$runId/job/$id",
        status = "completed",
        conclusion = "success",
        createdAt = Date(),
        startedAt = Date(),
        completedAt = Date(),
        name = "Analyze (python)",
        steps = mutableListOf(),
        checkRunUrl = "https://api.github.com/repos/$owner/$repo/check-runs/21454796844",
        labels = arrayOf("ubuntu-latest"),
        runnerId = 9,
        runnerName = "GitHub Actions 9",
        runnerGroupId = 2,
        runnerGroupName = "GitHub Actions",
    )
}