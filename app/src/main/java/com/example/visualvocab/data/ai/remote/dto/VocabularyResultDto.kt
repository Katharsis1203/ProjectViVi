package com.example.visualvocab.data.ai.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VocabularyResultDto(
    val englishWord: String = "",
    val spanishWord: String = "",
    val englishSentence: String = "",
    val spanishSentence: String = ""
)