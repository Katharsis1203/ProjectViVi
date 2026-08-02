package com.example.visualvocab.data.modelupdate

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

    suspend fun downloadAndInstall(
        manifest: ModelManifest
    ) {
        fileStore
            .preparePendingDirectory()

        try {
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

            validator.validate(
                manifest = manifest,
                modelFile =
                    fileStore
                        .pendingModelFile,
                metadataFile =
                    fileStore
                        .pendingMetadataFile
            )

            installer.install(
                manifest
            )
        } catch (
            exception: Exception
        ) {
            fileStore.clearPending()
            throw exception
        }
    }
}
