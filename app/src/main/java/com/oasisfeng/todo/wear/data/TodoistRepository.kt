package com.oasisfeng.todo.wear.data

import ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class TodoistRepository(private val client: OkHttpClient, private val json: Json) {

    suspend fun getTasks(token: String, limit: Int, filter: String? = null): Result<List<TodoistTask>> = withContext(Dispatchers.IO) {
        try {
            val url = (BASE_URL + if (filter.isNullOrEmpty()) "tasks" else "tasks/filter").toHttpUrl().newBuilder()
            url.addQueryParameter("limit", limit.toString())
            if (! filter.isNullOrEmpty()) url.addQueryParameter("query", filter)
            val request = Request.Builder().url(url.build()).header("Authorization", "Bearer $token").build()
            val response = client.newCall(request).execute()
            if (! response.isSuccessful)
                return@withContext Result.failure(IOException("Unexpected server $response"))
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
