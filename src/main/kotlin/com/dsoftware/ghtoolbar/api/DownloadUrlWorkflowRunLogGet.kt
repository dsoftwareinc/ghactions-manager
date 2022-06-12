package com.dsoftware.ghtoolbar.api

import com.intellij.openapi.diagnostic.logger
import org.apache.commons.io.IOUtils
import org.jetbrains.plugins.github.api.GithubApiRequest
import org.jetbrains.plugins.github.api.GithubApiResponse
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.ZipInputStream

fun extractFromStream(inputStream: InputStream): Map<String, Map<String, String>> {
    val jobNames = HashMap<String, String>()
    val content = HashMap<String, TreeMap<String, String>>()
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

    return content
}

fun stepsAsLog(steps: Map<String, String>): String {
    return steps.entries
        .map { "==== Step: ${it.key} ====\n${it.value}" }
        .joinToString("\n")
}

class DownloadUrlWorkflowRunLogGet(url: String) : GithubApiRequest.Get<String>(url) {
    private lateinit var workflowInfo: Map<String, Map<String, String>>

    init {
        LOG.info("DownloadUrlWorkflowRunLogGet ${url}")
    }

    override fun extractResult(response: GithubApiResponse): String {
        LOG.info("extracting result for $url")
        return response.handleBody {
            workflowInfo = extractFromStream(it)
            LOG.info("Got ${workflowInfo.size} jobs")
            if (workflowInfo.isEmpty()) {
                "Logs are unavailable"
            } else {
                workflowInfo.entries
                    .map { i -> "==== Job: ${i.key} ====\n${stepsAsLog(i.value)}" }
                    .joinToString("\n")
            }

        }
    }

    companion object {
        private val LOG = logger<DownloadUrlWorkflowRunLogGet>()
    }
}