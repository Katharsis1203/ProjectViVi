package com.example.visualvocab.domain.usecase

import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.Vocabulary
import com.example.visualvocab.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow

// this usecase is for getting the first set of vocabulary for a word.
class GenerateVocabularyUseCase(
    private val repository: VocabularyRepository
) {
    operator fun invoke(
        word: String,
        difficulty: SentenceDifficulty
    ): Flow<Vocabulary> {
        // we just call the repository without any previous sentence.
        return repository.getVocabulary(
            word = word,
            previousSentence = null,
            difficulty = difficulty
        )
    }
}
