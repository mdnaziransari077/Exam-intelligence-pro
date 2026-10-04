package com.example.data.repository

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class GeminiModelOption(val modelId: String, val label: String, val description: String) {
    PRO_HIGH_THINKING(
        "gemini-3.1-pro-preview",
        "Gemini 3.1 Pro (High Thinking)",
        "Deep reasoning for complex CBSE math proofs, scientific deductions, and question blueprints."
    ),
    FLASH_GENERAL(
        "gemini-3.5-flash",
        "Gemini 3.5 Flash",
        "Fast, comprehensive CBSE Class 10 explanations, question drafting, and study planning."
    ),
    FLASH_LITE_FAST(
        "gemini-3.1-flash-lite-preview",
        "Gemini 3.1 Flash Lite",
        "Rapid responses for quick definitions, formula queries, and unit reviews."
    )
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelTag: String? = null,
    val isHighThinking: Boolean = false
)

enum class MessageSender {
    USER,
    ASSISTANT
}

class GeminiRepository {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val systemInstructionText = """
        You are the Exam Intelligence AI Assistant, an authoritative academic intelligence system specialized in CBSE Class 10 board examinations.
        Your expertise includes:
        - CBSE Class 10 Syllabus & Competency-Based Assessment framework.
        - Mathematics (Standard & Basic), Science (086), Social Science (087), English Language & Literature, and Hindi.
        - Previous Year Questions (PYQs), marking schemes, and step-wise mark distribution.
        - Guiding students and teachers on organizing paper analysis projects, document staging, and topic mastery.
        
        Important Guidelines:
        - Be pedagogically clear, supportive, and mathematically/scientifically rigorous.
        - When solving problems, provide step-by-step breakdown aligned with CBSE marking criteria.
        - Remind users that document automated OCR and complete historical paper analytics are staged in Phase 2 roadmap, while you are ready right now for interactive intelligent practice and query analysis.
    """.trimIndent()

    suspend fun sendChatMessage(
        history: List<ChatMessage>,
        newPrompt: String,
        selectedModel: GeminiModelOption,
        enableHighThinking: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException(
                    "Gemini API key is not configured. Please add your GEMINI_API_KEY in the Secrets panel in AI Studio."
                )
            )
        }

        val effectiveModel = if (enableHighThinking) {
            "gemini-3.1-pro-preview"
        } else {
            selectedModel.modelId
        }

        try {
            val root = JSONObject()

            // System instruction
            val sysInstructionObj = JSONObject()
            val sysParts = JSONArray().apply {
                put(JSONObject().put("text", systemInstructionText))
            }
            sysInstructionObj.put("parts", sysParts)
            root.put("systemInstruction", sysInstructionObj)

            // Contents array
            val contentsArray = JSONArray()

            // Append past turns (up to last 10 messages for context)
            val recentHistory = history.takeLast(10)
            for (msg in recentHistory) {
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                val partObj = JSONObject().put("text", msg.text)
                val turnObj = JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(partObj))
                }
                contentsArray.put(turnObj)
            }

            // Append new user turn
            val newTurn = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", newPrompt)))
            }
            contentsArray.put(newTurn)
            root.put("contents", contentsArray)

            // Generation config
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.7)

            if (enableHighThinking) {
                val thinkingConfig = JSONObject().apply {
                    put("thinkingLevel", "HIGH")
                }
                generationConfig.put("thinkingConfig", thinkingConfig)
                // Note: Do NOT set maxOutputTokens when thinking is enabled
            }
            root.put("generationConfig", generationConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMessage = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: $responseBody"
                } catch (_: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception(errorMessage))
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No response received from Gemini model."))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val replyText = buildString {
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text")
                        if (text.isNotBlank()) {
                            append(text)
                        }
                    }
                }
            }

            if (replyText.isBlank()) {
                Result.failure(Exception("Received empty text candidate from model."))
            } else {
                Result.success(replyText)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
