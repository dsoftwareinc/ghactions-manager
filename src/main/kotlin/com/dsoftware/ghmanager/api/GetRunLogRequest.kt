package com.dsoftware.ghmanager.api

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiResponse
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


class GetJobLogRequest(url: String) : GithubApiRequest.Get<JobLog>(url) {
    override fun extractResult(response: GithubApiResponse): JobLog {
        LOG.debug("extracting result for $url")
        return response.handleBody {
            extractJobLogFromStream(it)
        }
    }

    companion object {
        private val LOG = logger<GetJobLogRequest>()
        fun extractJobLogFromStream(inputStream: InputStream): JobLog {
            val content = HashMap<Int, String>()
            var stepNumber = 1
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stepLog = StringBuilder()
                for (line in reader.lines()) {
                    if (line.contains("##[group]Run ")) {
                        content[stepNumber] = stepLog.toString()
                        stepLog.clear()
                        stepNumber++
                    } else {
                        stepLog.append(line + "\n")
                    }
                }
            } catch (e: IOException) {
                LOG.warn(e.message)
                throw e
            }
            return content
        }
    }
}