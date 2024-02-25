package com.dsoftware.ghmanager.ui

import com.dsoftware.ghmanager.api.model.Conclusion
import com.dsoftware.ghmanager.api.model.Status
import com.dsoftware.ghmanager.data.ListSelectionHolder
import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBList
import com.intellij.util.text.DateFormatUtil
import kotlinx.datetime.Instant
import javax.swing.Icon
import javax.swing.event.ListSelectionEvent

object ToolbarUtil {
    fun makeTimePretty(date: Instant?): String {
        if (date == null) {
            return "Unknown"
        }
        return DateFormatUtil.formatPrettyDateTime(date.toEpochMilliseconds())
    }

    fun statusIcon(status: String, conclusion: String?): Icon {
        return when (status) {
            Status.COMPLETED.value -> {
                when (conclusion) {
                    Conclusion.SUCCESS.value -> Icons.Checkmark
                    Conclusion.FAILURE.value -> Icons.X
                    "startup_failure" -> Icons.X
                    Conclusion.ACTION_REQUIRED.value -> AllIcons.General.Warning
                    Conclusion.CANCELLED.value -> AllIcons.Actions.Cancel
                    Conclusion.SKIPPED.value -> Icons.Skipped
                    null -> Icons.Checkmark
                    else -> Icons.PrimitiveDot
                }
            }

            Status.QUEUED.value -> Icons.Watch
            Status.IN_PROGRESS.value -> AnimatedIcon.Default.INSTANCE
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