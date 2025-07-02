package com.oasisfeng.todo.wear.data

data class TodoItem(val title: String, val completed: Boolean = false)

object TodoRepository {
    fun getRecentTodos(): List<TodoItem> {
        return listOf(
            TodoItem("Finish Gemini CLI demo"),
            TodoItem("Book flight to Google I/O"),
            TodoItem("Buy groceries for the week", completed = true),
            TodoItem("Call mom"),
            TodoItem("Schedule dentist appointment"),
            TodoItem("Work on the presentation slides"),
        )
    }
}
