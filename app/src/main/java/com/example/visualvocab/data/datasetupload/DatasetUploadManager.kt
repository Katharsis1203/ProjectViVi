package com.example.visualvocab.data.datasetupload

import android.content.Context
import com.example.visualvocab.BuildConfig
import com.example.visualvocab.data.modelupdate.ModelUpdateManager
import com.example.visualvocab.domain.repository.TrainingRepository
import java.io.ByteArrayOutputStream

// this manager handles the whole process of getting the data ready and sending it off.
class DatasetUploadManager(
    context: Context,
    private val trainingRepository: TrainingRepository,
    private val modelUpdateManager: ModelUpdateManager,
    private val uploadClient: DatasetUploadClient,
    private val installationIdProvider: InstallationIdProvider,
    private val uploadUrl: String = DatasetUploadConfig.UPLOAD_URL
) {
    private val appContext = context.applicationContext

    // this is the main function you call to upload everything.
    suspend fun uploadDataset(): DatasetUploadResult {
        // first see how many examples actually exist.
        val exampleCount = trainingRepository.getExampleCount()
        require(exampleCount > 0) {
            "No saved training examples are available to upload."
        }

        // turn the dataset into a ZIP file in memory.
        val datasetZip = ByteArrayOutputStream().use { output ->
            trainingRepository.exportDataset(output)
            output.toByteArray()
        }
        
        // collect all the extra info the server needs.
        val metadata = UploadMetadata(
            installationId = installationIdProvider.getInstallationId(),
            modelVersion = modelUpdateManager.getCurrentVersion(),
            exampleCount = exampleCount,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE.toLong(),
            uploadedAtEpochMillis = System.currentTimeMillis()
        )

        // finally, send it away.
        return uploadClient.upload(uploadUrl, datasetZip, metadata)
    }
}
