package com.example.visualvocab.data.modelupdate

import android.content.Context
import java.io.File

/**
 * Chooses a validated downloaded model when one exists.
 *
 * Otherwise, it falls back to the model bundled in app/src/main/assets.
 *
 * Phase 1 only prepares this selection layer. Phase 2 will add installation,
 * manifests, checksums, and model downloads.
 */
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
