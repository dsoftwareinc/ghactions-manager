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


class GetRunLogRequest(url: String) : GithubApiRequest.Get<Map<String, String>>(url) {
    private lateinit var workflowInfo: Map<String, Map<String, String>>

    override fun extractResult(response: GithubApiResponse): Map<String, String> {
        LOG.debug("extracting result for $url")
        return response.handleBody {
            workflowInfo = extractFromStream(it)
            LOG.debug("Got ${workflowInfo.size} jobs")
            if (workflowInfo.isEmpty()) {
                emptyMap<String, String>()
            } else {
                workflowInfo.entries.map { (key, value) ->
                    key to stepsAsLog(value)
                }.toMap()

            }
        }

    }

    private fun stepsAsLog(steps: Map<String, String>): String {
        return steps.entries
            .map {
                "\u001b[1;97m---- Step: ${it.key} ----\u001b[0m\n${it.value}"
            }
            .joinToString("\n")
    }

    companion object {
        private val LOG = logger<GetRunLogRequest>()
        fun extractFromStream(inputStream: InputStream): Map<String, Map<String, String>> {
            val jobNames = HashMap<String, String>()
            val content = HashMap<String, TreeMap<String, String>>()
            try {
                ZipInputStream(inputStream).use {
                    while (true) {
                        val entry = it.nextEntry ?: break

                        if (entry.isDirectory) {
                            continue
                        }

                        val name = entry.name

                        if (!name.contains("/")) {
                            var (jobIndex, jobName) = name.split("_")
                            if (jobIndex == "1") {
                                //This is a special case, all concatenated logs in root are marked with 1_
                                //We print the log with (1).txt as the first one only
                                if (!jobName.endsWith(" (1).txt")) {
                                    continue
                                }
                            }
                            jobIndex = jobIndex.padStart(4, '0')
                            jobName = jobName.removeSuffix(" (1).txt").removeSuffix(".txt")
                            jobNames.putIfAbsent(jobIndex, jobName)
                        } else {
                            var (jobName, stepFileName) = name.split("/")
                            stepFileName = stepFileName.removeSuffix(".txt")
                            val stepNameParts = stepFileName.split("_")
                            val stepIndex = stepNameParts[0].padStart(4, '0')
                            val stepName = stepNameParts.drop(1).joinToString("")
                            val stepLog = IOUtils.toString(it, StandardCharsets.UTF_8.toString())
                            content.computeIfAbsent(jobName) { TreeMap() }["${stepIndex}_${stepName}"] = stepLog
                        }
                    }
                }
            } catch (e: EOFException) {
                LOG.warn(e.message)
                return emptyMap()
            }
            return content
        }
    }
}