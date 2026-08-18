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

// this class is like a big container for all the things the app needs to work.
// it makes sure the app doesn't have to keep creating new stuff all the time.
class AppModule(private val context: Context) {

    private val dispatchers by lazy { AppDispatchers() }

    // this one helps us load pictures from the phone's memory.
    val bitmapLoader by lazy {
        BitmapLoader(context, dispatchers)
    }

    // groq is the smart AI thing for translating words.
    private val groqManager by lazy {
        GroqManager(BuildConfig.GROQ_API_KEY)
    }

    // this manages how the app talks to the AI to get new words.
    private val vocabularyRepository: VocabularyRepository by lazy {
        VocabularyRepositoryImpl(groqManager)
    }

    // this is for the camera and finding objects in photos.
    private val objectDetectorManager by lazy {
        ObjectDetectorManager(context)
    }

    // repo that puts together the detection stuff for us to use.
    private val detectionRepository: DetectionRepository by lazy {
        DetectionRepositoryImpl(context, objectDetectorManager)
    }

    // these usecases are the actual actions the UI can do.
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
