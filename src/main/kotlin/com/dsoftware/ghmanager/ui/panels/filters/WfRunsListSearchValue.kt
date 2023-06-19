package com.dsoftware.ghmanager.ui.panels.filters

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue
import kotlinx.serialization.Serializable

@Serializable
data class WfRunsListSearchValue(
    override val searchQuery: String? = null,
    val user: String? = null,
    val branch: String? = null,
    val status: String? = null,
) : ReviewListSearchValue {

    public fun getShortText(): String {
        @Suppress("HardCodedStringLiteral")
        return StringBuilder().apply {
            if (searchQuery != null) append(""""$searchQuery"""").append(" ")
            if (status != null) append("label:$status").append(" ")
            if (user != null) append("assignee:$user").append(" ")
            if (branch != null) append("author:$branch").append(" ")
        }.toString()
    }

    companion object {
        val EMPTY = WfRunsListSearchValue()
    }
}