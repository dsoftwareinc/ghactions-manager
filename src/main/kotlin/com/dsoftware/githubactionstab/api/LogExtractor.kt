package com.dsoftware.githubactionstab.api

import com.intellij.openapi.diagnostic.Logger
import org.apache.commons.io.IOUtils
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.ZipInputStream

class LogExtractor {

    fun extractFromStream(inputStream: InputStream): String? {
        val jobNames = TreeMap<String, String>()
        val content = TreeMap<String, TreeMap<String, String>>()
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

                    content.computeIfAbsent(jobName) { TreeMap<String, String>() }["${stepIndex}_${stepName}"] = stepLog
                }
            }
        }
        LOG.debug("Found ${jobNames.size} jobs")

        if (jobNames.isEmpty()) {
            return null
        }

        var result = ""
        jobNames.forEach { (_, jobName) ->
            if (content.containsKey(jobName)) {
                result += "========== $jobName ==========\n"
                val steps = content.getValue(jobName)
                steps.forEach { (stepFullName, stepLog) ->
                    val stepName = stepFullName.split("_")[1]
                    result += "---------- $stepName ----------\n"
                    result += stepLog + "\n"
                }
            }
        }
        return result
    }

    companion object {
        private val LOG = Logger.getInstance("com.dsoftware.githubactionstab.api")
    }
}