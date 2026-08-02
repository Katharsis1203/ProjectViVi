package com.example.visualvocab.data.modelupdate

import android.content.Context
import java.io.File

class ModelFileStore(
    context: Context
) {
    private val appContext =
        context.applicationContext

    val rootDirectory: File
        get() =
            File(
                appContext.filesDir,
                "yolo_models"
            )

    val activeDirectory: File
        get() =
            File(
                rootDirectory,
                "active"
            )

    val pendingDirectory: File
        get() =
            File(
                rootDirectory,
                "pending"
            )

    val previousDirectory: File
        get() =
            File(
                rootDirectory,
                "previous"
            )

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

    fun preparePendingDirectory() {
        pendingDirectory
            .deleteRecursively()

        check(
            pendingDirectory.mkdirs()
        ) {
            "Could not create pending model directory."
        }
    }

    fun clearPending() {
        pendingDirectory
            .deleteRecursively()
    }
}
