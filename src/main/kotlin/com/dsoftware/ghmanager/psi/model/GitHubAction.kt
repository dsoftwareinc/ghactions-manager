package com.dsoftware.ghmanager.psi.model

import kotlinx.serialization.Serializable

@Serializable
data class GitHubAction(
    val name: String,
    val latestVersion: String? = null,
) {
    val isLocalAction: Boolean
        get() = name.startsWith("./")
}