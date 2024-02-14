package com.dsoftware.ghmanager.api

import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.JobStep
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiResponse
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

typealias JobLog = Map<Int, StringBuilder>

class GetJobLogRequest(private val job: Job) : GithubApiRequest.Get<String>(job.url + "/logs") {
    private val stepsPeriodMap = job.steps?.associate { step ->
        step.number to (step.startedAt to step.completedAt)
    } ?: emptyMap()
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
        val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS"
        val formatter = SimpleDateFormat(dateTimePattern)

        val contentBuilders = HashMap<Int, StringBuilder>()

        formatter.timeZone = TimeZone.getTimeZone("UTC")
        var lineNum = 0
        var currStep = 1
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.lines()
            for (line in lines) {
                ++lineNum
                if (line.length < 29) {
                    contentBuilders.getOrDefault(currStep, StringBuilder()).append(line + "\n")
                    continue
                }
                val datetimeStr = line.substring(0, 23)
                try {
                    val time = formatter.parse(datetimeStr)
                    currStep = findStep(currStep, time)
                } catch (e: ParseException) {
                    LOG.warn("Failed to parse date from log line $lineNum: $line, $e")
                }
                contentBuilders.getOrPut(currStep) { StringBuilder(400_000) }.append(line + "\n")
            }
        } catch (e: IOException) {
            LOG.warn(e.message)
            throw e
        }
        return contentBuilders
    }

    private fun findStep(initialStep: Int, time: Date): Int {
        var currStep = initialStep
        while (currStep < lastStepNumber) {
            if (!stepsPeriodMap.containsKey(currStep)) {
                currStep += 1
                continue
            }
            val currStart = stepsPeriodMap[currStep]?.first
            val currEnd = stepsPeriodMap[currStep]?.second
            if (currStart != null && currStart.after(time)) {
                return currStep
            }
            if ((currStart == null || currStart.before(time) || currStart == time)
                && (currEnd == null || currEnd.after(time) || currEnd == time)
            ) {
                return currStep
            }
            currStep += 1
        }
        return currStep
    }

    private fun stepsAsLog(stepLogs: JobLog): String {
        val stepsResult: Map<Int, JobStep> = if (job.steps == null) {
            emptyMap()
        } else {
            job.steps.associateBy { it.number }
        }
        val stepNumbers = stepsResult.keys.sorted()
        if (!stepNumbers.containsAll(stepLogs.keys)) {
            LOG.warn(
                "Some logs do not have a step-result associated " +
                    "[steps in results=$stepNumbers, step with logs=${stepLogs.keys}] "
            )
        }
        val res = StringBuilder(1_000_000)
        for (index in stepNumbers) {
            val stepInfo = stepsResult[index]!!
            val indexStr = "%3d".format(index)
            res.append(
                when (stepInfo.conclusion) {
                    "skipped" -> "\u001B[0m\u001B[37m---- Step ${indexStr}: ${stepInfo.name} (skipped) ----\u001b[0m\n"
                    "failure" -> "\u001B[0m\u001B[31m---- Step ${indexStr}: ${stepInfo.name} (failed) ----\u001b[0m\n"
                    else -> "\u001B[0m\u001B[32m---- Step ${indexStr}: ${stepInfo.name} ----\u001b[0m\n"
                }
            )
            if (stepInfo.conclusion != "skipped" && stepLogs.containsKey(index) && (res.length < 950_000)) {
                if (res.length + (stepLogs[index]?.length ?: 0) < 990_000) {
                    res.append(stepLogs[index])
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