package com.example.techpaperjournal.data.openai

data class OpenAIResponse(
    val choices: List<OpenAIChoice>
)

data class OpenAIChoice (
    val message: Message
)