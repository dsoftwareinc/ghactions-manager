package com.dsoftware.ghmanager.psi

import com.dsoftware.ghmanager.api.GhApiRequestExecutor
import com.dsoftware.ghmanager.data.GhActionsService
import com.dsoftware.ghmanager.psi.model.GitHubAction
import com.dsoftware.ghmanager.ui.ToolbarUtil
import com.dsoftware.ghmanager.ui.settings.GhActionsSettingsService
import com.fasterxml.jackson.databind.JsonNode
import com.intellij.collaboration.api.dto.GraphQLRequestDTO
import com.intellij.collaboration.api.dto.GraphQLResponseDTO
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.ResourceUtil
import com.intellij.util.ThrowableConvertor
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.serialization.Serializable
import org.jetbrains.plugins.github.api.GithubApiContentHelper
import org.jetbrains.plugins.github.api.GithubApiRequest.Post
import org.jetbrains.plugins.github.api.GithubApiResponse
import org.jetbrains.plugins.github.api.data.graphql.GHGQLError
import org.jetbrains.plugins.github.exceptions.GithubAuthenticationException
import org.jetbrains.plugins.github.exceptions.GithubConfusingException
import org.jetbrains.plugins.github.exceptions.GithubJsonException
import org.jetbrains.plugins.github.util.GHCompatibilityUtil
import java.io.IOException
import java.util.concurrent.ScheduledFuture

@Service(Service.Level.PROJECT)
@State(name = "GitHubActionCache", storages = [Storage("githubActionCache.xml")])
class GitHubActionCache(private val project: Project) : PersistentStateComponent<GitHubActionCache.State?> {
    private var state = State()
    val actionsToResolve = mutableSetOf<String>()
    private val task: ScheduledFuture<*>

    private val ghActionsService = project.service<GhActionsService>()
    private val settingsService = project.service<GhActionsSettingsService>()
    private val serverPath: String
    private var requestExecutor: GhApiRequestExecutor? = null

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
            actionsToResolve.removeAll(state.actions.keys)
            actionsToResolve.forEach {
                resolveGithubAction(it)
            }
        }
    }

    @Serializable
    class State {
        val actions = TimedCache()
    }

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun cleanup() {
        state.actions.cleanup()
    }

    fun getAction(fullActionName: String): GitHubAction? {
        if (state.actions.containsKey(fullActionName)) {
            return state.actions[fullActionName]
        }
        LOG.info("Action $fullActionName not found in cache, adding to resolve list")
        actionsToResolve.add(fullActionName)
        return null
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

    @RequiresEdt
    private fun resolveGithubAction(fullActionName: String) {
        if (state.actions.containsKey(fullActionName)) {
            return
        }
        val requestExecutor = this.requestExecutor
        if (requestExecutor == null) {
            LOG.warn("Failed to get latest version of action $fullActionName: no GitHub account found")
            return
        }
        LOG.info("Resolving action $fullActionName")
        val actionOrg = fullActionName.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        val actionName = fullActionName.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        val query = ResourceUtil.getResource(
            GhActionsService::class.java.classLoader, "graphql/query", "getLatestRelease.graphql"
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
            state.actions[fullActionName] = GitHubAction(actionName, version)
        } catch (e: IOException) {
            LOG.warn("Failed to get latest version of action $fullActionName", e)
        }
    }


    class TraversedParsed<out T : Any>(
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
                        "INSUFFICIENT_SCOPES",
                        true
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

    companion object {
        private val LOG = logger<GitHubActionCache>()
    }
}