package com.example.visualvocab.data.ai.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroqRequestDto(
    val model: String,
    val messages: List<MessageDto>,
    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int,
    @SerialName("response_format") val responseFormat: ResponseFormatDto
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormatDto(
    val type: String
)
