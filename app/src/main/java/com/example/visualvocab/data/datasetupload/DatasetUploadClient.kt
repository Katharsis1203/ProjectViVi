package com.example.visualvocab.data.datasetupload

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class DatasetUploadClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun upload(
        uploadUrl: String,
        datasetZip: ByteArray,
        metadata: UploadMetadata
    ): DatasetUploadResult = withContext(Dispatchers.IO) {
        require(uploadUrl.startsWith("https://")) {
            "Dataset uploads must use HTTPS."
        }
        require(datasetZip.isNotEmpty()) {
            "The dataset ZIP is empty."
        }

        val boundary = "VisualVocab-${UUID.randomUUID()}"
        val connection = URL(uploadUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 120_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty(
            "Content-Type",
            "multipart/form-data; boundary=$boundary"
        )

        try {
            connection.outputStream.buffered().use { output ->
                val writer = MultipartBodyWriter(output, boundary)
                writer.writeTextPart("metadata", json.encodeToString(metadata))
                writer.writeTextPart("installationId", metadata.installationId)
                writer.writeTextPart("modelVersion", metadata.modelVersion)
                writer.writeTextPart("exampleCount", metadata.exampleCount.toString())
                writer.writeFilePart(
                    name = "dataset",
                    fileName = "visual_vocab_dataset.zip",
                    contentType = "application/zip",
                    bytes = datasetZip
                )
                writer.finish()
            }

            val responseCode = connection.responseCode
            val responseText = (
                if (responseCode in 200..299) connection.inputStream
                else connection.errorStream
            )?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                throw IllegalStateException(
                    "Dataset upload failed with HTTP $responseCode" +
                        responseText.takeIf { it.isNotBlank() }?.let { ": ${it.take(500)}" }.orEmpty()
                )
            }

            if (responseText.isBlank()) {
                return@withContext DatasetUploadResult(null, "Dataset uploaded successfully.")
            }

            runCatching {
                val obj = json.parseToJsonElement(responseText).jsonObject
                DatasetUploadResult(
                    uploadId = obj["uploadId"]?.jsonPrimitive?.content,
                    message = obj["message"]?.jsonPrimitive?.content
                        ?.takeIf { it.isNotBlank() }
                        ?: "Dataset uploaded successfully."
                )
            }.getOrElse {
                DatasetUploadResult(null, "Dataset uploaded successfully.")
            }
        } finally {
            connection.disconnect()
        }
    }
}
