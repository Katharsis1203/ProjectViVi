package com.example.visualvocab.data.datasetupload

import android.content.Context
import com.example.visualvocab.BuildConfig
import com.example.visualvocab.data.modelupdate.ModelUpdateManager
import com.example.visualvocab.domain.repository.TrainingRepository

class DatasetUploadManager(
    context: Context,
    private val trainingRepository: TrainingRepository,
    private val modelUpdateManager: ModelUpdateManager,
    private val uploadClient: DatasetUploadClient,
    private val installationIdProvider: InstallationIdProvider,
    private val uploadUrl: String = DatasetUploadConfig.UPLOAD_URL
) {
    private val appContext = context.applicationContext

    suspend fun uploadDataset(): DatasetUploadResult {
        val exampleCount = trainingRepository.getExampleCount()
        require(exampleCount > 0) {
            "No saved training examples are available to upload."
        }

        val datasetZip = trainingRepository.exportDatasetToByteArray()
        val metadata = UploadMetadata(
            installationId = installationIdProvider.getInstallationId(),
            modelVersion = modelUpdateManager.getCurrentVersion(),
            exampleCount = exampleCount,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE.toLong(),
            uploadedAtEpochMillis = System.currentTimeMillis()
        )

        return uploadClient.upload(uploadUrl, datasetZip, metadata)
    }
}
