package com.dsoftware.ghmanager.ui.panels.wfruns


import com.dsoftware.ghmanager.data.WorkflowRunListLoader
import com.dsoftware.ghmanager.data.WorkflowRunSelectionContext
import com.dsoftware.ghmanager.i18n.MessagesBundle.message
import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.dsoftware.ghmanager.ui.panels.wfruns.filters.WfRunsFiltersFactory
import com.dsoftware.ghmanager.ui.panels.wfruns.filters.WfRunsSearchPanelViewModel
import com.dsoftware.ghmanager.ui.panels.wfruns.filters.WorkflowRunListQuickFilter
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionPopupMenu
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ListUtil
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.vcs.ui.ProgressStripe
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.plugins.github.exceptions.GithubStatusCodeException
import org.jetbrains.plugins.github.ui.HtmlInfoPanel
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.ScrollPaneConstants
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener


class WorkflowRunsListPanel(
    parentDisposable: Disposable,
    private val context: WorkflowRunSelectionContext,
) : BorderLayoutPanel(), Disposable {
    private val scope = MainScope().also { Disposer.register(parentDisposable) { it.cancel() } }
    private val workflowRunsLoader: WorkflowRunListLoader
        get() = context.runsListLoader

    @VisibleForTesting
    val runListComponent = WorkflowRunsListComponent(workflowRunsLoader.workflowRunsListModel).apply {
        emptyText.clear()
        installPopup(this)
        ToolbarUtil.installSelectionHolder(this, context.runSelectionHolder)
    }
    private val scrollPane = ScrollPaneFactory.createScrollPane(
        runListComponent,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    ).apply {
        isOpaque = false
        viewport.isOpaque = false
        border = JBUI.Borders.empty()
    }
    private val progressStripe: ProgressStripe
    private val infoPanel = HtmlInfoPanel()

    init {
        Disposer.register(parentDisposable, this)
        val searchVm = WfRunsSearchPanelViewModel(scope, context)
        scope.launch {
            searchVm.searchState.collectLatest {
                context.updateFilter(it.toWorkflowRunFilter())
            }
        }

        val searchPanel = WfRunsFiltersFactory(searchVm).createWfRunsFiltersPanel(scope)
        context.toolWindow.project.messageBus.connect(this)
            .subscribe(GitRepository.GIT_REPO_CHANGE, GitRepositoryChangeListener { repo ->
                val currentFilter = searchVm.searchState.value
                val currentBranchName = repo.currentBranchName
                if (currentFilter.currentBranchFilter && currentBranchName != currentFilter.branch) {
                    searchVm.searchState.update {
                        it.copy(branch = currentBranchName)
                    }
                }
                searchVm.quickFilters = listOf(
                    WorkflowRunListQuickFilter.All(),
                    WorkflowRunListQuickFilter.InProgres(),
                    WorkflowRunListQuickFilter.CurrentUser(context),
                    WorkflowRunListQuickFilter.CurrentBranch(repo.currentBranchName),
                )
            })


        progressStripe = ProgressStripe(
            JBUI.Panels.simplePanel(scrollPane).addToTop(infoPanel).addToTop(searchPanel).apply {
                isOpaque = false
            }, this, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS
        )
        addToCenter(progressStripe)
        workflowRunsLoader.workflowRunsListModel.addListDataListener(object : ListDataListener {
            override fun intervalAdded(e: ListDataEvent) = updateInfoPanelAndEmptyText()
            override fun contentsChanged(e: ListDataEvent) {}
            override fun intervalRemoved(e: ListDataEvent) = updateInfoPanelAndEmptyText()
        })
        workflowRunsLoader.addLoadingStateChangeListener(this) {
            setLoading(workflowRunsLoader.loading)
            updateInfoPanelAndEmptyText()
        }

        workflowRunsLoader.addErrorChangeListener(this) { updateInfoPanelAndEmptyText() }
        setLoading(workflowRunsLoader.loading)
        updateInfoPanelAndEmptyText()
        val actionsManager = ActionManager.getInstance()
        val actionsGroup = actionsManager.getAction("GHWorkflows.ActionGroup") as ActionGroup
        val actionToolbar = actionsManager.createActionToolbar(ActionPlaces.CONTEXT_TOOLBAR, actionsGroup, false)
        actionToolbar.targetComponent = this

        add(actionToolbar.component, BorderLayout.WEST)
    }

    private fun updateInfoPanelAndEmptyText() {
        runListComponent.emptyText.clear()
        if (workflowRunsLoader.loading) {
            runListComponent.emptyText.text = message("panel.workflow-runs.loading")
            return
        }
        val error = workflowRunsLoader.error
        if (error == null) {
            runListComponent.emptyText.apply {
                text = message("panel.workflow-runs.no-runs")
                appendSecondaryText(
                    message("panel.workflow-runs.no-runs.refresh"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES
                ) {
                    workflowRunsLoader.reset()
                    workflowRunsLoader.loadMore(true)
                }
            }
            infoPanel.setInfo(
                when {
                    workflowRunsLoader.loading -> message("panel.workflow-runs.loading")
                    workflowRunsLoader.workflowRunsListModel.isEmpty -> message("panel.workflow-runs.no-runs")
                    else -> message(
                        "panel.workflow-runs.loaded",
                        workflowRunsLoader.workflowRunsListModel.size,
                        workflowRunsLoader.totalCount
                    )
                }
            )
            return
        }
        LOG.warn("Got error when getting workflow-runs: $error")
        runListComponent.emptyText.setText(
            message("panel.workflow-runs.error"), SimpleTextAttributes.ERROR_ATTRIBUTES
        ).appendLine(
            getLoadingErrorText(workflowRunsLoader.url, error), SimpleTextAttributes.ERROR_ATTRIBUTES, null
        )

        runListComponent.emptyText.attachTo(runListComponent)
    }


    override fun dispose() {}

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) progressStripe.startLoading() else progressStripe.stopLoading()
    }

    companion object {
        private val LOG = logger<WorkflowRunsListPanel>()

        private fun installPopup(list: WorkflowRunsListComponent) {
            val actionManager = ActionManager.getInstance()
            list.addMouseListener(object : PopupHandler() {
                override fun invokePopup(comp: Component, x: Int, y: Int) {
                    val (place, groupId) = if (ListUtil.isPointOnSelection(list, x, y)) {
                        Pair("GithubWorkflowListPopupSelected", "Github.Workflow.ToolWindow.List.Popup.Selected")
                    } else {
                        Pair("GithubWorkflowListPopup", "Github.Workflow.ToolWindow.List.Popup")
                    }
                    val popupMenu: ActionPopupMenu = actionManager.createActionPopupMenu(
                        place, actionManager.getAction(groupId) as ActionGroup
                    )

                    popupMenu.setTargetComponent(list)
                    popupMenu.component.show(comp, x, y)
                }
            })
        }

        private fun getLoadingErrorText(url: String, error: Throwable, newLineSeparator: String = "\n"): String {
            if (error is GithubStatusCodeException && error.error != null) {
                val githubError = error.error!!
                val builder =
                    StringBuilder("url: $url").append(newLineSeparator).append(error.message).append(newLineSeparator)
                if (githubError.errors?.isNotEmpty()!!) {
                    builder.append(": ").append(newLineSeparator)
                    for (e in githubError.errors!!) {
                        builder.append(
                            e.message ?: "${e.code} error in ${e.resource} field ${e.field}"
                        ).append(newLineSeparator)
                    }
                }
                val res = builder.toString()
                LOG.warn("Error: $res when getting URL: $url")
                return res
            }

            return error.message?.let { addDotIfNeeded(it) } ?: message("panel.workflow-runs.unknown-error")
        }

        private fun addDotIfNeeded(line: String) = if (line.endsWith('.')) line else "$line."
    }
}