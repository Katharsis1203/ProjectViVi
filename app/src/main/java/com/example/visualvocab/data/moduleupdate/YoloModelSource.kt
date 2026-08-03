package com.example.visualvocab.data.modelupdate

import java.io.File

// this defines the two places a YOLO model can come from: the app assets or downloaded files.
sealed interface YoloModelSource {

    // for models that were included in the APK when it was built.
    data class Assets(
        val modelAssetName: String,
        val metadataAssetName: String
    ) : YoloModelSource

    // for models that were downloaded after the app was installed.
    data class Files(
        val modelFile: File,
        val metadataFile: File
    ) : YoloModelSource
}
