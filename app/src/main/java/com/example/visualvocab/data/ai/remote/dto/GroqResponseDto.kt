package com.example.visualvocab.data.ai.remote.dto

import kotlinx.serialization.Serializable

// this is what comes back from groq. the app just cares about the message part.
@Serializable
data class GroqResponseDto(
    val choices: List<ChoiceDto> = emptyList()
)

@Serializable
data class ChoiceDto(
    val message: MessageDto
)
