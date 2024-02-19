package com.dsoftware.ghmanager.ui.panels


import com.dsoftware.ghmanager.actions.ActionKeys
import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import com.dsoftware.ghmanager.data.WorkflowRunSelectionContext
import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.*
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.ClientProperty
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ListUtil
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
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
import org.jetbrains.plugins.github.ui.HtmlInfoPanel
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.ListModel
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants


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

    private inner class JobsListCellRenderer
        : ListCellRenderer<Job>, JBPanel<JobsListCellRenderer>(
        MigLayout(
            LC().gridGap("0", "0")
                .insets("0", "0", "0", "0")
        )
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
            list: JList<out Job>,
            job: Job,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
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
                    "queued" -> "Job is queued"
                    "in_progress" -> "Job running"
                    else -> {
                        val startedAtLabel = ToolbarUtil.makeTimePretty(job.startedAt)
                        val took =
                            if (job.conclusion == "cancelled" || job.completedAt == null || job.startedAt == null)
                                ""
                            else {
                                val duration = job.completedAt - job.startedAt
                                val minutes = duration.inWholeMinutes
                                val seconds = duration.inWholeSeconds % 60
                                "took ${minutes}:" +
                                    "${seconds.toString().padStart(2, '0')} minutes"
                            }
                        "Attempt #${job.runAttempt} started $startedAtLabel $took"
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

    companion object {
        private val actionManager = ActionManager.getInstance()

        fun createJobsListComponent(
            jobValueModel: SingleValueModel<WorkflowRunJobs?>,
            runSelectionContext: WorkflowRunSelectionContext,
            infoInNewLine: Boolean,
        ): Pair<HtmlInfoPanel, JComponent> {
            val infoPanel = HtmlInfoPanel()
            val list = CollectionListModel<Job>()
            list.removeAll()
            jobValueModel.addAndInvokeListener {
                list.removeAll()
                infoPanel.setInfo("")
                it?.let {
                    list.add(it.jobs)
                    infoPanel.setInfo("${it.totalCount} jobs loaded")
                }
            }
            val listComponent = JobListComponent(list, infoInNewLine).apply {
                emptyText.text = "No jobs in workflow run"
            }.also {
                installPopup(it)
                ToolbarUtil.installSelectionHolder(it, runSelectionContext.jobSelectionHolder)
            }

            return Pair(infoPanel, ScrollPaneFactory.createScrollPane(
                listComponent,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            ).apply {
                isOpaque = false
                viewport.isOpaque = false
                border = JBUI.Borders.empty()
            })
        }

        private fun installPopup(list: JobListComponent) {
            list.addMouseListener(object : PopupHandler() {
                override fun invokePopup(comp: Component, x: Int, y: Int) {
                    val (place, groupId) = if (ListUtil.isPointOnSelection(list, x, y)) {
                        Pair("JobListPopupSelected", "Github.ToolWindow.JobList.Popup.Selected")
                    } else {
                        Pair("JobListPopup", "Github.ToolWindow.JobList.Popup")
                    }
                    val popupMenu: ActionPopupMenu = actionManager.createActionPopupMenu(
                        place, actionManager.getAction(groupId) as ActionGroup,
                    )

                    popupMenu.setTargetComponent(list)
                    popupMenu.component.show(comp, x, y)
                }
            })
        }

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
