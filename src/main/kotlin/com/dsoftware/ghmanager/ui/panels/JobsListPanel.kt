package com.dsoftware.ghmanager.ui.panels


import com.dsoftware.ghmanager.api.model.Job
import com.dsoftware.ghmanager.api.model.Status
import com.dsoftware.ghmanager.api.model.WorkflowRunJobs
import com.dsoftware.ghmanager.data.WorkflowRunSelectionContext
import com.dsoftware.ghmanager.i18n.MessagesBundle.message
import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPopupMenu
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ListUtil
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.plugins.github.ui.HtmlInfoPanel
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.ScrollPaneConstants


class JobsListPanel(
    parentDisposable: Disposable,
    jobsValueModel: SingleValueModel<WorkflowRunJobs>,
    private val runSelectionContext: WorkflowRunSelectionContext,
    private val infoInNewLine: Boolean
) : BorderLayoutPanel(), Disposable {
    private val topInfoPanel = HtmlInfoPanel()
    @VisibleForTesting
    internal val jobsListModel = CollectionListModel<Job>()

    init {
        Disposer.register(parentDisposable, this)
        jobsListModel.removeAll()
        val scrollPane = ScrollPaneFactory.createScrollPane(
            createListComponent(),
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        ).apply {
            isOpaque = false
            viewport.isOpaque = false
            border = JBUI.Borders.empty()
        }

        isOpaque = false
        add(topInfoPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        jobsListModel.removeAll()
        jobsValueModel.addAndInvokeListener {
            jobsListModel.removeAll()
            jobsListModel.add(it.jobs)
            topInfoPanel.setInfo(infoTitleString(it.jobs))
        }
    }

    private fun infoTitleString(jobs: List<Job>): String {
        val statusCounter = jobs.groupingBy { job -> job.status }.eachCount()
        val res = mutableListOf<String>()
        if (statusCounter.getOrDefault(Status.COMPLETED.value, 0) != 0) {
            val completedStr = "${statusCounter[Status.COMPLETED.value]} completed"
            val statusStrList = mutableListOf<String>()
            val conclusionCounter = jobs.groupingBy { job -> job.conclusion }.eachCount()
            if (conclusionCounter.containsKey("success")) {
                statusStrList.add("${conclusionCounter["success"]} successful")
            }
            if (conclusionCounter.containsKey("failure")) {
                statusStrList.add("""<span style="color:red">${conclusionCounter["failure"]} failed</span>""")
            }
            res.add(completedStr + " (" + statusStrList.joinToString(", ") + ")")
        }
        if (statusCounter.getOrDefault(Status.IN_PROGRESS.value, 0) != 0) {
            res.add("${statusCounter[Status.IN_PROGRESS.value]} in progress")
        }
        if (statusCounter.getOrDefault(Status.QUEUED.value, 0) != 0) {
            res.add("${statusCounter[Status.QUEUED.value]} queued")
        }

        return res.joinToString(", ")
    }

    private fun createListComponent(): JobListComponent {
        return JobListComponent(jobsListModel, infoInNewLine).apply {
            emptyText.text = message("panel.jobs.info.no-jobs")
        }.also {
            installPopup(it)
            ToolbarUtil.installSelectionHolder(it, runSelectionContext.jobSelectionHolder)
        }
    }

    private fun installPopup(list: JobListComponent) {
        val actionManager = ActionManager.getInstance()
        list.addMouseListener(object : PopupHandler() {
            override fun invokePopup(comp: Component, x: Int, y: Int) {
                val (place, groupId) = if (ListUtil.isPointOnSelection(list, x, y)) {
                    Pair("JobListPopupSelected", "GhActionsMgr.ToolWindow.JobList.Popup.Selected")
                } else {
                    Pair("JobListPopup", "GhActionsMgr.ToolWindow.JobList.Popup")
                }
                val popupMenu: ActionPopupMenu = actionManager
                    .createActionPopupMenu(place, actionManager.getAction(groupId) as ActionGroup)

                popupMenu.setTargetComponent(list)
                popupMenu.component.show(comp, x, y)
            }
        })
    }

    override fun dispose() {}

}

