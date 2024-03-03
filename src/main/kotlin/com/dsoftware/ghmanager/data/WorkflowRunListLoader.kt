package com.dsoftware.ghmanager.data

import ai.grazie.utils.applyIf
import com.dsoftware.ghmanager.api.GithubApi
import com.dsoftware.ghmanager.api.WorkflowRunFilter
import com.dsoftware.ghmanager.api.model.WorkflowRun
import com.dsoftware.ghmanager.api.model.WorkflowType
import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.intellij.collaboration.async.CompletableFutureUtil
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.util.EventDispatcher
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor
import org.jetbrains.plugins.github.api.GithubApiRequests
import org.jetbrains.plugins.github.api.data.GHUser
import org.jetbrains.plugins.github.api.data.request.GithubRequestPagination
import org.jetbrains.plugins.github.util.NonReusableEmptyProgressIndicator
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledFuture
import kotlin.properties.Delegates

class WorkflowRunListLoader(
    project: Project,
    parentDisposable: Disposable,
    private val requestExecutor: GithubApiRequestExecutor,
    private val repositoryCoordinates: RepositoryCoordinates,
    private var filter: WorkflowRunFilter,
) : Disposable {
    private val settingsService = project.service<GhActionsSettingsService>()
    val workflowRunsListModel = CollectionListModel<WorkflowRun>()
    val repoCollaborators = ArrayList<GHUser>()
    val repoBranches = ArrayList<String>()
    val workflowTypes = ArrayList<WorkflowType>()
    private val progressManager = ProgressManager.getInstance()
    private var lastFuture = CompletableFuture.completedFuture(emptyList<WorkflowRun>())
    private val loadingStateChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)
    private val errorChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)
    val url: String = GithubApi.getWorkflowRuns(repositoryCoordinates, filter).url
    var totalCount: Int = 1
    private val page: Int = 1
    private val task: ScheduledFuture<*>
    var refreshRuns: Boolean = true
    private var progressIndicator = NonReusableEmptyProgressIndicator()
    var error: Throwable? by Delegates.observable(null) { _, _, _ ->
        errorChangeEventDispatcher.multicaster.eventOccurred()
    }

    var loading: Boolean by Delegates.observable(false) { _, _, _ ->
        loadingStateChangeEventDispatcher.multicaster.eventOccurred()
    }

    init {
        LOG.debug("Initialize WorkflowRunListLoader for ${repositoryCoordinates.repositoryPath}")
        Disposer.register(parentDisposable, this)
        task = ToolbarUtil.executeTaskAtSettingsFrequency(project) {
            if (refreshRuns && error == null) loadMore(update = true)
        }
        LOG.debug("emptying workflowRunsListModel")
        workflowRunsListModel.removeAll()
    }

    fun loadMore(update: Boolean = false) {
        if (canLoadMore() || update) {
            loading = true
            requestLoadMore(progressIndicator, update).handleOnEdt { list, error ->
                if (progressIndicator.isCanceled) return@handleOnEdt
                loading = false
                if (error != null) {
                    if (!CompletableFutureUtil.isCancellation(error)) this.error = error
                } else if (!list.isNullOrEmpty()) {
                    workflowRunsListModel.addAll(0, list.sorted())
                }
                LOG.debug("workflowRunsListModel has ${workflowRunsListModel.size} runs")
            }
        }
    }

    private fun requestLoadMore(indicator: ProgressIndicator, update: Boolean): CompletableFuture<List<WorkflowRun>> {
        applyIf(repoCollaborators.isEmpty()) {
            progressManager.submitIOTask(NonReusableEmptyProgressIndicator()) { updateCollaborators(it) }
        }.applyIf(repoBranches.isEmpty()) {
            progressManager.submitIOTask(NonReusableEmptyProgressIndicator()) { updateBranches(it) }
        }.applyIf(workflowTypes.isEmpty()) {
            progressManager.submitIOTask(NonReusableEmptyProgressIndicator()) { updateWorkflowTypes(it) }
        }
        lastFuture = lastFuture.thenCompose {
            progressManager.submitIOTask(indicator) { doLoadMore(indicator, update) }
        }
        return lastFuture
    }

    private fun updateCollaborators(indicator: ProgressIndicator) {
        val collaboratorSet = mutableSetOf<GHUser>()
        var nextLink: String? = null
        do {
            val request = if (nextLink == null) {
                GithubApiRequests.Repos.Collaborators.get(
                    repositoryCoordinates.serverPath,
                    repositoryCoordinates.repositoryPath.owner,
                    repositoryCoordinates.repositoryPath.repository,
                    GithubRequestPagination()
                )
            } else {
                GithubApiRequests.Repos.Collaborators.get(nextLink)
            }
            LOG.info("Calling ${request.url}")
            val response = requestExecutor.execute(indicator, request)
            response.items.map {
                collaboratorSet.add(GHUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null))
            }
            nextLink = response.nextLink
        } while (nextLink != null)
        repoCollaborators.clear()
        repoCollaborators.addAll(collaboratorSet)
    }

    private fun updateBranches(indicator: ProgressIndicator) {
        val branchSet = mutableSetOf<String>()
        var nextLink: String? = null
        do {
            val request = if (nextLink == null) {
                GithubApiRequests.Repos.Branches.get(
                    repositoryCoordinates.serverPath,
                    repositoryCoordinates.repositoryPath.owner,
                    repositoryCoordinates.repositoryPath.repository,
                    GithubRequestPagination()
                )
            } else {
                GithubApiRequests.Repos.Branches.get(nextLink)
            }
            LOG.info("Calling ${request.url}")
            val response = requestExecutor.execute(indicator, request)
            response.items.map { branchSet.add(it.name) }
            nextLink = response.nextLink
        } while (response.hasNext)
        repoBranches.clear()
        repoBranches.addAll(branchSet)
    }

    private fun updateWorkflowTypes(indicator: ProgressIndicator) {
        val workflowTypesSet = mutableSetOf<WorkflowType>()
        var nextPage: Int = 0
        do {
            nextPage += 1
            val request = GithubApi.getWorkflowTypes(
                repositoryCoordinates,
                GithubRequestPagination(pageNumber = nextPage, pageSize = 100)
            )
            LOG.info("Calling ${request.url}")
            val response = requestExecutor.execute(indicator, request)
            workflowTypesSet.addAll(response.workflows)
        } while (nextPage * 100 < response.totalCount)
        workflowTypes.clear()
        workflowTypes.addAll(workflowTypesSet)
    }

    override fun dispose() {
        progressIndicator.cancel()
        task.cancel(true)
    }

    fun reset() {
        LOG.debug("Removing all from the list model")
        lastFuture = lastFuture.handle { _, _ ->
            listOf()
        }
        progressIndicator.cancel()
        progressIndicator = NonReusableEmptyProgressIndicator()
        error = null
        loading = false
        workflowRunsListModel.removeAll()
        repoCollaborators.clear()
        repoBranches.clear()
        workflowTypes.clear()
    }

    private fun canLoadMore() = !loading && (page * settingsService.state.pageSize < totalCount)

    private fun doLoadMore(indicator: ProgressIndicator, update: Boolean): List<WorkflowRun> {
        LOG.debug("Do load more update: $update, indicator: $indicator")
        val request = GithubApi.getWorkflowRuns(
            repositoryCoordinates,
            filter,
            pagination = GithubRequestPagination(page, settingsService.state.pageSize),
        )
        LOG.info("Calling ${request.url}")
        val wfRunsResponse = try {
            requestExecutor.execute(indicator, request)
        } catch (e: Exception) {
            LOG.error("Error loading workflow runs")
            throw e
        }
        totalCount = wfRunsResponse.totalCount
        val workflowRuns = wfRunsResponse.workflowRuns
        if (update) {
            val existingRunIds = workflowRunsListModel.items.mapIndexed { idx, it -> it.id to idx }.toMap()
            val newRuns = workflowRuns.filter { !existingRunIds.containsKey(it.id) }

            workflowRuns
                .filter { existingRunIds.contains(it.id) }
                .forEach { // Update
                    val index = existingRunIds.getOrDefault(it.id, null)
                    if (index != null && workflowRunsListModel.getElementAt(index) != it) {
                        workflowRunsListModel.setElementAt(it, index)
                    }
                }
            return newRuns
        }
        LOG.debug("Got ${workflowRuns.size} in page $page workflows (totalCount=$totalCount)")
        return workflowRuns
    }

    fun addLoadingStateChangeListener(disposable: Disposable, listener: () -> Unit) =
        SimpleEventListener.addDisposableListener(loadingStateChangeEventDispatcher, disposable, listener)

    fun addErrorChangeListener(disposable: Disposable, listener: () -> Unit) =
        SimpleEventListener.addDisposableListener(errorChangeEventDispatcher, disposable, listener)

    fun setFilter(value: WorkflowRunFilter) {
        this.filter = value
    }

    companion object {
        private val LOG = logger<WorkflowRunListLoader>()
    }
}
//
//object Contributors : GithubApiRequests.Entity("/contributors") {
//
//    fun get(server: GithubServerPath, username: String, repoName: String, pagination: GithubRequestPagination? = null) =
//        GithubApiRequest.Get.jsonPage<GithubUserWithPermissions>(
//            GithubApiRequests.getUrl(
//                server, "/repos/$username/$repoName", urlSuffix,
//                getQuery(pagination?.toString().orEmpty())
//            )
//        ).withOperationName("get contributors")
//
//    private fun getQuery(vararg queryParts: String): String {
//        val builder = StringBuilder()
//        for (part in queryParts) {
//            if (part.isEmpty()) continue
//            if (builder.isEmpty()) builder.append("?")
//            else builder.append("&")
//            builder.append(part)
//        }
//        return builder.toString()
//    }
//}