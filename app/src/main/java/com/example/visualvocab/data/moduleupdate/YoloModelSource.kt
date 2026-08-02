package com.example.visualvocab.data.modelupdate

import java.io.File

/**
 * Describes where a YOLO model package is loaded from.
 *
 * A model package always contains both:
 * - the .tflite model
 * - its matching classes.json file
 */
sealed interface YoloModelSource {

    data class Assets(
        val modelAssetName: String,
        val metadataAssetName: String
    ) : YoloModelSource

    data class Files(
        val modelFile: File,
        val metadataFile: File
    ) : YoloModelSource
}
