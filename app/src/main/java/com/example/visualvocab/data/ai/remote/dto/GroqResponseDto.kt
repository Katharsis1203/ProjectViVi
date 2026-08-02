package com.example.visualvocab.data.ai.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GroqResponseDto(
    val choices: List<ChoiceDto> = emptyList()
)

@Serializable
data class ChoiceDto(
    val message: MessageDto
)
