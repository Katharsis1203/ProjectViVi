package com.example.visualvocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visualvocab.app.VisualVocabApp
import com.example.visualvocab.data.training.TrainingExportManager
import com.example.visualvocab.data.training.TrainingRepositoryImpl
import com.example.visualvocab.feature.vocab.presentation.VocabViewModel
import com.example.visualvocab.feature.vocab.ui.VisualVocabScreen
import com.example.visualvocab.ui.theme.VisualVocabTheme

class MainActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContent {
            VisualVocabTheme {
                val app =
                    application as
                            VisualVocabApp

                val appModule =
                    app.appModule

                val trainingRepository =
                    remember(app) {
                        TrainingRepositoryImpl(
                            app.applicationContext
                        )
                    }

                val trainingExportManager =
                    remember(
                        app,
                        trainingRepository
                    ) {
                        TrainingExportManager(
                            context =
                                app.applicationContext,
                            repository =
                                trainingRepository
                        )
                    }

                val viewModel:
                        VocabViewModel =
                    viewModel {
                        VocabViewModel(
                            bitmapLoader =
                                appModule
                                    .bitmapLoader,
                            detectObjectsUseCase =
                                appModule
                                    .detectObjectsUseCase,
                            generateVocabularyUseCase =
                                appModule
                                    .generateVocabularyUseCase,
                            regenerateSentenceUseCase =
                                appModule
                                    .regenerateSentenceUseCase,
                            trainingRepository =
                                trainingRepository,
                            trainingExportManager =
                                trainingExportManager
                        )
                    }

                VisualVocabScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}