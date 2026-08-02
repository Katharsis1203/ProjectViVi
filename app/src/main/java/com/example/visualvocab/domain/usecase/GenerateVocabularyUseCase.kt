package com.example.visualvocab.domain.usecase

import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.Vocabulary
import com.example.visualvocab.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow

class GenerateVocabularyUseCase(
    private val repository: VocabularyRepository
) {
    operator fun invoke(
        word: String,
        difficulty: SentenceDifficulty
    ): Flow<Vocabulary> {
        return repository.getVocabulary(
            word = word,
            previousSentence = null,
            difficulty = difficulty
        )
    }
}