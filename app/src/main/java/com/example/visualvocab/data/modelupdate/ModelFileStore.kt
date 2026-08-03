package com.example.visualvocab.data.modelupdate

import android.content.Context
import java.io.File

// this class manages where we store our AI model files on the phone's storage.
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

    // where we put a new model while it's still downloading.
    val pendingDirectory: File
        get() =
            File(
                rootDirectory,
                "pending"
            )

    // a backup of the previous model in case we need it.
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

    // clean out the pending folder so we can start a new download.
    fun preparePendingDirectory() {
        pendingDirectory
            .deleteRecursively()

        check(
            pendingDirectory.mkdirs()
        ) {
            "Could not create pending model directory."
        }
    }

    // just delete the pending files if we don't need them anymore.
    fun clearPending() {
        pendingDirectory
            .deleteRecursively()
    }
}
