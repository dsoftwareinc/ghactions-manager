package com.dsoftware.ghmanager.api

import com.dsoftware.ghmanager.api.model.Job
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.rd.util.first
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiResponse
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone


class GetJobLogRequest(private val job: Job) : GithubApiRequest.Get<JobLog>(job.url + "/logs") {
    override fun extractResult(response: GithubApiResponse): JobLog {
        LOG.debug("extracting result for $url")
        return response.handleBody {
            extractJobLogFromStream(it)
        }
    }

    fun extractJobLogFromStream(inputStream: InputStream): JobLog {
        val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSX"
        val formatter = SimpleDateFormat(dateTimePattern)
        val stepsPeriodMap: Map<Int, Pair<Date?, Date?>> = job.steps?.associate { step ->
            val start = if(step.startedAt != null) formatter.parse(step.startedAt) else null
            val completed = if(step.completedAt != null) formatter.parse(step.completedAt) else null
            step.number to (start to completed)
        } ?: emptyMap()
        val contentBuilders = HashMap<Int, StringBuilder>()

        formatter.timeZone = TimeZone.getTimeZone("UTC")
        fun findStep(datetimeStr: String): Int {
            val time = formatter.parse(datetimeStr)
            var lo = 0
            var hi = job.steps!!.size - 1
            while (lo <= hi) {
                val mid = (lo + hi) / 2
                val midVal = stepsPeriodMap[mid]
                if (midVal?.second != null && midVal.second!!.before(time)) {
                    lo = mid + 1
                } else if (midVal?.first != null && midVal.first!!.after(time)) {
                    hi = mid - 1
                } else {
                    return mid
                }
            }
            return -1
        }
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.lines()

            var lineNum = 0
            var currStep = 0
            for (line in lines) {
                ++lineNum
                if (line.length < 29) {
                    contentBuilders.getOrDefault(currStep, StringBuilder()).append(line + "\n")
                    continue
                }
                val dateStr = line.substring(0, 28)
                currStep = findStep(dateStr)
                contentBuilders.getOrDefault(currStep, StringBuilder()).append(line + "\n")
            }
        } catch (e: IOException) {
            LOG.warn(e.message)
            throw e
        }
        return contentBuilders.map { (k, v) -> k to v.toString() }.toMap()
    }

    companion object {
        private val LOG = logger<GetJobLogRequest>()

    }
}