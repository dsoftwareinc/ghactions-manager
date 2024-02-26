package com.dsoftware.ghmanager

import com.dsoftware.ghmanager.api.model.GitHubAuthor
import com.dsoftware.ghmanager.api.model.GitHubHeadCommit
import com.dsoftware.ghmanager.api.model.GitHubRepository
import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.JobStep
import com.dsoftware.ghmanager.api.model.WorkflowRun
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun Job.withStep(
    number: Int, conclusion: String, startedAt: Instant = Clock.System.now(), completedAt: Instant = Clock.System.now()
): Job {
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
    id: Long = 21454796844, runId: Long = 7863783013, owner: String = "owner", repo: String = "repo"
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
        createdAt = Clock.System.now(),
        startedAt = Clock.System.now(),
        completedAt = Clock.System.now(),
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

fun createWorkflowRun(
    id: Long = 21454796844,
    owner: String = "owner",
    repo: String = "repo",
    status: String = "completed",
    conclusion: String = "success",
    branch: String = "master",
    workflowId: Long = 7863783013,
): WorkflowRun {
    return WorkflowRun(
        id = id,
        path = "cunla/fakeredis-py",
        nodeId = "CR_kwDOHLrRQs8AAAAE_s44LA",
        headBranch = branch,
        headSha = "a0576c489ba7cad8cad4ba7e14a7fe30ef9959a1",
        runNumber = 1,
        event = "push",
        status = status,
        conclusion = conclusion,
        url = "https://api.github.com/repos/$owner/$repo/actions/runs/$id",
        htmlUrl = "https://api.github.com/repos/$owner/$repo/actions/runs/$id",
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        jobsUrl = "https://api.github.com/repos/$owner/$repo/actions/runs/$id/jobs",
        logsUrl = "https://api.github.com/repos/$owner/$repo/actions/runs/$id/logs",
        checkSuiteUrl = "https://api.github.com/repos/$owner/$repo/actions/runs/$id/check-suites",
        artifactsUrl = "https://api.github.com/repos/$owner/$repo/actions/runs/$id/artifacts",
        cancelUrl = "https://api.github.com/repos/$owner/$repo/actions/runs/$id/cancel",
        rerunUrl = "https://api.github.com/repos/$owner/$repo/actions/runs/$id/rerun",
        workflowId = workflowId,
        workflowUrl = "https://api.github.com/repos/$owner/$repo/actions/workflows/$workflowId",
        name = "Push on master",
        headCommit = GitHubHeadCommit(
            id = "a0576c489ba7cad8cad4ba7e14a7fe30ef9959a1",
            message = "Commit message",
            author = GitHubAuthor(name = owner, email = "$owner@a.com")
        ),
        repository = GitHubRepository(
            id = 1,
            pullsUrl = "https://api.github.com/repos/$owner/$repo/pulls",
            htmlUrl = ""
        ),
        pullRequests = emptyList()
    )
}