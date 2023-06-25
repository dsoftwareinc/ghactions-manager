package com.dsoftware.ghmanager.api

import com.intellij.openapi.diagnostic.logger
import org.apache.commons.io.IOUtils
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiResponse
import java.io.EOFException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.ZipInputStream

class GetRunLogRequest(url: String) : GithubApiRequest.Get<GitHubLog>(url) {
    private lateinit var workflowInfo: GitHubLog

    override fun extractResult(response: GithubApiResponse): GitHubLog {
        LOG.debug("extracting result for $url")
        return response.handleBody {
            workflowInfo = extractFromStream(it)
            workflowInfo
        }
    }

    companion object {
        private val LOG = logger<GetRunLogRequest>()
        fun extractFromStream(inputStream: InputStream): GitHubLog {
            val content = HashMap<String, TreeMap<Int, String>>()
            try {
                ZipInputStream(inputStream).use {
                    while (true) {
                        val entry = it.nextEntry ?: break
                        if (entry.isDirectory) {
                            continue
                        }
                        val name = entry.name

                        if (!name.contains("/")) {
                            val (jobIndex, jobName) = name.split("_")
                            if (jobIndex == "1") {
                                //This is a special case, all concatenated logs in root are marked with 1_
                                //We print the log with (1).txt as the first one only
                                if (!jobName.endsWith(" (1).txt")) {
                                    continue
                                }
                            }
                        } else {
                            var (jobName, stepFileName) = name.split("/")
                            stepFileName = stepFileName.removeSuffix(".txt")
                            val stepNameParts = stepFileName.split("_")
                            val stepIndex = stepNameParts[0].toInt()
                            val stepLog = IOUtils.toString(it, StandardCharsets.UTF_8.toString())
                            content.computeIfAbsent(jobName) { TreeMap() }[stepIndex] = stepLog
                        }
                    }
                }
            } catch (e: EOFException) {
                LOG.warn(e.message)
                throw e
            }
            return content
        }
    }
}