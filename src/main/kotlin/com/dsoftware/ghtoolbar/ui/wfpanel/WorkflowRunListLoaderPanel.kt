package com.dsoftware.ghtoolbar.ui.wfpanel


import com.dsoftware.ghtoolbar.api.model.GitHubWorkflowRun
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
import com.intellij.ui.*
import com.intellij.util.ui.ComponentWithEmptyText
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.vcs.log.ui.frame.ProgressStripe
import org.jetbrains.plugins.github.exceptions.GithubStatusCodeException
import org.jetbrains.plugins.github.ui.HtmlInfoPanel
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener
import javax.swing.event.ListSelectionEvent

internal class WorkflowRunListLoaderPanel(
    disposable: Disposable,
    private val runListLoader: WorkflowRunListLoader,
    listReloadAction: RefreshAction,
    private val contentComponent: JComponent,
    private val loadAllAfterFirstScroll: Boolean = false
) : BorderLayoutPanel(), Disposable {

    private lateinit var progressStripe: ProgressStripe

    private var userScrolled = false
    val scrollPane = ScrollPaneFactory.createScrollPane(
        contentComponent,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    ).apply {
        isOpaque = false
        viewport.isOpaque = false
        border = JBUI.Borders.empty()
        verticalScrollBar.model.addChangeListener { potentiallyLoadMore() }
        verticalScrollBar.model.addChangeListener {
            if (!userScrolled && verticalScrollBar.value > 0) userScrolled = true
        }
    }

    private val infoPanel = HtmlInfoPanel()

    protected open val loadingText
        get() = "Loading..."

    var errorHandler: LoadingErrorHandler? = null

    init {
        LOG.info("Initialize ListLoaderPanel")
        addToCenter(createCenterPanel(JBUI.Panels.simplePanel(scrollPane).addToTop(infoPanel).apply {
            isOpaque = false
        }))

        runListLoader.addLoadingStateChangeListener(this) {
            setLoading(runListLoader.loading)
            updateEmptyText()
        }

        runListLoader.addErrorChangeListener(this) {
            updateInfoPanel()
            updateEmptyText()
        }

        setLoading(runListLoader.loading)
        updateInfoPanel()
        updateEmptyText()
        errorHandler = LoadingErrorHandler {
            LOG.warn("Error on GitHub Workflow Run list loading, resetting the loader")
            runListLoader.reset()
        }
        listReloadAction.registerCustomShortcutSet(this, disposable)
        val actionsGroup = DefaultActionGroup()
        actionsGroup.add(listReloadAction)
        val actionToolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.CONTEXT_TOOLBAR, actionsGroup, false)
        actionToolbar.targetComponent = this

        add(actionToolbar.component, BorderLayout.WEST)

        Disposer.register(disposable) {
            Disposer.dispose(this)
        }
    }

    private fun updateEmptyText() {
        val emptyText = (contentComponent as? ComponentWithEmptyText)?.emptyText ?: return
        emptyText.clear()
        if (runListLoader.loading) {
            emptyText.text = loadingText
        } else {
            val error = runListLoader.error
            if (error != null) {
                displayErrorStatus(emptyText, error)
            } else {
                displayEmptyStatus(emptyText)
            }
        }
    }

    private fun displayErrorStatus(emptyText: StatusText, error: Throwable) {
        LOG.info("Display error status")
        emptyText.appendText(getErrorPrefix(runListLoader.loadedData.isEmpty()), SimpleTextAttributes.ERROR_ATTRIBUTES)
            .appendSecondaryText(getLoadingErrorText(error), SimpleTextAttributes.ERROR_ATTRIBUTES, null)

        errorHandler?.getActionForError()?.let {
            emptyText.appendSecondaryText(" ${it.getValue("Name")}", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES, it)
        }
    }

    protected open fun updateInfoPanel() {
        val error = runListLoader.error
        if (error != null && runListLoader.loadedData.isNotEmpty()) {
            val errorPrefix = getErrorPrefix(runListLoader.loadedData.isEmpty())
            val errorText = getLoadingErrorText(error, "<br/>")
            val action = errorHandler?.getActionForError()
            if (action != null) {
                //language=HTML
                infoPanel.setInfo(
                    """<html lang="en"><body>$errorPrefix<br/>$errorText<a href=''>&nbsp;${action.getValue("Name")}</a></body></html>""",
                    HtmlInfoPanel.Severity.ERROR
                ) {
                    action.actionPerformed(
                        ActionEvent(
                            infoPanel,
                            ActionEvent.ACTION_PERFORMED,
                            it.eventType.toString()
                        )
                    )
                }

            } else {
                //language=HTML
                infoPanel.setInfo(
                    """<html lang="en"><body>$errorPrefix<br/>$errorText</body></html>""",
                    HtmlInfoPanel.Severity.ERROR
                )
            }
        } else infoPanel.setInfo(null)
    }

    protected open fun getErrorPrefix(listEmpty: Boolean) = if (listEmpty) "Can't load list" else "Can't load full list"

    private fun potentiallyLoadMore() {
        LOG.info("Potentially loading more")
        if (runListLoader.canLoadMore() && ((userScrolled && loadAllAfterFirstScroll) || isScrollAtThreshold())) {
            LOG.info("Load more")
            runListLoader.loadMore()
        }
    }

    private fun isScrollAtThreshold(): Boolean {
        val verticalScrollBar = scrollPane.verticalScrollBar
        val visibleAmount = verticalScrollBar.visibleAmount
        val value = verticalScrollBar.value
        val maximum = verticalScrollBar.maximum
        if (maximum == 0) return false
        val scrollFraction = (visibleAmount + value) / maximum.toFloat()
        if (scrollFraction < 0.5) return false
        return true
    }

    override fun dispose() {}

    fun createCenterPanel(content: JComponent): JPanel {
        LOG.info("Create center panel")
        val stripe = ProgressStripe(
            content, this,
            ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS
        )
        progressStripe = stripe
        return stripe
    }

    fun setLoading(isLoading: Boolean) {
        if (isLoading) progressStripe.startLoading() else progressStripe.stopLoading()
    }

    fun displayEmptyStatus(emptyText: StatusText) {
        LOG.info("Display empty status")
        emptyText.text = "Nothing loaded. "
        emptyText.appendSecondaryText("Refresh", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
            runListLoader.reset()
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

        private fun getLoadingErrorText(error: Throwable, newLineSeparator: String = "\n"): String {
            if (error is GithubStatusCodeException && error.error != null) {
                val githubError = error.error!!
                val builder = StringBuilder(githubError.message)
                if (githubError.errors?.isNotEmpty()!!) {
                    builder.append(": ").append(newLineSeparator)
                    for (e in githubError.errors!!) {
                        builder.append(
                            e.message
                                ?: "${e.code} error in ${e.resource} field ${e.field}"
                        ).append(newLineSeparator)
                    }
                }
                return builder.toString()
            }

            return error.message?.let { addDotIfNeeded(it) } ?: "Unknown loading error."
        }

        private fun addDotIfNeeded(line: String) = if (line.endsWith('.')) line else "$line."
    }
}