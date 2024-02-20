package com.dsoftware.ghmanager.api

import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.JobStep
import com.intellij.openapi.diagnostic.logger
import kotlinx.datetime.Instant
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiResponse
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

typealias JobLog = Map<Int, StringBuilder>

class GetJobLogRequest(private val job: Job) : GithubApiRequest.Get<String>(job.url + "/logs") {
    private val stepsPeriodMap = job.steps.associate { step ->
        step.number to (step.startedAt to step.completedAt)
    }
    private val lastStepNumber: Int = stepsPeriodMap.keys.maxOrNull() ?: 0

    override fun extractResult(response: GithubApiResponse): String {
        LOG.debug("extracting result for $url")
        return response.handleBody {
            extractJobLogFromStream(it)
        }
    }

    fun extractJobLogFromStream(inputStream: InputStream): String {
        val stepLogs = extractLogByStep(inputStream)
        return stepsAsLog(stepLogs)
    }

    fun extractLogByStep(inputStream: InputStream): Map<Int, StringBuilder> {
        val contentBuilders = HashMap<Int, StringBuilder>()
        var lineNum = 0
        var currStep = 1
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.lines()
            for (line in lines) {
                ++lineNum
                if (line.length >= 29
                    && (line.contains("##[group]Run ")
                        || line.contains("Post job cleanup")
                        || line.contains("Cleaning up orphan processes"))
                ) {
                    val datetimeStr = line.substring(0, 28)
                    try {
                        val time = Instant.parse(datetimeStr)
                        val nextStep = findStep(currStep, time)
                        if (nextStep != currStep) {
                            LOG.debug("Line $lineNum: step changed from $currStep to $nextStep")
                        }
                        currStep = nextStep
                    } catch (e: Exception) {
                        LOG.warn("Failed to parse date \"$datetimeStr\" from log line $lineNum: $line, $e")
                    }
                }
                contentBuilders.getOrPut(currStep) { StringBuilder(400_000) }.append(line + "\n")
            }
        } catch (e: IOException) {
            LOG.warn("Could not read log from input stream", e)
        }
        return contentBuilders
    }

    private fun findStep(initialStep: Int, time: Instant): Int {
        var currStep = initialStep
        while (currStep < lastStepNumber) {
            if (!stepsPeriodMap.containsKey(currStep)) {
                currStep += 1
                continue
            }
            val currStart = stepsPeriodMap[currStep]?.first
            val currEnd = stepsPeriodMap[currStep]?.second
            if (currStart != null && currStart > time) {
                return currStep
            }
            if (currEnd == null || currEnd > time || currEnd == time) {
                return currStep
            }
            currStep += 1
        }
        return currStep
    }

    private fun stepsAsLog(stepLogs: JobLog): String {
        val stepsResult: Map<Int, JobStep> = job.steps.associateBy { it.number }
        val stepNumbers = stepsResult.keys.sorted()

        val res = StringBuilder(1_000_000)
        for (stepNumber in stepNumbers) {
            val stepInfo = stepsResult[stepNumber]!!
            val indexStr = "%3d".format(stepNumber)
            res.append(
                when (stepInfo.conclusion) {
                    "skipped" -> "\u001B[0m\u001B[37m---- Step ${indexStr}: ${stepInfo.name} (skipped) ----\u001b[0m\n"
                    "failure" -> "\u001B[0m\u001B[31m---- Step ${indexStr}: ${stepInfo.name} (failed) ----\u001b[0m\n"
                    else -> "\u001B[0m\u001B[32m---- Step ${indexStr}: ${stepInfo.name} ----\u001b[0m\n"
                }
            )
            if (stepInfo.conclusion != "skipped" && stepLogs.containsKey(stepNumber) && (res.length < 950_000)) {
                if (res.length + (stepLogs[stepNumber]?.length ?: 0) < 990_000) {
                    res.append(stepLogs[stepNumber])
                } else {
                    res.append("Log is too big to display, showing only first 1mb")
                }
            }
        }
        return res.toString()
    }

    companion object {
        private val LOG = logger<GetJobLogRequest>()
    }
}