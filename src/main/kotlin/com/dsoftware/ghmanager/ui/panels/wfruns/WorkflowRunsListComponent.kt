package com.dsoftware.ghmanager.ui.panels.wfruns

import com.dsoftware.ghmanager.actions.ActionKeys
import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.ClientProperty
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.ListUtil
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListUiUtil
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.ListModel
import javax.swing.ListSelectionModel

class WorkflowRunsListComponent(model: ListModel<WorkflowRun>) : JBList<WorkflowRun>(model), DataProvider,
    CopyProvider {

    init {
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val renderer = WorkflowRunsListCellRenderer()
        cellRenderer = renderer
        putClientProperty(UIUtil.NOT_IN_HIERARCHY_COMPONENTS, listOf(renderer))

        ScrollingUtil.installActions(this)
        ClientProperty.put(this, AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun getToolTipText(event: MouseEvent): String? {
        val childComponent = ListUtil.getDeepestRendererChildComponentAt(this, event.point)
        if (childComponent !is JComponent) return null
        return childComponent.toolTipText
    }

    override fun getData(dataId: String): Any? = when {
        PlatformDataKeys.COPY_PROVIDER.`is`(dataId) -> this
        ActionKeys.SELECTED_WORKFLOW_RUN.`is`(dataId) -> selectedValue
        ActionKeys.SELECTED_WORKFLOW_RUN_FILEPATH.`is`(dataId) -> selectedValue?.path
        else -> null
    }

    private inner class WorkflowRunsListCellRenderer : ListCellRenderer<WorkflowRun>, JPanel() {

        private val stateIcon = JLabel()
        private val title = JLabel()
        private val info = JLabel()
        private val labels = JPanel().apply { layout = BoxLayout(this, BoxLayout.X_AXIS) }

        init {
            border = JBUI.Borders.empty(5, 8)
            layout = MigLayout(
                LC().gridGap("0", "0").insets("0", "0", "0", "0").fillX()
            )
            val gapAfter = "${JBUI.scale(5)}px"
            add(stateIcon, CC().gapAfter(gapAfter))
            add(title, CC().growX().pushX().minWidth("pref/2px"))
            add(labels, CC().minWidth("pref/2px").alignX("right").wrap())
            add(info, CC().minWidth("pref/2px").skip(1).spanX(3))
        }

        override fun getListCellRendererComponent(
            list: JList<out WorkflowRun>,
            ghWorkflowRun: WorkflowRun,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            UIUtil.setBackgroundRecursively(this, ListUiUtil.WithTallRow.background(list, isSelected, list.hasFocus()))
            val primaryTextColor = ListUiUtil.WithTallRow.foreground(isSelected, list.hasFocus())
            val secondaryTextColor = ListUiUtil.WithTallRow.secondaryForeground(isSelected, list.hasFocus())

            stateIcon.icon = ToolbarUtil.statusIcon(ghWorkflowRun.status, ghWorkflowRun.conclusion)
            title.apply {
                text = ghWorkflowRun.headCommit.message.split("\n")[0]
                foreground = primaryTextColor
            }

            info.apply {
                val updatedAtLabel = ToolbarUtil.makeTimePretty(ghWorkflowRun.updatedAt)
                val action = if (ghWorkflowRun.event == "release") "created by" else "pushed by"

                text =
                    "${ghWorkflowRun.name} #${ghWorkflowRun.runNumber}: $action ${ghWorkflowRun.headCommit.author.name} started $updatedAtLabel"
                foreground = secondaryTextColor
            }

            labels.apply {
                removeAll()
                add(JBLabel(" ${ghWorkflowRun.headBranch} ", UIUtil.ComponentStyle.SMALL).apply {
                    foreground = JBColor(ColorUtil.softer(secondaryTextColor), ColorUtil.softer(secondaryTextColor))
                })
                add(Box.createRigidArea(JBDimension(4, 0)))
            }
            return this
        }
    }

    override fun performCopy(dataContext: DataContext) = TODO("Not yet implemented")
    override fun isCopyEnabled(dataContext: DataContext): Boolean = false
    override fun isCopyVisible(dataContext: DataContext): Boolean = false
}
