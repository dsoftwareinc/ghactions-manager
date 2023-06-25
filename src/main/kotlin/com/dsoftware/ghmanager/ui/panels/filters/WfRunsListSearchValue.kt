package com.dsoftware.ghmanager.ui.panels.filters

import com.dsoftware.ghmanager.api.WorkflowRunFilter
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue
import kotlinx.serialization.Serializable

@Serializable
data class WfRunsListSearchValue(
    override val searchQuery: String? = null,
    val actor: String? = null,
    val branch: String? = null,
    val status: Status? = null,
) : ReviewListSearchValue {

    fun getShortText(): String {
        @Suppress("HardCodedStringLiteral")
        return StringBuilder().apply {
            if (searchQuery != null) append(""""$searchQuery"""").append(" ")
            if (status != null) append("status:$status").append(" ")
            if (actor != null) append("user:$actor").append(" ")
            if (branch != null) append("branch:$branch").append(" ")
        }.toString()
    }

    companion object {
        val EMPTY = WfRunsListSearchValue()
    }

    fun toWorkflowRunFilter(): WorkflowRunFilter {
        return WorkflowRunFilter(branch, status?.toString()?.lowercase(), actor, "")
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
}