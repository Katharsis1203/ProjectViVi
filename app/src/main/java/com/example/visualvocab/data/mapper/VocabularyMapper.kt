package com.example.visualvocab.data.mapper

import com.example.visualvocab.data.ai.remote.dto.VocabularyResultDto
import com.example.visualvocab.domain.model.Vocabulary

fun VocabularyResultDto.toDomain(): Vocabulary {
    return Vocabulary(
        englishWord = englishWord,
        spanishWord = spanishWord,
        englishSentence = englishSentence,
        spanishSentence = spanishSentence
    )
}