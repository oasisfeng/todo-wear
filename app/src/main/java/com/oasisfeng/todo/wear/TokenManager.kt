package com.oasisfeng.todo.wear

import android.content.Context
import android.content.SharedPreferences

object TokenManager {

    private const val PREFS_NAME = "com.oasisfeng.todo.wear.prefs"
    private const val KEY_API_TOKEN = "api_token"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_API_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        return getPrefs(context).getString(KEY_API_TOKEN, null)
    }
}