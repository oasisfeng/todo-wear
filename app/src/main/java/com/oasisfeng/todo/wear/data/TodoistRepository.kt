package com.oasisfeng.todo.wear.data

import ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import okhttp3.Request

class TodoistRepository(private val client: OkHttpClient, private val json: Json) {

    suspend fun getTasks(token: String): Result<List<TodoistTask>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(BASE_URL + "tasks")
                .header("Authorization", "Bearer $token")
                .build()
            val response = client.newCall(request).execute()
            if (! response.isSuccessful) throw Exception("Unexpected code $response")
            val deserializer = ApiResponse.serializer(ListSerializer(TodoistTask.serializer()))
            val tasks = json.decodeFromString(deserializer, response.body!!.string()).results
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val BASE_URL = "https://api.todoist.com/api/v1/"

        fun create(): TodoistRepository {
            val json = Json { ignoreUnknownKeys = true }
            val okHttpClient = OkHttpClient.Builder().build()
            return TodoistRepository(okHttpClient, json)
        }
    }
}