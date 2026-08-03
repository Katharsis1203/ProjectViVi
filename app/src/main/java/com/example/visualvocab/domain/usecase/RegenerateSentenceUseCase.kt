package com.example.visualvocab.domain.usecase

import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.Vocabulary
import com.example.visualvocab.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow

// this usecase lets the user get a different sentence if they don't like the first one.
class RegenerateSentenceUseCase(
    private val repository: VocabularyRepository
) {
    operator fun invoke(
        word: String,
        previousSentence: String,
        difficulty: SentenceDifficulty
    ): Flow<Vocabulary> {
        // we pass the old sentence so the AI knows to give us a new one.
        return repository.getVocabulary(
            word = word,
            previousSentence = previousSentence,
            difficulty = difficulty
        )
    }
}
