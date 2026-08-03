package com.example.visualvocab.data.modelupdate

import android.content.Context
import java.io.File

// this class helps the app choose which YOLO model to use.
// it checks if we have a newer downloaded one, otherwise it just uses the one that came with the app.
class DefaultYoloModelProvider(
    context: Context,
    private val bundledModelAssetName: String =
        DEFAULT_BUNDLED_MODEL,
    private val bundledMetadataAssetName: String =
        DEFAULT_BUNDLED_METADATA,
    private val bundledVersion: String =
        DEFAULT_BUNDLED_VERSION
) : YoloModelProvider {

    private val appContext =
        context.applicationContext

    // where downloaded models are kept.
    private val activeDirectory: File
        get() =
            File(
                appContext.filesDir,
                ACTIVE_DIRECTORY_PATH
            )

    private val downloadedModelFile: File
        get() =
            File(
                activeDirectory,
                MODEL_FILE_NAME
            )

    private val downloadedMetadataFile: File
        get() =
            File(
                activeDirectory,
                METADATA_FILE_NAME
            )

    private val downloadedVersionFile: File
        get() =
            File(
                activeDirectory,
                VERSION_FILE_NAME
            )

    // tells us if we should load the model from the phone's files or from the app's assets.
    override fun getActiveSource():
        YoloModelSource {

        return if (
            downloadedModelFile.isFile &&
            downloadedMetadataFile.isFile
        ) {
            YoloModelSource.Files(
                modelFile =
                    downloadedModelFile,
                metadataFile =
                    downloadedMetadataFile
            )
        } else {
            YoloModelSource.Assets(
                modelAssetName =
                    bundledModelAssetName,
                metadataAssetName =
                    bundledMetadataAssetName
            )
        }
    }

    // returns the version string of the model currently in use.
    override fun getActiveVersion(): String {
        return if (
            downloadedModelFile.isFile &&
            downloadedMetadataFile.isFile &&
            downloadedVersionFile.isFile
        ) {
            downloadedVersionFile
                .readText()
                .trim()
                .ifBlank {
                    UNKNOWN_DOWNLOADED_VERSION
                }
        } else {
            bundledVersion
        }
    }

    companion object {
        private const val DEFAULT_BUNDLED_MODEL =
            "best.tflite"

        private const val DEFAULT_BUNDLED_METADATA =
            "classes.json"

        private const val DEFAULT_BUNDLED_VERSION =
            "bundled-1.0.0"

        private const val ACTIVE_DIRECTORY_PATH =
            "yolo_models/active"

        private const val MODEL_FILE_NAME =
            "best.tflite"

        private const val METADATA_FILE_NAME =
            "classes.json"

        private const val VERSION_FILE_NAME =
            "version.txt"

        private const val UNKNOWN_DOWNLOADED_VERSION =
            "downloaded-unknown"
    }
}
