package com.example.visualvocab.data.modelupdate

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// this class takes care of moving a newly downloaded model into the active folder.
class ModelInstaller(
    private val fileStore:
        ModelFileStore
) {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    // move the pending model to be the active one.
    fun install(
        manifest: ModelManifest
    ) {
        val pendingModel =
            fileStore.pendingModelFile

        val pendingMetadata =
            fileStore.pendingMetadataFile

        // make sure both the model and its info file are actually there.
        require(
            pendingModel.isFile &&
                pendingMetadata.isFile
        ) {
            "The pending model package is incomplete."
        }

        // remove the old backup and move the current model to the previous folder.
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

        // create the folder for the new model and copy the files over.
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

        // save the version and manifest so the app knows what's installed.
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

        // clean up the temporary download folder.
        fileStore.clearPending()
    }

    // helper to move a whole folder by renaming it or copying if rename fails.
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
