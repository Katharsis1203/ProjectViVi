package com.example.visualvocab.data.datasetupload

import kotlinx.serialization.Serializable

@Serializable
data class UploadMetadata(
    val installationId: String,
    val modelVersion: String,
    val exampleCount: Int,
    val appVersionName: String,
    val appVersionCode: Long,
    val uploadedAtEpochMillis: Long
)
