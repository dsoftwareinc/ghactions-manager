package com.dsoftware.ghmanager.ui.panels

import com.dsoftware.ghmanager.actions.ActionKeys
import com.dsoftware.ghmanager.api.model.Conclusion
import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.Status
import com.dsoftware.ghmanager.i18n.MessagesBundle
import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.ClientProperty
import com.intellij.ui.ListUtil
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.util.applyIf
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListUiUtil
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.ListModel
import javax.swing.ListSelectionModel


class JobListComponent(
    model: ListModel<Job>, private val infoInNewLine: Boolean,
) : JBList<Job>(model), DataProvider, CopyProvider {

    init {
        isEnabled = true
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val renderer = JobsListCellRenderer()
        cellRenderer = renderer
        putClientProperty(UIUtil.NOT_IN_HIERARCHY_COMPONENTS, listOf(renderer))
        ScrollingUtil.installActions(this)
        ClientProperty.put(this, AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)
    }

    override fun getToolTipText(event: MouseEvent): String? {
        val childComponent = ListUtil.getDeepestRendererChildComponentAt(this, event.point)
        if (childComponent !is JComponent) return null
        return childComponent.toolTipText
    }

    override fun getData(dataId: String): Any? = when {
        PlatformDataKeys.COPY_PROVIDER.`is`(dataId) -> this
        ActionKeys.SELECTED_JOB.`is`(dataId) -> selectedValue
        else -> null
    }

    private inner class JobsListCellRenderer : ListCellRenderer<Job>, JBPanel<JobsListCellRenderer>(
        MigLayout(LC().gridGap("0", "0").insets("0", "0", "0", "0"))
    ) {
        private val title = JLabel()
        private val info = JLabel()

        init {
            val infoCC = CC().minWidth("pref/2px").maxWidth("pref/1px").applyIf(infoInNewLine) { newline() }
            border = JBUI.Borders.empty(5, 8)
            add(title, CC().growX().pushX().minWidth("pref/2px").maxWidth("pref/1px"))
            add(info, infoCC)
        }

        override fun getListCellRendererComponent(
            list: JList<out Job>, job: Job, index: Int, isSelected: Boolean, cellHasFocus: Boolean,
        ): Component {
            UIUtil.setBackgroundRecursively(this, ListUiUtil.WithTallRow.background(list, isSelected, list.hasFocus()))
            val primaryTextColor = ListUiUtil.WithTallRow.foreground(isSelected, list.hasFocus())
            val secondaryTextColor = ListUiUtil.WithTallRow.secondaryForeground(isSelected, list.hasFocus())

            title.apply {
                icon = ToolbarUtil.statusIcon(job.status, job.conclusion)
                text = job.name
                foreground = primaryTextColor
            }

            info.apply {
                val info = when (job.status) {
                    Status.QUEUED.value -> MessagesBundle.message("panel.jobs.job-status.queued")
                    Status.IN_PROGRESS.value -> MessagesBundle.message("panel.jobs.job-status.in-progress")
                    Status.WAITING.value -> MessagesBundle.message("panel.jobs.job-status.waiting")
                    else -> { // Status.COMPLETED.value
                        when (job.conclusion) {
                            Conclusion.SKIPPED.value -> MessagesBundle.message("panel.jobs.job-status.completed.skipped")
                            Conclusion.CANCELLED.value -> MessagesBundle.message("panel.jobs.job-status.completed.cancelled")
                            Conclusion.TIMED_OUT.value -> MessagesBundle.message("panel.jobs.job-status.completed.timed-out")
                            Conclusion.FAILURE.value, Conclusion.SUCCESS.value -> {
                                val startedAtLabel = ToolbarUtil.makeTimePretty(job.startedAt)
                                val took =
                                    if (job.conclusion == "cancelled" || job.completedAt == null || job.startedAt == null)
                                        ""
                                    else {
                                        val duration = job.completedAt - job.startedAt
                                        val minutes = duration.inWholeMinutes
                                        val seconds = duration.inWholeSeconds % 60
                                        MessagesBundle.message(
                                            "panel.jobs.job-status.completed.took",
                                            minutes,
                                            seconds.toString().padStart(2, '0')
                                        )
                                    }
                                MessagesBundle.message(
                                    "panel.jobs.job-status.completed.job-done",
                                    job.runAttempt,
                                    startedAtLabel
                                ) + " " + took
                            }

                            else -> ""
                        }
                    }
                }
                text = "<html>$info</html>"
                foreground = secondaryTextColor
            }
            return this
        }
    }

    override fun performCopy(dataContext: DataContext) = TODO("Not yet implemented")

    override fun isCopyEnabled(dataContext: DataContext): Boolean = false

    override fun isCopyVisible(dataContext: DataContext): Boolean = false
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}