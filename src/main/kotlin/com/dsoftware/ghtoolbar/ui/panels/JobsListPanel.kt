package com.dsoftware.ghtoolbar.ui.panels


import WorkflowRunJob
import WorkflowRunJobs
import com.dsoftware.ghtoolbar.actions.ActionKeys
import com.dsoftware.ghtoolbar.ui.ToolbarUtil
import com.dsoftware.ghtoolbar.workflow.JobListSelectionHolder
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListUiUtil
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.Component
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseEvent
import java.time.Duration
import javax.swing.*
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener
import javax.swing.event.ListSelectionEvent


class JobList(model: ListModel<WorkflowRunJob>) : JBList<WorkflowRunJob>(model), DataProvider,
    CopyProvider {

    init {
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val renderer = JobsListCellRenderer()
        cellRenderer = renderer
        putClientProperty(UIUtil.NOT_IN_HIERARCHY_COMPONENTS, listOf(renderer))

        ScrollingUtil.installActions(this)
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
        : ListCellRenderer<WorkflowRunJob>, JBPanel<JobsListCellRenderer>(
        MigLayout(
            LC().gridGap("0", "0")
                .insets("0", "0", "0", "0")
                .fillX()
        )
    ) {

        private val stateIcon = JLabel()
        private val title = JLabel()
        private val info = JLabel()
        private val labels = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
        }

        init {
            border = JBUI.Borders.empty(5, 8)
            val gapAfter = "${JBUI.scale(5)}px"
            add(stateIcon, CC().gapAfter(gapAfter))
            add(title, CC().growX().pushX().minWidth("pref/2px"))
            add(labels, CC().minWidth("pref/2px").alignX("right").wrap())
            add(info, CC().minWidth("pref/2px").skip(1).spanX(3))
        }

        override fun getListCellRendererComponent(
            list: JList<out WorkflowRunJob>,
            job: WorkflowRunJob,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            UIUtil.setBackgroundRecursively(this, ListUiUtil.WithTallRow.background(list, isSelected, list.hasFocus()))
            val primaryTextColor = ListUiUtil.WithTallRow.foreground(isSelected, list.hasFocus())
            val secondaryTextColor = ListUiUtil.WithTallRow.secondaryForeground(list, isSelected)

            title.apply {
                icon = ToolbarUtil.statusIcon(job.status, job.conclusion)
                text = job.name
                foreground = primaryTextColor
            }

            info.apply {
                val startedAtLabel = ToolbarUtil.makeTimePretty(job.startedAt)
                val took = if (job.conclusion == "cancelled" || job.completedAt == null || job.startedAt == null)
                    ""
                else {
                    val duration = Duration.between(job.startedAt.toInstant(), job.completedAt.toInstant())
                    "took ${duration.toMinutes()}:${duration.toSecondsPart()} "
                }
                text = "Attempt #${job.runAttempt} at $startedAtLabel $took"
                foreground = secondaryTextColor
            }
            return this
        }
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

    companion object {
        private val actionManager = ActionManager.getInstance()

        fun createJobsListComponent(
            jobModel: SingleValueModel<WorkflowRunJobs?>,
            jobSelectionHolder: JobListSelectionHolder,
        ): JComponent {
            val list = CollectionListModel<WorkflowRunJob>()
            if (jobModel.value != null) {
                list.add(jobModel.value!!.jobs)
            }
            jobModel.addListener {
                list.removeAll()
                if (jobModel.value != null) {
                    list.add(jobModel.value!!.jobs)
                }
            }
            val listComponent = JobList(list).apply {
                emptyText.clear()
            }.also {
                it.addFocusListener(object : FocusListener {
                    override fun focusGained(e: FocusEvent?) {
                        if (it.selectedIndex < 0 && !it.isEmpty) it.selectedIndex = 0
                    }

                    override fun focusLost(e: FocusEvent?) {}
                })
                installPopup(it)
                installJobSelectionSaver(it, jobSelectionHolder)
            }

            return ScrollPaneFactory.createScrollPane(
                listComponent,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            ).apply {
                isOpaque = false
                viewport.isOpaque = false
                border = JBUI.Borders.empty()
            }

        }

        private fun installPopup(list: JobList) {
            list.addMouseListener(object : PopupHandler() {
                override fun invokePopup(comp: Component, x: Int, y: Int) {

                    val (place, groupId) = if (ListUtil.isPointOnSelection(list, x, y)) {
                        Pair("JobListPopupSelected", "Github.ToolWindow.JobList.Popup.Selected")
                    } else {
                        Pair("JobListPopup", "Github.ToolWindow.JobList.Popup")
                    }
                    val popupMenu: ActionPopupMenu =
                        actionManager.createActionPopupMenu(
                            place,
                            actionManager.getAction(groupId) as ActionGroup
                        )

                    popupMenu.setTargetComponent(list)
                    popupMenu.component.show(comp, x, y)
                }
            })
        }

        private fun installJobSelectionSaver(
            list: JobList,
            jobSelectionHolder: JobListSelectionHolder,
        ) {
            var savedSelection: WorkflowRunJob? = null

            list.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
                if (!e.valueIsAdjusting) {
                    val selectedIndex = list.selectedIndex
                    if (selectedIndex >= 0 && selectedIndex < list.model.size) {
                        jobSelectionHolder.selection = list.model.getElementAt(selectedIndex)
                        savedSelection = null
                    }
                }
            }

            list.model.addListDataListener(object : ListDataListener {
                override fun intervalAdded(e: ListDataEvent) {
                    if (e.type == ListDataEvent.INTERVAL_ADDED)
                        (e.index0..e.index1).find { list.model.getElementAt(it) == savedSelection }
                            ?.run {
                                if (list.model.size == 0) return
                                ApplicationManager.getApplication().invokeLater { ScrollingUtil.selectItem(list, this) }
                            }
                }

                override fun contentsChanged(e: ListDataEvent) {}
                override fun intervalRemoved(e: ListDataEvent) {
                    if (e.type == ListDataEvent.INTERVAL_REMOVED) savedSelection = jobSelectionHolder.selection
                }
            })
        }
    }
}
