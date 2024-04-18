package com.dsoftware.ghmanager

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.PluginManagerCore.getPlugin
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import java.awt.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal class PluginErrorReportSubmitter : ErrorReportSubmitter() {
    private val REPORT_URL =
        "https://github.com/cunla/ghactions-manager/issues/new?assignees=&labels=&projects=&template=bug_report.md"

    override fun getReportActionText(): String {
        return "Report Issue on Plugin Issues Tracker"
    }

    override fun submit(
        events: Array<IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo?>
    ): Boolean {
        val event = events[0]
        val throwableTitle = event.throwableText.lines()[0]

        val sb = StringBuilder(REPORT_URL)

        val titleEncoded = URLEncoder.encode(event.throwable?.message ?: throwableTitle, StandardCharsets.UTF_8)
        sb.append("&title=${titleEncoded}")

        val pluginVersion = getPlugin(pluginDescriptor.pluginId)?.version ?: "unknown"

        val body = """                    
        ### Describe the bug 
        A clear and concise description of what the bug is. 
        Add a screenshot if it is relevant.
        
        **Describe the bug:**
        ${additionalInfo ?: ""}
        ${event.message ?: ""}
        
        #### Stack trace
        
        {{{PLACEHOLDER}}}
        
        ### Steps to reproduce
        
        <!-- Steps to reproduce the issue. -->
        
        ### Expected behavior
        
        <!-- A clear and concise description of what you expected to happen. -->
        
        ### Additional context
        
        Plugin version: $pluginVersion
        IDE: ${ApplicationInfo.getInstance().fullApplicationName} (${ApplicationInfo.getInstance().build.asString()})
        OS: ${SystemInfo.getOsNameAndVersion()}

        """.trimIndent().replace("{{{PLACEHOLDER}}}", event.throwableText)
        sb.append("&body=${URLEncoder.encode(body, StandardCharsets.UTF_8)}")
        BrowserUtil.browse(sb.toString())

        consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
        return true
    }

}
