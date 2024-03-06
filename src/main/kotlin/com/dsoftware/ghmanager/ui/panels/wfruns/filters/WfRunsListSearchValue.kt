package com.dsoftware.ghmanager.ui.panels.wfruns.filters

import com.dsoftware.ghmanager.api.WorkflowRunFilter
import com.dsoftware.ghmanager.api.model.WorkflowType
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.plugins.github.api.data.GHUser

@Serializable
data class WfRunsListSearchValue(
    override val searchQuery: String? = null,
    @Transient val actor: GHUser? = null,
    val branch: String? = null,
    val status: Status? = null,
    val event: Event? = null,
    val workflowType: WorkflowType? = null,
    val currentBranchFilter: Boolean = false,
) : ReviewListSearchValue {

    fun getShortText(): String {
        @Suppress("HardCodedStringLiteral")
        return StringBuilder().apply {
            if (searchQuery != null) append(""""$searchQuery"""").append(" ")
            if (status != null) append("status:$status").append(" ")
            if (actor != null) append("user:${actor.shortName}").append(" ")
            if (branch != null) append("branch:$branch").append(" ")
        }.toString()
    }

    companion object {
        val EMPTY = WfRunsListSearchValue()
    }

    fun toWorkflowRunFilter(): WorkflowRunFilter {
        return WorkflowRunFilter(
            branch,
            status?.toString()?.lowercase(),
            actor?.shortName,
            event?.value,
            workflowType?.id
        )
    }

    enum class Status {
        COMPLETED,
        QUEUED,
        CANCELLED,
        SKIPPED,
        SUCCESS,
        IN_PROGRESS,
        FAILURE,
        STALE,
        TIMED_OUT,
    }

    enum class Event(val value: String) {
        PULL_REQUEST("pull_request"),
        PUSH("push"),
        PULL_REQUEST_TARGET("pull_request_target"),
        RELEASE("release"),
    }
}