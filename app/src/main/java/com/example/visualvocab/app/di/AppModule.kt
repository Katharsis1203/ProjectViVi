package com.example.visualvocab.app.di

import android.content.Context
import com.example.visualvocab.BuildConfig
import com.example.visualvocab.core.common.AppDispatchers
import com.example.visualvocab.core.image.BitmapLoader
import com.example.visualvocab.data.ai.remote.GroqManager
import com.example.visualvocab.data.ai.repository.VocabularyRepositoryImpl
import com.example.visualvocab.data.detection.DetectionRepositoryImpl
import com.example.visualvocab.data.detection.ObjectDetectorManager
import com.example.visualvocab.domain.repository.DetectionRepository
import com.example.visualvocab.domain.repository.VocabularyRepository
import com.example.visualvocab.domain.usecase.DetectObjectsUseCase
import com.example.visualvocab.domain.usecase.GenerateVocabularyUseCase
import com.example.visualvocab.domain.usecase.RegenerateSentenceUseCase

class AppModule(private val context: Context) {

    private val dispatchers by lazy { AppDispatchers() }

    val bitmapLoader by lazy {
        BitmapLoader(context, dispatchers)
    }

    private val groqManager by lazy {
        GroqManager(BuildConfig.GROQ_API_KEY)
    }

    private val vocabularyRepository: VocabularyRepository by lazy {
        VocabularyRepositoryImpl(groqManager)
    }

    private val objectDetectorManager by lazy {
        ObjectDetectorManager(context)
    }

    private val detectionRepository: DetectionRepository by lazy {
        DetectionRepositoryImpl(context, objectDetectorManager)
    }

    val detectObjectsUseCase by lazy {
        DetectObjectsUseCase(detectionRepository)
    }

    val generateVocabularyUseCase by lazy {
        GenerateVocabularyUseCase(vocabularyRepository)
    }

    val regenerateSentenceUseCase by lazy {
        RegenerateSentenceUseCase(vocabularyRepository)
    }
}
