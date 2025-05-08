package com.example.techpaperjournal.core.openai

import com.example.techpaperjournal.BuildConfig
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

private const val OPENAI_API_KEY = BuildConfig.API_KEY

interface OpenAIService {
    @Headers(
        "Content-Type: application/json",
        "Authorization: Bearer $OPENAI_API_KEY"
    )
    @POST("chat/completions")
    suspend fun generateSummary(@Body request: OpenAIRequest): OpenAIResponse
}