package com.example.visualvocab.data.datasetupload

import kotlinx.serialization.Serializable

// just a data class to hold all the extra info about an upload.
@Serializable
data class UploadMetadata(
    val installationId: String,
    val modelVersion: String,
    val exampleCount: Int,
    val appVersionName: String,
    val appVersionCode: Long,
    val uploadedAtEpochMillis: Long
)
