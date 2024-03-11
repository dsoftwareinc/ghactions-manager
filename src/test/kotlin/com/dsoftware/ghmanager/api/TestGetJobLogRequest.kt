package com.dsoftware.ghmanager.api

import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import org.jetbrains.plugins.github.api.GithubApiContentHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource


class TestGetJobLogRequest {

    @ParameterizedTest
    @MethodSource("data")
    fun `test GetJobLogRequest should split log lines to steps`(
        logContentFilename: String,
        jobsJsonFilename: String,
        expectedLogLinesCount: Map<Int, Int>
    ) {
        // arrange
        val logContent = TestGetJobLogRequest::class.java.getResource(logContentFilename)!!.readText()
        val wfJobsJson = TestGetJobLogRequest::class.java.getResource(jobsJsonFilename)!!.readText()
        val wfJobs: WorkflowRunJobs = GithubApiContentHelper.fromJson(wfJobsJson)
        val job = wfJobs.jobs.first()

        //act
        val jobLog = GetJobLogRequest(job).extractLogByStep(logContent.byteInputStream())

        //assert
        val jobLogLinesCount = jobLog.map { it.key to it.value.split("\n").size }.toMap()
        assertEquals(
            logContent.split("\n").size,
            expectedLogLinesCount.values.sum() - expectedLogLinesCount.keys.count()
        )
        assertEquals(expectedLogLinesCount, jobLogLinesCount)
    }

    @ParameterizedTest
    @MethodSource("bigData")
    fun `test GetJobLogRequest should split log lines to steps with links since larger than 1mb`(
        logContentFilename: String,
        jobsJsonFilename: String,
        expectedLogTotalLinesCount: Int,
        expectedLogLinesCount: Map<Int, Int>,
        stepsWithLinks: List<Int>
    ) {
        // arrange
        val logContent = TestGetJobLogRequest::class.java.getResource(logContentFilename)!!.readText()
        val wfJobsJson = TestGetJobLogRequest::class.java.getResource(jobsJsonFilename)!!.readText()
        val wfJobs: WorkflowRunJobs = GithubApiContentHelper.fromJson(wfJobsJson)
        val job = wfJobs.jobs.first()
        val getJobLogRequest = GetJobLogRequest(job)
        //act

        val stepLogMap = getJobLogRequest.extractLogByStep(logContent.byteInputStream())
        val jobLog = getJobLogRequest.stepsAsLog(stepLogMap)

        //assert
        val jobLogLinesCount = stepLogMap.map { it.key to it.value.split("\n").size }.toMap()
        assertEquals(
            logContent.split("\n").size,
            expectedLogLinesCount.values.sum() - expectedLogLinesCount.keys.count()
        )
        assertEquals(expectedLogLinesCount, jobLogLinesCount)
        stepsWithLinks.forEach { step ->
            assertTrue(
                jobLog.contains(job.htmlUrl + "?#step:$step"),
                "Expected to contain link to step $step: ${job.htmlUrl}?#step:$step"
            )
        }
    }


    companion object {

        @JvmStatic
        fun data() = listOf(
            arrayOf(
                "/wf-run-7863783013-single-job-21454796844.log",
                "/wf-run-7863783013-jobs.json",
                mapOf((1 to 34), (2 to 21), (3 to 836), (4 to 810), (6 to 5), (7 to 2), (8 to 14))
            ),
            arrayOf(
                "/wf-run-7946420025-single-job-21694126031.log",
                "/wf-run-7946420025-jobs.json",
                mapOf((1 to 33), (2 to 14), (3 to 15), (4 to 743), (7 to 15))
            ),
        )

        @JvmStatic
        fun bigData() = listOf(
            arrayOf(
                "/wf-run-8175351748-single-job-22352264016.log",
                "/wf-run-8175351748-jobs.json",
                20384,
                mapOf(
                    (1 to 30),
                    (2 to 81),
                    (3 to 14),
                    (4 to 33),
                    (5 to 24),
                    (6 to 1238),
                    (7 to 25),
                    (8 to 25),
                    (9 to 27),
                    (10 to 26),
                    (11 to 188),
                    (12 to 23),
                    (13 to 18647),
                    (24 to 3),
                    (25 to 2),
                    (26 to 14),
                ),
                listOf(13)
            )
        )

    }
}
