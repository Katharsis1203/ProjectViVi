package com.example.visualvocab.data.mapper

import com.example.visualvocab.data.ai.remote.dto.VocabularyResultDto
import com.example.visualvocab.domain.model.Vocabulary

// this just turns the data we get from the AI into the format our app likes to use internally.
fun VocabularyResultDto.toDomain(): Vocabulary {
    return Vocabulary(
        englishWord = englishWord,
        spanishWord = spanishWord,
        englishSentence = englishSentence,
        spanishSentence = spanishSentence
    )
}
