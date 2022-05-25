package com.dsoftware.githubactionstab.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.ListUtil
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListUiUtil
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import com.dsoftware.githubactionstab.api.GitHubWorkflowRun
import com.dsoftware.githubactionstab.workflow.action.GitHubWorkflowRunActionKeys
import java.awt.Component
import java.awt.event.MouseEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import javax.swing.*


class GitHubWorkflowRunList(model: ListModel<GitHubWorkflowRun>) : JBList<GitHubWorkflowRun>(model), DataProvider,
    CopyProvider {

    init {
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val renderer = WorkflowRunsListCellRenderer()
        cellRenderer = renderer
        UIUtil.putClientProperty(this, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, listOf(renderer))

        ScrollingUtil.installActions(this)
    }

    override fun getToolTipText(event: MouseEvent): String? {
        val childComponent = ListUtil.getDeepestRendererChildComponentAt(this, event.point)
        if (childComponent !is JComponent) return null
        return childComponent.toolTipText
    }

    override fun getData(dataId: String): Any? = when {
        PlatformDataKeys.COPY_PROVIDER.`is`(dataId) -> this
        GitHubWorkflowRunActionKeys.SELECTED_WORKFLOW_RUN.`is`(dataId) -> selectedValue
        else -> null
    }

    private inner class WorkflowRunsListCellRenderer : ListCellRenderer<GitHubWorkflowRun>, JPanel() {

        private val stateIcon = JLabel()
        private val title = JLabel()
        private val info = JLabel()
        private val labels = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
        }
        private val assignees = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
        }

        init {
            border = JBUI.Borders.empty(5, 8)

            layout = MigLayout(
                LC().gridGap("0", "0")
                    .insets("0", "0", "0", "0")
                    .fillX()
            )

            val gapAfter = "${JBUI.scale(5)}px"
            add(
                stateIcon, CC()
                    .gapAfter(gapAfter)
            )
            add(
                title, CC()
                    .growX()
                    .pushX()
                    .minWidth("pref/2px")
            )
            add(
                labels, CC()
                    .minWidth("pref/2px")
                    .alignX("right")
                    .wrap()
            )
            add(
                info, CC()
                    .minWidth("pref/2px")
                    .skip(1)
                    .spanX(3)
            )
        }

        override fun getListCellRendererComponent(
            list: JList<out GitHubWorkflowRun>,
            value: GitHubWorkflowRun,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            UIUtil.setBackgroundRecursively(this, ListUiUtil.WithTallRow.background(list, isSelected, list.hasFocus()))
            val primaryTextColor = ListUiUtil.WithTallRow.foreground(isSelected, list.hasFocus())
            val secondaryTextColor = ListUiUtil.WithTallRow.secondaryForeground(list, isSelected)

            stateIcon.apply {
                icon = when (value.status) {
                    "completed" -> {
                        when (value.conclusion) {
                            "success" -> AllIcons.Actions.Commit
                            "failure" -> GitHubIcons.X
                            else -> GitHubIcons.PrimitiveDot
                        }
                    }
                    "queued" -> GitHubIcons.PrimitiveDot
                    "in progress" -> GitHubIcons.PrimitiveDot
                    "neutral" -> GitHubIcons.PrimitiveDot
                    "success" -> AllIcons.Actions.Commit
                    "failure" -> GitHubIcons.X
                    "cancelled" -> GitHubIcons.X
                    "action required" -> GitHubIcons.Watch
                    "timed out" -> GitHubIcons.Watch
                    "skipped" -> GitHubIcons.X
                    "stale" -> GitHubIcons.Watch
                    else -> GitHubIcons.PrimitiveDot
                }
            }
            title.apply {
                text = value.head_commit.message
                foreground = primaryTextColor
            }
            var updatedAtLabel = "Unknown"
            if (value.updated_at != null) {
                updatedAtLabel = makeTimePretty(value.updated_at)
            }
            var action = "pushed by"
            if (value.event == "release") {
                action = "created by"
            }
            info.apply {
                text = "${value.workflowName} #${value.run_number}: " +
                    "$action ${value.head_commit.author.name} " +
                    "on $updatedAtLabel"
                foreground = secondaryTextColor
            }
            labels.apply {
                removeAll()
                add(JBLabel(" ${value.head_branch} ", UIUtil.ComponentStyle.SMALL).apply {
                    foreground = JBColor(ColorUtil.softer(secondaryTextColor), ColorUtil.softer(secondaryTextColor))
                })
                add(Box.createRigidArea(JBDimension(4, 0)))
            }
            return this
        }

        fun makeTimePretty(date: Date): String {
            val localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
            val zonedDateTime = localDateTime.atZone(ZoneOffset.UTC)
            return DateFormatUtil.formatPrettyDateTime(zonedDateTime.toInstant().toEpochMilli())
        }
    }

    companion object {
        private val LOG = Logger.getInstance("com.dsoftware.githubactionstab")
    }

    override fun performCopy(dataContext: DataContext) {
        TODO("Not yet implemented")
    }

    override fun isCopyEnabled(dataContext: DataContext): Boolean {
        return false
    }

    override fun isCopyVisible(dataContext: DataContext): Boolean {
        return false
    }
}