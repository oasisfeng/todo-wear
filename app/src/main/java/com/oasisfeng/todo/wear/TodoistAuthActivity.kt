package com.oasisfeng.todo.wear

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.ambient.AmbientLifecycleObserver.AmbientLifecycleCallback
import androidx.wear.phone.interactions.authentication.CodeChallenge
import androidx.wear.phone.interactions.authentication.CodeVerifier
import androidx.wear.phone.interactions.authentication.OAuthRequest
import androidx.wear.phone.interactions.authentication.OAuthResponse
import androidx.wear.phone.interactions.authentication.RemoteAuthClient
import androidx.wear.tiles.TileService
import com.oasisfeng.todo.wear.tile.MainTileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.util.Base64

class TodoistAuthActivity : ComponentActivity() {

    private lateinit var remoteAuthClient: RemoteAuthClient
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val ambientObserver = AmbientLifecycleObserver(this, object : AmbientLifecycleCallback {})

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(ambientObserver)
        val executor = ContextCompat.getMainExecutor(this)
        val state = generateSecureState()
        val codeVerifier = CodeVerifier()
        val url = "https://todoist.com/oauth/authorize?scope=data:read_write,data:delete&state=$state"
        try {
            val request = OAuthRequest.Builder(applicationContext).setCodeChallenge(CodeChallenge(codeVerifier))
                .setAuthProviderUrl(url.toUri()).setClientId(BuildConfig.TODOIST_CLIENT_ID).build()
            remoteAuthClient = RemoteAuthClient.create(this)
            remoteAuthClient.sendAuthorizationRequest(request, executor, object : RemoteAuthClient.Callback() {

                override fun onAuthorizationResponse(request: OAuthRequest, response: OAuthResponse) {
                    val responseUrl = response.responseUrl ?: return finish()
                    if (responseUrl.getQueryParameter("state") != state)
                        return finish().also { Log.e(TAG, "Authorization failed: state mismatch") }
                    val code = responseUrl.getQueryParameter("code")
                    if (code != null) lifecycleScope.launch {
                        val redirectUri = request.requestUrl.getQueryParameter("redirect_uri")!!
                        val token = exchangeCodeForToken(code, redirectUri)
                        if (token != null) {
                            Log.i(TAG, "Authorization succeeded: $token")
                            TokenManager.saveToken(this@TodoistAuthActivity, token)
                            TileService.getUpdater(applicationContext).requestUpdate(MainTileService::class.java)
                            finish()
                        }
                    } else {
                        val error = responseUrl.getQueryParameter("error")
                        Log.e(TAG, "Authorization failed: $error")
                        Toast.makeText(this@TodoistAuthActivity, error, Toast.LENGTH_LONG).show()
                        finish()
                    }
                }

                override fun onAuthorizationError(request: OAuthRequest, errorCode: Int) {
                    Log.e(TAG, "Authorization failed with error code: $errorCode")
                    Toast.makeText(this@TodoistAuthActivity, "Auth error: $errorCode", Toast.LENGTH_LONG).show()
                    finish()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start authorization", e)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            finish()
        }
    }
    private suspend fun exchangeCodeForToken(code: String, redirectUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("client_id", BuildConfig.TODOIST_CLIENT_ID)
                .add("client_secret", BuildConfig.TODOIST_CLIENT_SECRET)
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .build()

            val request = Request.Builder()
                .url("https://todoist.com/oauth/access_token")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Unexpected code $response")

            val tokenResponse = json.decodeFromString(TokenResponse.serializer(), response.body!!.string())
            tokenResponse.accessToken
        } catch (e: Exception) {
            Log.e(TAG, "Failed to exchange code for token", e)
            null
        }
    }

    @Serializable
    data class TokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("token_type") val tokenType: String
    )

    private fun generateSecureState(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private const val TAG = "TodoistAuthActivity"
    }
}
