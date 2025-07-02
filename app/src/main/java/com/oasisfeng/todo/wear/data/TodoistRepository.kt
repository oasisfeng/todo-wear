package com.oasisfeng.todo.wear.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import okhttp3.Request

class TodoistRepository(private val client: OkHttpClient, private val json: Json) {

    suspend fun getTasks(token: String): Result<List<TodoistTask>> {
        return try {
            val request = Request.Builder()
                .url(BASE_URL + "v2/tasks")
                .header("Authorization", "Bearer $token")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Unexpected code $response")
            val tasks = json.decodeFromString(ListSerializer(TodoistTask.serializer()), response.body!!.string())
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val BASE_URL = "https://api.todoist.com/"

        fun create(): TodoistRepository {
            val json = Json { ignoreUnknownKeys = true }
            val okHttpClient = OkHttpClient.Builder().build()
            return TodoistRepository(okHttpClient, json)
        }
    }
}