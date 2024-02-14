package com.dsoftware.ghmanager.api.model

import kotlinx.serialization.Serializable

data class WorkflowTypes(
    val totalCount: Int,
    val workflows: List<WorkflowType> = emptyList()
)

@Serializable
data class WorkflowType(
    val id: Long,
    val name: String,
    val path: String,
    val state: String,
)