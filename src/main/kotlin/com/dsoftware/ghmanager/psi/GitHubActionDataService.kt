package com.dsoftware.ghmanager.psi

import com.dsoftware.ghmanager.api.GhApiRequestExecutor
import com.dsoftware.ghmanager.data.GhActionsService
import com.dsoftware.ghmanager.psi.model.GitHubAction
import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.intellij.collaboration.api.dto.GraphQLRequestDTO
import com.intellij.collaboration.api.dto.GraphQLResponseDTO
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.EventDispatcher
import com.intellij.util.ResourceUtil
import com.intellij.util.ThrowableConvertor
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.plugins.github.api.GithubApiContentHelper
import org.jetbrains.plugins.github.api.GithubApiRequest.Post
import org.jetbrains.plugins.github.api.GithubApiResponse
import org.jetbrains.plugins.github.api.data.graphql.GHGQLError
import org.jetbrains.plugins.github.exceptions.GithubAuthenticationException
import org.jetbrains.plugins.github.exceptions.GithubConfusingException
import org.jetbrains.plugins.github.exceptions.GithubJsonException
import org.jetbrains.plugins.github.util.GHCompatibilityUtil
import java.io.IOException
import java.time.Duration
import java.util.EventListener
import java.util.concurrent.ScheduledFuture

@Service(Service.Level.PROJECT)
@State(
    name = "GhActionsManagerSettings.ActionsDataCache",
    storages = [Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)]
)
class GitHubActionDataService(
    private val project: Project
) : PersistentStateComponent<GitHubActionDataService.State?>, Disposable {
    private val actionsCache: Cache<String, GitHubAction> = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .maximumSize(200)
        .build()

    val actionsToResolve = mutableSetOf<String>()
    private val task: ScheduledFuture<*>
    private val ghActionsService = project.service<GhActionsService>()

    private val settingsService = project.service<GhActionsSettingsService>()
    private val serverPath: String

    @VisibleForTesting
    internal var requestExecutor: GhApiRequestExecutor? = null

    private val actionsLoadedEventDispatcher = EventDispatcher.create(ActionsLoadedListener::class.java)

    init {
        this.serverPath = determineServerPath()
        ghActionsService.gitHubAccounts.firstOrNull()?.let { account ->
            val token = if (settingsService.state.useGitHubSettings) {
                GHCompatibilityUtil.getOrRequestToken(account, project)
            } else {
                settingsService.state.apiToken
            }
            requestExecutor = if (token == null) null else GhApiRequestExecutor.create(token)
        }
        task = ToolbarUtil.executeTaskAtCustomFrequency(project, 5) {
            resolveActions()
        }
    }

    @Serializable
    data class State(val actions: List<GitHubAction>)

    override fun getState(): State = State(actionsCache.asMap().values.toList())

    override fun loadState(state: State) {
        actionsCache.putAll(state.actions.associateBy { it.name })
    }

    fun whenActionLoaded(actionName: String, listenerMethod: () -> Unit) {
        if (actionsCache.getIfPresent(actionName) != null) {
            listenerMethod()
            return
        }
        actionsToResolve.add(actionName)
        addListener(listenerMethod)
    }

    fun whenActionsLoaded(listenerMethod: () -> Unit) {
        if (actionsToResolve.isEmpty()) {
            listenerMethod()
            return
        }
        addListener(listenerMethod)
    }

    fun getAction(fullActionName: String): GitHubAction? {
        val action = actionsCache.getIfPresent(fullActionName)
        if (action != null) {
            return action
        }
        LOG.info("Action $fullActionName not found in cache, adding to resolve list")
        actionsToResolve.add(fullActionName)
        return null
    }

    private fun addListener(listenerMethod: () -> Unit) {
        val listenerDisposable = Disposer.newDisposable()
        Disposer.register(this, listenerDisposable)
        actionsLoadedEventDispatcher.addListener(object : ActionsLoadedListener {
            override fun actionsLoaded() = runInEdt {
                listenerMethod()
                listenerDisposable.dispose() // Ensure listener will only run once
            }
        }, listenerDisposable)
    }

    private fun resolveActions() {
        if (actionsToResolve.isEmpty()) {
            return
        }
        actionsToResolve.removeAll(actionsCache.asMap().keys)
        actionsToResolve.forEach {
            resolveGithubAction(it)
        }
        actionsLoadedEventDispatcher.multicaster.actionsLoaded()
        actionsLoadedEventDispatcher.listeners.clear()
    }

    private fun determineServerPath(): String {
        val mappings = ghActionsService.knownRepositoriesState.value
        if (mappings.isEmpty()) {
            LOG.info("No repository mappings, using default graphql url")
            return "https://api.github.com/graphql"
        } else {
            val mapping = mappings.iterator().next()
            return mapping.repository.serverPath.toGraphQLUrl()
        }
    }

    @RequiresBackgroundThread
    private fun resolveGithubAction(fullActionName: String) {
        if (actionsCache.getIfPresent(fullActionName) != null) {
            LOG.debug("Action $fullActionName already resolved")
            return
        }
        val requestExecutor = this.requestExecutor
        if (requestExecutor == null) {
            LOG.warn("No request executor available, skipping action resolution for $fullActionName")
            return
        }
        LOG.debug("Resolving action $fullActionName")
        val parts = fullActionName.split("/")
        if (parts.size != 2) {
            LOG.warn("Invalid action name $fullActionName")
            return
        }
        if (Tools.isLocalAction(fullActionName)) { // TODO handle local actions
            LOG.debug("Action $fullActionName is local, skipping resolution")
            return
        }
        resolveActionData(requestExecutor, parts[0], parts[1])
    }


    private fun resolveActionData(requestExecutor: GhApiRequestExecutor, actionOrg: String, actionName: String) {
        val query = ResourceUtil.getResource(
            GhActionsService::class.java.classLoader, "graphql", "getLatestRelease.graphql"
        )?.readText() ?: ""

        val request = TraversedParsed(
            serverPath,
            query,
            mapOf("owner" to actionOrg, "name" to actionName),
            JsonNode::class.java,
            "repository",
            "latestRelease",
            "tag",
            "name"
        )
        try {
            val response = requestExecutor.execute(request)
            val version = response.toString().replace("\"", "")
            actionsCache.put("$actionOrg/$actionName", GitHubAction(actionName, version))
        } catch (e: IOException) {
            LOG.warn("Failed to get latest version of action $actionOrg/$actionName", e)
            return
        }
    }

    private class TraversedParsed<out T : Any>(
        url: String,
        private val query: String,
        private val variablesObject: Any,
        private val clazz: Class<out T>,
        private vararg val pathFromData: String
    ) : Post<T>(GithubApiContentHelper.JSON_MIME_TYPE, url) {

        override val body: String
            get() = GithubApiContentHelper.toJson(GraphQLRequestDTO(query, variablesObject), true)

        private fun throwException(errors: List<GHGQLError>): Nothing {
            if (errors.any {
                    it.type.equals(
                        "INSUFFICIENT_SCOPES", true
                    )
                }) throw GithubAuthenticationException("Access token has not been granted the required scopes.")

            if (errors.size == 1) throw GithubConfusingException(errors.single().toString())
            throw GithubConfusingException(errors.toString())
        }

        override fun extractResult(response: GithubApiResponse): T {
            return parseResponse(response, clazz, pathFromData)
                ?: throw GithubJsonException("Non-nullable entity is null or entity path is invalid")
        }

        private fun <T> parseResponse(
            response: GithubApiResponse, clazz: Class<T>, pathFromData: Array<out String>
        ): T? {
            val result: GraphQLResponseDTO<out JsonNode, GHGQLError> = parseGQLResponse(response, JsonNode::class.java)
            val data = result.data
            if (data != null && !data.isNull) {
                var node: JsonNode = data
                for (path in pathFromData) {
                    node = node[path] ?: break
                }
                if (!node.isNull) return GithubApiContentHelper.fromJson(node.toString(), clazz, true)
            }
            val errors = result.errors
            if (errors == null) return null
            else throwException(errors)
        }

        private fun <T> parseGQLResponse(
            response: GithubApiResponse, dataClass: Class<out T>
        ): GraphQLResponseDTO<out T, GHGQLError> {
            return response.readBody(ThrowableConvertor {
                @Suppress("UNCHECKED_CAST") GithubApiContentHelper.readJsonObject(
                    it, GraphQLResponseDTO::class.java, dataClass, GHGQLError::class.java, gqlNaming = true
                ) as GraphQLResponseDTO<T, GHGQLError>
            })
        }
    }


    private interface ActionsLoadedListener : EventListener {
        fun actionsLoaded() {}
    }


    companion object {
        private val LOG = logger<GitHubActionDataService>()
    }

    override fun dispose() {
        task.cancel(true)
    }
}