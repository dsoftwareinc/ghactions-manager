package com.dsoftware.ghmanager.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.SpinningProgressIcon
import com.intellij.util.text.DateFormatUtil
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import javax.swing.Icon

object ToolbarUtil {
    @JvmField
    val LOG: Logger = Logger.getInstance("ghActionsManager")

    const val SETTINGS_DISPLAY_NAME = "GitHub Workflows Manager"
    fun makeTimePretty(date: Date?): String {
        if (date == null) {
            return "Unknown"
        }
        val localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
        val zonedDateTime = localDateTime.atZone(ZoneOffset.UTC)
        return DateFormatUtil.formatPrettyDateTime(zonedDateTime.toInstant().toEpochMilli())
    }

    fun statusIcon(status: String, conclusion: String?): Icon {
        return when (status) {
            "completed" -> {
                when (conclusion) {
                    "success" -> AllIcons.Actions.Commit
                    "failure" -> Icons.X
                    "cancelled" -> AllIcons.Actions.Cancel
                    "skipped" -> Icons.Skipped
                    else -> Icons.PrimitiveDot
                }
            }

            "queued" -> Icons.Watch
            "in_progress" -> SpinningProgressIcon()
            "neutral" -> Icons.PrimitiveDot
            "success" -> AllIcons.Actions.Commit
            "failure" -> Icons.X
            "cancelled" -> AllIcons.Actions.Cancel
            "action required" -> Icons.Watch
            "timed out" -> Icons.Watch
            "skipped" -> Icons.X
            "stale" -> Icons.Watch
            else -> Icons.PrimitiveDot
        }
    }
}