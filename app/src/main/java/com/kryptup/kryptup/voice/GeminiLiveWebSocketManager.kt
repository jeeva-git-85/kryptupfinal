package com.kryptup.kryptup.voice

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString

class GeminiLiveWebSocketManager(
    private val apiKey: String,
    private val onMessageReceived: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val client = OkHttpClient.Builder().build()
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect() {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Send initial setup frame for gemini-3.8-live
                val setupJson = JsonObject().apply {
                    add("setup", JsonObject().apply {
                        addProperty("model", "models/gemini-3.8-live")
                        add("generationConfig", JsonObject().apply {
                            addProperty("responseModalities", "TEXT")
                        })
                    })
                }
                webSocket.send(gson.toJson(setupJson))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = gson.fromJson(text, JsonObject::class.java)
                    val serverContent = json.getAsJsonObject("serverContent")
                    val modelTurn = serverContent?.getAsJsonObject("modelTurn")
                    val parts = modelTurn?.getAsJsonArray("parts")
                    val textText = parts?.get(0)?.asJsonObject?.get("text")?.asString

                    if (!textText.isNullOrEmpty()) {
                        scope.launch(Dispatchers.Main) {
                            onMessageReceived(textText)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore non-text chunk frames or parse errors
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Handle raw audio PCM response bytes if configured
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch(Dispatchers.Main) {
                    onError(t.localizedMessage ?: "WebSocket connection failed")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                // Connection closed cleanly
            }
        })
    }

    fun sendLivePrompt(prompt: String) {
        val promptJson = JsonObject().apply {
            add("realtimeInput", JsonObject().apply {
                add("mediaChunks", gson.toJsonTree(listOf(
                    JsonObject().apply {
                        addProperty("mimeType", "text/plain")
                        addProperty("data", Base64.encodeToString(prompt.toByteArray(), Base64.NO_WRAP))
                    }
                )))
            })
        }
        webSocket?.send(gson.toJson(promptJson))
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}
