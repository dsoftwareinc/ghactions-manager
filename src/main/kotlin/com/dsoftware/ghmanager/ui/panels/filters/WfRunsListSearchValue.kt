package com.dsoftware.ghmanager.ui.panels.filters

import com.dsoftware.ghmanager.api.WorkflowRunFilter
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue
import kotlinx.serialization.Serializable

@Serializable
data class WfRunsListSearchValue(
    override val searchQuery: String? = null,
    val user: String? = null,
    val branch: String? = null,
    val status: Status? = null,
) : ReviewListSearchValue {

    fun getShortText(): String {
        @Suppress("HardCodedStringLiteral")
        return StringBuilder().apply {
            if (searchQuery != null) append(""""$searchQuery"""").append(" ")
            if (status != null) append("status:$status").append(" ")
            if (user != null) append("user:$user").append(" ")
            if (branch != null) append("branch:$branch").append(" ")
        }.toString()
    }

    companion object {
        val EMPTY = WfRunsListSearchValue()
    }

    fun toWorkflowRunFilter(): WorkflowRunFilter {
        return WorkflowRunFilter(branch, status?.toString()?.lowercase(), user, "")
    }

    enum class Status {
        COMPLETED,
        FAILURE,
        SKIPPED,
        STALE,
        SUCCESS,
        TIMED_OUT,
        IN_PROGRESS,
        QUEUED,
    }
}