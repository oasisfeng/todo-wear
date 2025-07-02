package com.oasisfeng.todo.wear

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.wear.tiles.TileService
import com.oasisfeng.todo.wear.tile.MainTileService

class TodoistAuthActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todoist_auth)

        val apiTokenInput = findViewById<EditText>(R.id.api_token_input)
        val saveButton = findViewById<Button>(R.id.save_button)

        saveButton.setOnClickListener {
            val apiToken = apiTokenInput.text.toString().trim()
            if (apiToken.isNotEmpty()) {
                TokenManager.saveToken(this, apiToken)
                TileService.getUpdater(applicationContext).requestUpdate(MainTileService::class.java)
                finish()
            }
        }
    }
}