package com.example.visualvocab.data.ai.repository

import com.example.visualvocab.data.ai.remote.GroqManager
import com.example.visualvocab.data.mapper.toDomain
import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.Vocabulary
import com.example.visualvocab.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class VocabularyRepositoryImpl(
    private val groqManager: GroqManager
) : VocabularyRepository {

    override fun getVocabulary(
        word: String,
        previousSentence: String?,
        difficulty: SentenceDifficulty
    ): Flow<Vocabulary> = flow {
        val resultDto = groqManager.generateVocabulary(
            word = word,
            previousEnglishSentence = previousSentence,
            difficulty = difficulty
        )

        emit(resultDto.toDomain())
    }
}