package com.kryptup.kryptup.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiRestApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKeyQuery: String,
        @Body request: GeminiRestRequest
    ): GeminiRestResponse
}

data class GeminiRestRequest(
    @SerializedName("contents") val contents: List<GeminiRestContent>,
    @SerializedName("tools") val tools: List<GeminiTool>? = null,
    @SerializedName("system_instruction") val systemInstruction: GeminiRestContent? = null
)

data class GeminiTool(
    @SerializedName("function_declarations") val functionDeclarations: List<GeminiFunctionDeclaration>
)

data class GeminiFunctionDeclaration(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("parameters") val parameters: Map<String, Any>
)

data class GeminiRestContent(
    @SerializedName("parts") val parts: List<GeminiRestPart>,
    @SerializedName("role") val role: String? = null
)

data class GeminiRestPart(
    @SerializedName("text") val text: String? = null,
    @SerializedName("function_call") val functionCall: GeminiFunctionCall? = null
)

data class GeminiFunctionCall(
    @SerializedName("name") val name: String,
    @SerializedName("args") val args: Map<String, Any>? = null
)

data class GeminiRestResponse(
    @SerializedName("candidates") val candidates: List<GeminiRestCandidate>?
)

data class GeminiRestCandidate(
    @SerializedName("content") val content: GeminiRestContent?
)
