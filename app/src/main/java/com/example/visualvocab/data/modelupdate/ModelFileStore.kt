package com.example.visualvocab.data.modelupdate

import android.content.Context
import java.io.File

// this class manages where the AI model files are stored on the phone.
class ModelFileStore(
    context: Context
) {
    private val appContext =
        context.applicationContext

    // the main folder where all our models live.
    val rootDirectory: File
        get() =
            File(
                appContext.filesDir,
                "yolo_models"
            )

    // where the model currently being used is kept.
    val activeDirectory: File
        get() =
            File(
                rootDirectory,
                "active"
            )

    // where the app puts a new model while it's still downloading.
    val pendingDirectory: File
        get() =
            File(
                rootDirectory,
                "pending"
            )

    // a backup of the previous model in case it's needed.
    val previousDirectory: File
        get() =
            File(
                rootDirectory,
                "previous"
            )

    // the files inside the pending folder.
    val pendingModelFile: File
        get() =
            File(
                pendingDirectory,
                "best.tflite"
            )

    val pendingMetadataFile: File
        get() =
            File(
                pendingDirectory,
                "classes.json"
            )

    // the files inside the active folder.
    val activeModelFile: File
        get() =
            File(
                activeDirectory,
                "best.tflite"
            )

    val activeMetadataFile: File
        get() =
            File(
                activeDirectory,
                "classes.json"
            )

    val activeVersionFile: File
        get() =
            File(
                activeDirectory,
                "version.txt"
            )

    val activeManifestFile: File
        get() =
            File(
                activeDirectory,
                "manifest.json"
            )

    // clean out the pending folder so a new download can start.
    fun preparePendingDirectory() {
        pendingDirectory
            .deleteRecursively()

        check(
            pendingDirectory.mkdirs()
        ) {
            "Could not create pending model directory."
        }
    }

    // just delete the pending files if they are not needed anymore.
    fun clearPending() {
        pendingDirectory
            .deleteRecursively()
    }
}
