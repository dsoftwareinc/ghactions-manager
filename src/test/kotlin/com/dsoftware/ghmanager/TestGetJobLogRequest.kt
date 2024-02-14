package com.dsoftware.ghmanager

import com.dsoftware.ghmanager.api.GetJobLogRequest
import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
}