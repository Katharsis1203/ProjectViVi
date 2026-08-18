package com.example.visualvocab.data.modelupdate

import kotlinx.serialization.Serializable

// this data class describes a new model version available to download.
@Serializable
data class ModelManifest(
    val version: String,
    val modelUrl: String,
    val metadataUrl: String,
    val modelSha256: String,
    val metadataSha256: String,
    val releaseNotes: String = "",
    val downloadSizeBytes: Long = 0L,
    val minimumAppVersion: Int = 1
)
