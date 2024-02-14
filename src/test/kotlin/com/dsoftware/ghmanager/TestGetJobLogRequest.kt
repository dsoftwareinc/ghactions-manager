package com.dsoftware.ghmanager

import com.dsoftware.ghmanager.api.model.Job
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TestGetJobLogRequest : BasePlatformTestCase() {
    private val mapper = ObjectMapper().registerKotlinModule()

    fun testGetJobLogRequest() {
        val logContent = TestGetJobLogRequest::class.java.getResource("wf-run-single-job.log")?.readText()
        val jobJson = TestGetJobLogRequest::class.java.getResource("wf-run-single-job.json")?.readText()
        val obj: List<Job> = mapper.readValue(json)
    }
}