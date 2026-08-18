package com.example.visualvocab.domain.repository

import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.Vocabulary
import kotlinx.coroutines.flow.Flow

// interface for getting translations and sentences from the AI.
interface VocabularyRepository {
    fun getVocabulary(
        word: String,
        previousSentence: String?,
        difficulty: SentenceDifficulty
    ): Flow<Vocabulary>
}
