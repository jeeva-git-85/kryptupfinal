package com.kryptup.kryptup.voice

import com.kryptup.kryptup.BuildConfig
import com.kryptup.kryptup.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

sealed class AgentAction {
    data class TextResponse(val text: String) : AgentAction()
    data class NavigateScreen(val screen: String, val message: String) : AgentAction()
    data class SendCrypto(val amount: String, val symbol: String, val recipient: String, val message: String) : AgentAction()
}

class GeminiManager {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val retrofitApi by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiRestApi::class.java)
    }

    // App Function Declarations for Gemini Agent
    private val appTools = listOf(
        GeminiTool(
            functionDeclarations = listOf(
                GeminiFunctionDeclaration(
                    name = "navigateScreen",
                    description = "Navigates to a specific screen in KryptUp. Allowed screens: 'wallet', 'bank', 'trading', 'scanner', 'chat', 'rewards'.",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "screen" to mapOf(
                                "type" to "STRING",
                                "description" to "Target screen name: wallet, bank, trading, scanner, chat, or rewards."
                            )
                        ),
                        "required" to listOf("screen")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "sendCrypto",
                    description = "Initiates a crypto or token transfer to a recipient.",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "amount" to mapOf("type" to "STRING", "description" to "Transfer amount e.g. 0.5"),
                            "symbol" to mapOf("type" to "STRING", "description" to "Token symbol e.g. ETH, USDT"),
                            "recipient" to mapOf("type" to "STRING", "description" to "Recipient name or wallet address")
                        ),
                        "required" to listOf("amount", "symbol", "recipient")
                    )
                )
            )
        )
    )

    suspend fun executeAgentRequest(userPrompt: String): AgentAction = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey.length < 10) {
            return@withContext AgentAction.TextResponse("KryptUp AI: Please configure your Gemini API key inside local.properties.")
        }

        val systemInstructionText = "You are KryptUp AI, an autonomous financial agent for the KryptUp Super-App. When a user asks to open a screen or send crypto, invoke the corresponding tool."

        val request = GeminiRestRequest(
            contents = listOf(
                GeminiRestContent(
                    parts = listOf(GeminiRestPart(text = userPrompt))
                )
            ),
            tools = appTools,
            systemInstruction = GeminiRestContent(
                parts = listOf(GeminiRestPart(text = systemInstructionText))
            )
        )

        val candidateModels = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro", "gemini-pro")

        for (modelName in candidateModels) {
            try {
                val response = retrofitApi.generateContent(
                    model = modelName,
                    apiKeyQuery = apiKey,
                    request = request
                )

                val candidatePart = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()

                // 1. Check if Gemini called a native app tool
                val functionCall = candidatePart?.functionCall
                if (functionCall != null) {
                    return@withContext when (functionCall.name) {
                        "navigateScreen" -> {
                            val targetScreen = functionCall.args?.get("screen")?.toString() ?: "wallet"
                            AgentAction.NavigateScreen(targetScreen, "Navigating to $targetScreen screen...")
                        }
                        "sendCrypto" -> {
                            val amt = functionCall.args?.get("amount")?.toString() ?: "0"
                            val sym = functionCall.args?.get("symbol")?.toString() ?: "ETH"
                            val rec = functionCall.args?.get("recipient")?.toString() ?: "Recipient"
                            AgentAction.SendCrypto(amt, sym, rec, "Opening transfer for $amt $sym to $rec...")
                        }
                        else -> AgentAction.TextResponse(candidatePart.text ?: "Executing action...")
                    }
                }

                // 2. Otherwise return plain text response
                val textResponse = candidatePart?.text
                if (!textResponse.isNullOrEmpty()) {
                    return@withContext AgentAction.TextResponse(textResponse)
                }
            } catch (e: Exception) {
                android.util.Log.w("GeminiManager", "Gemini AI model $modelName failed: ${e.message}")
            }
        }

        // Fall back to intelligent local Financial NLP engine if remote Gemini API key/endpoint fails
        val localNlp = FinancialNlpEngine()
        when (val intent = localNlp.parseCommand(userPrompt)) {
            is VoiceIntent.NavigateScreen -> {
                AgentAction.NavigateScreen(intent.screen, "Navigating to ${intent.screen} screen...")
            }
            is VoiceIntent.SendCrypto -> {
                AgentAction.SendCrypto(
                    amount = intent.amount.toString(),
                    symbol = intent.symbol,
                    recipient = intent.recipient,
                    message = "Opening transfer for ${intent.amount} ${intent.symbol} to ${intent.recipient}..."
                )
            }
            is VoiceIntent.CheckBalance -> {
                AgentAction.TextResponse("KryptUp AI: Checking your portfolio balance...")
            }
            is VoiceIntent.CheckPrice -> {
                AgentAction.TextResponse("KryptUp AI: Fetching live market price for ${intent.symbol}...")
            }
            is VoiceIntent.Unknown -> {
                AgentAction.TextResponse("KryptUp AI Assistant: I can help you send crypto, link bank accounts, trade assets, or check market prices.")
            }
        }
    }

    // Retain legacy method for backward compatibility
    suspend fun processPrompt(userPrompt: String): String {
        return when (val action = executeAgentRequest(userPrompt)) {
            is AgentAction.TextResponse -> action.text
            is AgentAction.NavigateScreen -> action.message
            is AgentAction.SendCrypto -> action.message
        }
    }
}
