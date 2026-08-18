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

// this class is for uploading our dataset zip files to the server.
class DatasetUploadClient {
    // this turns the metadata into JSON strings.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // this function does the actual work of sending the file over the internet.
    suspend fun upload(
        uploadUrl: String,
        datasetZip: ByteArray,
        metadata: UploadMetadata
    ): DatasetUploadResult = withContext(Dispatchers.IO) {
        // only HTTPS is allowed because it's safer.
        require(uploadUrl.startsWith("https://")) {
            "Dataset uploads must use HTTPS."
        }
        // can't upload nothing, right?
        require(datasetZip.isNotEmpty()) {
            "The dataset ZIP is empty."
        }

        // a unique boundary for the multipart request is needed so the server knows where things end.
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
            // here the metadata and the actual ZIP file go into the request.
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

            // check if the server liked what was sent.
            val responseCode = connection.responseCode
            val responseText = (
                if (responseCode in 200..299) connection.inputStream
                else connection.errorStream
            )?.bufferedReader()?.use { it.readText() }.orEmpty()

            // if it's not a 200-range code, something went wrong.
            if (responseCode !in 200..299) {
                throw IllegalStateException(
                    "Dataset upload failed with HTTP $responseCode" +
                        responseText.takeIf { it.isNotBlank() }?.let { ": ${it.take(500)}" }.orEmpty()
                )
            }

            if (responseText.isBlank()) {
                return@withContext DatasetUploadResult(null, "Dataset uploaded successfully.")
            }

            // try to read what the server said back to us.
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
            // always clean up the connection.
            connection.disconnect()
        }
    }
}
