package com.oasisfeng.todo.wear.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TodoistTask(
    val id: String,
    val content: String,
    @SerialName("due") val dueDate: DueDate? = null
)

@Serializable
data class DueDate(
    val string: String
)