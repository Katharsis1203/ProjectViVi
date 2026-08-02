package com.example.visualvocab.data.modelupdate

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ModelInstaller(
    private val fileStore:
        ModelFileStore
) {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    fun install(
        manifest: ModelManifest
    ) {
        val pendingModel =
            fileStore.pendingModelFile

        val pendingMetadata =
            fileStore.pendingMetadataFile

        require(
            pendingModel.isFile &&
                pendingMetadata.isFile
        ) {
            "The pending model package is incomplete."
        }

        fileStore.previousDirectory
            .deleteRecursively()

        if (
            fileStore.activeDirectory
                .exists()
        ) {
            moveDirectory(
                source =
                    fileStore.activeDirectory,
                destination =
                    fileStore.previousDirectory
            )
        }

        check(
            fileStore.activeDirectory
                .mkdirs()
        ) {
            "Could not create active model directory."
        }

        pendingModel.copyTo(
            target =
                fileStore.activeModelFile,
            overwrite = true
        )

        pendingMetadata.copyTo(
            target =
                fileStore.activeMetadataFile,
            overwrite = true
        )

        fileStore.activeVersionFile
            .writeText(
                manifest.version
            )

        fileStore.activeManifestFile
            .writeText(
                json.encodeToString(
                    manifest
                )
            )

        fileStore.clearPending()
    }

    private fun moveDirectory(
        source: File,
        destination: File
    ) {
        destination
            .deleteRecursively()

        if (
            !source.renameTo(
                destination
            )
        ) {
            source.copyRecursively(
                target =
                    destination,
                overwrite = true
            )

            source.deleteRecursively()
        }
    }
}
