package com.dsoftware.ghtoolbar.ui.wfpanel


import com.dsoftware.ghtoolbar.api.model.GitHubWorkflowRun
import com.dsoftware.ghtoolbar.ui.ListLoaderPanel
import com.dsoftware.ghtoolbar.workflow.LoadingErrorHandler
import com.dsoftware.ghtoolbar.workflow.WorkflowRunListSelectionHolder
import com.dsoftware.ghtoolbar.workflow.data.WorkflowRunDataContext
import com.dsoftware.ghtoolbar.workflow.data.WorkflowRunListLoader
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
    disposable: Disposable,
    runListLoader: WorkflowRunListLoader,
    listReloadAction: RefreshAction,
    contentComponent: JComponent
) : ListLoaderPanel(runListLoader, contentComponent), Disposable {

    private lateinit var progressStripe: ProgressStripe

    init {
        errorHandler = LoadingErrorHandler {
            LOG.warn("Error on GitHub Workflow Run list loading, resetting the loader")
            runListLoader.reset()
        }
        listReloadAction.registerCustomShortcutSet(this, disposable)

        val logActionsGroup = DefaultActionGroup()
        logActionsGroup.add(listReloadAction)
        val actionToolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.CONTEXT_TOOLBAR, logActionsGroup, false)
        actionToolbar.targetComponent = this

        add(actionToolbar.component, BorderLayout.WEST)

        Disposer.register(disposable) {
            Disposer.dispose(this)
        }
    }

    override fun createCenterPanel(content: JComponent): JPanel {
        LOG.info("Create center panel")
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

    override fun displayEmptyStatus(emptyText: StatusText) {
        LOG.info("Display empty status")
        emptyText.text = "Nothing loaded. "
        emptyText.appendSecondaryText("Refresh", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
            listLoader.reset()
        }
    }

    companion object {
        private val LOG = logger<WorkflowRunListLoaderPanel>()
        private val actionManager = ActionManager.getInstance()
        private fun installPopup(list: WorkflowRunList) {
            list.addMouseListener(object : PopupHandler() {
                override fun invokePopup(comp: java.awt.Component, x: Int, y: Int) {

                    val (place, groupId) = if (ListUtil.isPointOnSelection(list, x, y)) {
                        Pair("GithubWorkflowListPopupSelected", "Github.Workflow.ToolWindow.List.Popup.Selected")
                    } else {
                        Pair("GithubWorkflowListPopup", "Github.Workflow.ToolWindow.List.Popup")
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

            val listReloadAction = actionManager.getAction("Github.Workflow.List.Reload") as RefreshAction

            return WorkflowRunListLoaderPanel(disposable, context.listLoader, listReloadAction, list)
        }
    }
}