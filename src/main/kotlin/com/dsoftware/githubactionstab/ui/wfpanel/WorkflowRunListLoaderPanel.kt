package com.dsoftware.githubactionstab.ui.wfpanel

import com.dsoftware.githubactionstab.api.GitHubWorkflowRun
import com.dsoftware.githubactionstab.ui.ListLoaderPanel
import com.dsoftware.githubactionstab.workflow.LoadingErrorHandler
import com.dsoftware.githubactionstab.workflow.WorkflowRunListSelectionHolder
import com.dsoftware.githubactionstab.workflow.data.WorkflowRunDataContext
import com.dsoftware.githubactionstab.workflow.data.WorkflowRunListLoader
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ListUtil
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.StatusText
import com.intellij.vcs.log.ui.frame.ProgressStripe
import java.awt.BorderLayout
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener
import javax.swing.event.ListSelectionEvent

internal class WorkflowRunListLoaderPanel(
    runListLoader: WorkflowRunListLoader,
    private val listReloadAction: RefreshAction,
    contentComponent: JComponent
) : ListLoaderPanel(runListLoader, contentComponent), Disposable {

    private lateinit var progressStripe: ProgressStripe

    override fun createCenterPanel(content: JComponent): JPanel {
        LOG.debug("Create center panel")
        val stripe = ProgressStripe(
            content, this,
            ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS
        )
        progressStripe = stripe
        return stripe
    }

    override fun setLoading(isLoading: Boolean) {
        if (isLoading) progressStripe.startLoading() else progressStripe.stopLoading()
    }

    init {
//        runListLoader.addOutdatedStateChangeListener(this) {
//            LOG.debug("Update info panel")
//            updateInfoPanel()
//        }
    }

    override fun displayEmptyStatus(emptyText: StatusText) {
        LOG.debug("Display empty status")
        emptyText.text = "Nothing loaded. "
        emptyText.appendSecondaryText("Refresh", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
            listLoader.reset()
        }
    }

    override fun updateInfoPanel() {
        super.updateInfoPanel()
//        if (infoPanel.isEmpty && listLoader.outdated) {
//            infoPanel.setInfo("<html><body>The list is outdated. <a href=''>Refresh</a></body></html>",
//                HtmlInfoPanel.Severity.INFO) {
//                ActionUtil.invokeAction(listReloadAction, this, ActionPlaces.UNKNOWN, it.inputEvent, null)
//            }
//        }
    }

    companion object {
        private val LOG = logger<WorkflowRunListLoaderPanel>()
        private val actionManager = ActionManager.getInstance()
        private fun installPopup(list: WorkflowRunList) {
            val popupHandler = object : PopupHandler() {
                override fun invokePopup(comp: java.awt.Component, x: Int, y: Int) {

                    val popupMenu: ActionPopupMenu = if (ListUtil.isPointOnSelection(list, x, y)) {
                        actionManager
                            .createActionPopupMenu(
                                "GithubWorkflowListPopupSelected",
                                actionManager.getAction("Github.Workflow.ToolWindow.List.Popup.Selected") as ActionGroup
                            )
                    } else {
                        actionManager
                            .createActionPopupMenu(
                                "GithubWorkflowListPopup",
                                actionManager.getAction("Github.Workflow.ToolWindow.List.Popup") as ActionGroup
                            )
                    }
                    popupMenu.setTargetComponent(list)
                    popupMenu.component.show(comp, x, y)
                }
            }
            list.addMouseListener(popupHandler)
        }

        private fun installWorkflowRunSelectionSaver(
            list: WorkflowRunList,
            listSelectionHolder: WorkflowRunListSelectionHolder,
        ) {
            var savedSelection: GitHubWorkflowRun? = null

            list.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
                if (!e.valueIsAdjusting) {
                    val selectedIndex = list.selectedIndex
                    if (selectedIndex >= 0 && selectedIndex < list.model.size) {
                        listSelectionHolder.selection = list.model.getElementAt(selectedIndex)
                        savedSelection = null
                    }
                }
            }

            list.model.addListDataListener(object : ListDataListener {
                override fun intervalAdded(e: ListDataEvent) {
                    if (e.type == ListDataEvent.INTERVAL_ADDED)
                        (e.index0..e.index1).find { list.model.getElementAt(it) == savedSelection }
                            ?.run {
                                ApplicationManager.getApplication().invokeLater { ScrollingUtil.selectItem(list, this) }
                            }
                }

                override fun contentsChanged(e: ListDataEvent) {}
                override fun intervalRemoved(e: ListDataEvent) {
                    if (e.type == ListDataEvent.INTERVAL_REMOVED) savedSelection = listSelectionHolder.selection
                }
            })
        }

        fun createWorkflowRunsListComponent(
            context: WorkflowRunDataContext,
            listSelectionHolder: WorkflowRunListSelectionHolder,
            disposable: Disposable,
        ): JComponent {

            val list = WorkflowRunList(context.listModel).apply {
                emptyText.clear()
            }.also {
                it.addFocusListener(object : FocusListener {
                    override fun focusGained(e: FocusEvent?) {
                        if (it.selectedIndex < 0 && !it.isEmpty) it.selectedIndex = 0
                    }

                    override fun focusLost(e: FocusEvent?) {}
                })

                installPopup(it)
                installWorkflowRunSelectionSaver(it, listSelectionHolder)
            }

            //Cannot seem to have context menu, when right click, why?
            val listReloadAction = actionManager.getAction("Github.Workflow.List.Reload") as RefreshAction

            return WorkflowRunListLoaderPanel(context.listLoader, listReloadAction, list).apply {
                errorHandler = LoadingErrorHandler {
                    LOG.debug("Error on GitHub Workflow Run list loading, resetting the loader")
                    context.listLoader.reset()
                }
            }.also {
                listReloadAction.registerCustomShortcutSet(it, disposable)

                val logActionsGroup = DefaultActionGroup()
                logActionsGroup.add(listReloadAction)
                val actionToolbar = ActionManager.getInstance()
                    .createActionToolbar(ActionPlaces.CONTEXT_TOOLBAR, logActionsGroup, false)
                actionToolbar.targetComponent = it

                it.add(actionToolbar.component, BorderLayout.WEST)

                Disposer.register(disposable) {
                    Disposer.dispose(it)
                }
            }
        }
    }
}