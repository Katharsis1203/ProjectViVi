package com.example.visualvocab.data.modelupdate

// this manager coordinates checking for, downloading, and installing model updates.
class ModelUpdateManager(
    private val modelProvider:
        YoloModelProvider,
    private val fileStore:
        ModelFileStore,
    private val downloadClient:
        ModelDownloadClient,
    private val validator:
        ModelPackageValidator,
    private val installer:
        ModelInstaller,
    private val manifestUrl: String =
        ModelUpdateConfig.MANIFEST_URL
) {

    fun getCurrentVersion(): String {
        return modelProvider
            .getActiveVersion()
    }

    // check the web for a new manifest and compare it to our current version.
    suspend fun checkForUpdate():
        ModelUpdateInfo {

        val manifest =
            downloadClient
                .fetchManifest(
                    manifestUrl
                )

        return ModelUpdateInfo(
            manifest = manifest,
            currentVersion =
                getCurrentVersion()
        )
    }

    // download the new model files and install them if they are good.
    suspend fun downloadAndInstall(
        manifest: ModelManifest
    ) {
        fileStore
            .preparePendingDirectory()

        try {
            // download both the model and the labels.
            downloadClient
                .downloadFile(
                    sourceUrl =
                        manifest.modelUrl,
                    destination =
                        fileStore
                            .pendingModelFile
                )

            downloadClient
                .downloadFile(
                    sourceUrl =
                        manifest.metadataUrl,
                    destination =
                        fileStore
                            .pendingMetadataFile
                )

            // make sure everything is okay before swapping it out.
            validator.validate(
                manifest = manifest,
                modelFile =
                    fileStore
                        .pendingModelFile,
                metadataFile =
                    fileStore
                        .pendingMetadataFile
            )

            // actually install it.
            installer.install(
                manifest
            )
        } catch (
            exception: Exception
        ) {
            // if anything failed, clean up the mess.
            fileStore.clearPending()
            throw exception
        }
    }
}
