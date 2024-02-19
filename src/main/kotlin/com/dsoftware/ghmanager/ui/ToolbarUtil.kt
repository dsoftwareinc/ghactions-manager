package com.dsoftware.ghmanager.ui

import com.dsoftware.ghmanager.data.ListSelectionHolder
import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBList
import com.intellij.util.text.DateFormatUtil
import kotlinx.datetime.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import javax.swing.Icon
import javax.swing.event.ListSelectionEvent

object ToolbarUtil {

    const val SETTINGS_DISPLAY_NAME = "GitHub Workflows Manager"
    fun makeTimePretty(date: Date?): String {
        if (date == null) {
            return "Unknown"
        }
        val localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
        val zonedDateTime = localDateTime.atZone(ZoneId.systemDefault())
        return DateFormatUtil.formatPrettyDateTime(zonedDateTime.toInstant().toEpochMilli())
    }

    fun makeTimePretty(date: Instant?): String {
        if (date == null) {
            return "Unknown"
        }
        return DateFormatUtil.formatPrettyDateTime(date.toEpochMilliseconds())
    }

    fun statusIcon(status: String, conclusion: String?): Icon {
        return when (status) {
            "completed" -> {
                when (conclusion) {
                    "success" -> Icons.Checkmark
                    "failure" -> Icons.X
                    "startup_failure" -> Icons.X
                    "action_required" -> AllIcons.General.Warning
                    "cancelled" -> AllIcons.Actions.Cancel
                    "skipped" -> Icons.Skipped
                    null -> Icons.Checkmark
                    else -> Icons.PrimitiveDot
                }
            }

            "queued" -> Icons.Watch
            "in_progress" -> AnimatedIcon.Default.INSTANCE
            "neutral" -> Icons.PrimitiveDot
            "success" -> Icons.Checkmark
            "failure" -> Icons.X
            "cancelled" -> AllIcons.Actions.Cancel
            "action required" -> Icons.Watch
            "timed_out" -> AllIcons.General.Warning
            "skipped" -> Icons.Skipped
            "stale" -> AllIcons.General.Warning
            else -> Icons.PrimitiveDot
        }
    }

    fun <T> installSelectionHolder(list: JBList<T>, selectionHolder: ListSelectionHolder<T>) {
        list.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
            if (!e.valueIsAdjusting) {
                val selectedIndex = list.selectedIndex
                if (selectedIndex >= 0 && selectedIndex < list.model.size) {
                    val currSelection = list.model.getElementAt(selectedIndex)
                    if (selectionHolder.selection != currSelection)
                        selectionHolder.selection = currSelection
                }
            }
        }
    }
}