package com.dsoftware.ghmanager

import com.dsoftware.ghmanager.api.GetJobLogRequest
import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.JobStep
import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jetbrains.rd.util.Date
import junit.framework.TestCase



class TestGetJobLogRequest : TestCase() {
    private val mapper = ObjectMapper().registerKotlinModule()

    override fun setUp() {
        super.setUp()
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    fun testGetJobLogRequest() {
        // arrange
        val logContent = TestGetJobLogRequest::class.java.getResource("/wf-run-single-job.log")!!.readText()
        val wfJobsJson = TestGetJobLogRequest::class.java.getResource("/wf-run-jobs.json")!!.readText()
        val wfJobs: WorkflowRunJobs = mapper.readValue(wfJobsJson)
        val job = wfJobs.jobs.first()

        //act
        val jobLog = GetJobLogRequest(job).extractLogByStep(logContent.byteInputStream())

        //assert
        val jobLogLinesCount = jobLog.map { it.key to it.value.split("\n").size }.toMap()
        assertTrue(jobLogLinesCount == mapOf((1 to 33), (2 to 21), (3 to 834), (4 to 813), (6 to 5), (8 to 15)))
    }

    fun testStepsWithRightColor() {
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

    fun testBadLogStructure() {
        // arrange
        val wfJobsJson = TestGetJobLogRequest::class.java.getResource("/wf-run-jobs.json")!!.readText()
        val wfJobs: WorkflowRunJobs = mapper.readValue(wfJobsJson)
        val job = wfJobs.jobs.first()
        val line="2024-02-11T18:09:51.DDDDDDDZ LTS\n"
        //act
        val log = GetJobLogRequest(job).extractJobLogFromStream(line.byteInputStream())

        //assert
        assertTrue(log.contains(line))

    }

}