package com.example.techpaperjournal.data.openai

import com.google.gson.annotations.SerializedName

data class OpenAIRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<Message>,
    val temperature: Double,
    @SerializedName("max_tokens")
    val maxTokens: Int,
)

data class Message(
    val role: String,
    val content: String
)
