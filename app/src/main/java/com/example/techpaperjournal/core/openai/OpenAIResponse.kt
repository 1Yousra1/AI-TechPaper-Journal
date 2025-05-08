package com.example.techpaperjournal.core.openai

data class OpenAIResponse(
    val choices: List<OpenAIChoice>
)

data class OpenAIChoice (
    val message: Message
)