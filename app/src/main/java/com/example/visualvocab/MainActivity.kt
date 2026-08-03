package com.example.visualvocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visualvocab.app.VisualVocabApp
import com.example.visualvocab.data.training.TrainingExportManager
import com.example.visualvocab.data.training.TrainingRepositoryImpl
import com.example.visualvocab.data.modelupdate.DefaultYoloModelProvider
import com.example.visualvocab.data.modelupdate.ModelDownloadClient
import com.example.visualvocab.data.modelupdate.ModelFileStore
import com.example.visualvocab.data.modelupdate.ModelInstaller
import com.example.visualvocab.data.modelupdate.ModelPackageValidator
import com.example.visualvocab.data.modelupdate.ModelUpdateManager
import com.example.visualvocab.data.datasetupload.DatasetUploadClient
import com.example.visualvocab.data.datasetupload.DatasetUploadManager
import com.example.visualvocab.data.datasetupload.InstallationIdProvider
import com.example.visualvocab.data.progress.PlayerProgressStore
import com.example.visualvocab.feature.vocab.presentation.VocabViewModel
import com.example.visualvocab.feature.vocab.ui.VisualVocabScreen
import com.example.visualvocab.ui.theme.VisualVocabTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VisualVocabTheme {
                val app = application as VisualVocabApp
                val appModule = app.appModule

                val trainingRepository = remember(app) {
                    TrainingRepositoryImpl(app.applicationContext)
                }

                val trainingExportManager = remember(app, trainingRepository) {
                    TrainingExportManager(
                        context = app.applicationContext,
                        repository = trainingRepository
                    )
                }

                // Model update logic is retained for background compatibility but removed from UI flow
                val modelUpdateManager = remember(app) {
                    val provider = DefaultYoloModelProvider(app.applicationContext)
                    val fileStore = ModelFileStore(app.applicationContext)
                    ModelUpdateManager(
                        modelProvider = provider,
                        fileStore = fileStore,
                        downloadClient = ModelDownloadClient(),
                        validator = ModelPackageValidator(app.applicationContext),
                        installer = ModelInstaller(fileStore)
                    )
                }

                val datasetUploadManager = remember(app, trainingRepository, modelUpdateManager) {
                    DatasetUploadManager(
                        context = app.applicationContext,
                        trainingRepository = trainingRepository,
                        modelUpdateManager = modelUpdateManager,
                        uploadClient = DatasetUploadClient(),
                        installationIdProvider = InstallationIdProvider(app.applicationContext)
                    )
                }

                val playerProgressStore = remember(app) {
                    PlayerProgressStore(app.applicationContext)
                }

                val viewModel: VocabViewModel = viewModel {
                    VocabViewModel(
                        bitmapLoader = appModule.bitmapLoader,
                        detectObjectsUseCase = appModule.detectObjectsUseCase,
                        generateVocabularyUseCase = appModule.generateVocabularyUseCase,
                        regenerateSentenceUseCase = appModule.regenerateSentenceUseCase,
                        trainingRepository = trainingRepository,
                        trainingExportManager = trainingExportManager,
                        datasetUploadManager = datasetUploadManager,
                        playerProgressStore = playerProgressStore
                    )
                }

                VisualVocabScreen(viewModel = viewModel)
            }
        }
    }
}
