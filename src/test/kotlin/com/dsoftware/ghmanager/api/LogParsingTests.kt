package com.dsoftware.ghmanager.api

import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import com.dsoftware.ghmanager.createJob
import com.dsoftware.ghmanager.withStep
import org.jetbrains.plugins.github.api.GithubApiContentHelper
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions.assertTrue

class LogParsingTests {
    @Test
    fun `test steps' log showing with right color`() {
        // arrange
        val job = createJob()
            .withStep(number = 1, conclusion = "success")
            .withStep(number = 2, conclusion = "skipped")
            .withStep(number = 3, conclusion = "failure")

        //act
        val log = GetJobLogRequest(job).extractJobLogFromStream("".byteInputStream())

        //assert

        assertTrue(log.contains("[32m---- Step   1: step 1 ----"))
        assertTrue(log.contains("[37m---- Step   2: step 2 (skipped) ----"))
        assertTrue(log.contains("[31m---- Step   3: step 3 (failed) ----"))
    }

    fun `test log has bad date-time structure`() {
        // arrange
        val wfJobsJson = TestGetJobLogRequest::class.java.getResource("/wf-run-7863783013-jobs.json")!!.readText()
        val wfJobs: WorkflowRunJobs = GithubApiContentHelper.fromJson(wfJobsJson)
        val job = wfJobs.jobs.first()
        val line = "2024-02-11T18:09:51.DDDDDDDZ LTS ##[group]Run \n"
        //act
        val log = GetJobLogRequest(job).extractJobLogFromStream(line.byteInputStream())

        //assert
        assertTrue(log.contains(line))
    }
}