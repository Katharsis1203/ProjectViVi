package com.example.visualvocab.data.ai.repository

import com.example.visualvocab.data.ai.remote.GroqManager
import com.example.visualvocab.data.ai.remote.dto.VocabularyResultDto
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
        // asking the manager to do the heavy lifting of talking to the AI.
        val resultDto = groqManager.generateVocabulary(
            word = word,
            previousEnglishSentence = previousSentence,
            difficulty = difficulty
        )

        // then the DTO turns into a normal domain object.
        // i just put the mapping code here instead of having a separate file for it 
        // because it was getting way too cluttered with tiny files.
        emit(resultDto.toDomain())
    }
}

// this just turns the data from the AI into the format the app likes to use internally.
private fun VocabularyResultDto.toDomain(): Vocabulary {
    return Vocabulary(
        englishWord = englishWord,
        spanishWord = spanishWord,
        englishSentence = englishSentence,
        spanishSentence = spanishSentence
    )
}
