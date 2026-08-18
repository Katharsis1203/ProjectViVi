package com.example.visualvocab.data.ai.remote.dto

import kotlinx.serialization.Serializable

// this is the actual vocabulary data the AI gives us, like the words and sentences.
@Serializable
data class VocabularyResultDto(
    val englishWord: String = "",
    val spanishWord: String = "",
    val englishSentence: String = "",
    val spanishSentence: String = ""
)
