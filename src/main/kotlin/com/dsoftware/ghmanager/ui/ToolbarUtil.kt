package com.dsoftware.ghmanager.ui

import com.dsoftware.ghmanager.data.ListSelectionHolder
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SpinningProgressIcon
import com.intellij.ui.components.JBList
import com.intellij.util.text.DateFormatUtil
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import javax.swing.Icon
import javax.swing.event.ListSelectionEvent

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
            "in_progress" -> AnimatedIcon.Default.INSTANCE
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