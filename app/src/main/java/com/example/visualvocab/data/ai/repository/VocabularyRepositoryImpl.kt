package com.example.visualvocab.data.ai.repository

import com.example.visualvocab.data.ai.remote.GroqManager
import com.example.visualvocab.data.mapper.toDomain
import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.Vocabulary
import com.example.visualvocab.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// this repository is the bridge between the AI and the rest of our app.
class VocabularyRepositoryImpl(
    private val groqManager: GroqManager
) : VocabularyRepository {

    override fun getVocabulary(
        word: String,
        previousSentence: String?,
        difficulty: SentenceDifficulty
    ): Flow<Vocabulary> = flow {
        // we ask the manager to do the heavy lifting of talking to the AI.
        val resultDto = groqManager.generateVocabulary(
            word = word,
            previousEnglishSentence = previousSentence,
            difficulty = difficulty
        )

        // then we turn the DTO into a normal domain object.
        emit(resultDto.toDomain())
    }
}
