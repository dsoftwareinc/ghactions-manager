package com.dsoftware.ghmanager.ui.panels.filters

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue
import kotlinx.serialization.Serializable

@Serializable
data class WfRunsListSearchValue(
    override val searchQuery: String? = null,
    val user: String? = null,
    val branch: String? = null,
    val type: String? = null,
) : ReviewListSearchValue {
    companion object {
        val EMPTY = WfRunsListSearchValue()
    }
}