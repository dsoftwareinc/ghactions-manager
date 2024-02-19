package com.dsoftware.ghmanager

import com.dsoftware.ghmanager.api.GetJobLogRequest
import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import org.jetbrains.plugins.github.api.GithubApiContentHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters


@RunWith(Parameterized::class)
class TestGetJobLogRequest(
    private val logContentFilename: String,
    private val jobsJsonFilename: String,
    private val expectedLogLinesCount: Map<Int, Int>
) {

    @Test
    fun testGetJobLogRequest() {
        // arrange
        val logContent = TestGetJobLogRequest::class.java.getResource(logContentFilename)!!.readText()
        val wfJobsJson = TestGetJobLogRequest::class.java.getResource(jobsJsonFilename)!!.readText()
        val wfJobs: WorkflowRunJobs = GithubApiContentHelper.fromJson(wfJobsJson)
        val job = wfJobs.jobs.first()

        //act
        val jobLog = GetJobLogRequest(job).extractLogByStep(logContent.byteInputStream())

        //assert
        val jobLogLinesCount = jobLog.map { it.key to it.value.split("\n").size }.toMap()
//        assertEquals(
//            logContent.split("\n").size,
//            expectedLogLinesCount.values.sum() - expectedLogLinesCount.keys.count()
//        )
        assertEquals(expectedLogLinesCount, jobLogLinesCount)
    }


    companion object {

        @JvmStatic
        @Parameters
        fun data() = listOf(
//            arrayOf(
//                "/wf-run-single-job-21454796844.log",
//                "/wf-run-jobs-7863783013.json",
//                mapOf((1 to 33), (2 to 21), (3 to 834), (4 to 813), (6 to 5), (8 to 15))
//            ),
            arrayOf(
                "/wf-run-7946420025-single-job-21694126229.log",
                "/wf-run-7946420025-jobs.json",
                mapOf((1 to 23), (2 to 25), (3 to 49), (4 to 699), (7 to 12))
            )
        )

    }
}
