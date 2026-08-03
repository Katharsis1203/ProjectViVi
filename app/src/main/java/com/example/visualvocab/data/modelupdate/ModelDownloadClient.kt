package com.example.visualvocab.data.modelupdate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// this class is for downloading new AI models from the web.
class ModelDownloadClient {

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    // get the manifest file that tells us about the latest model version.
    suspend fun fetchManifest(
        manifestUrl: String
    ): ModelManifest =
        withContext(Dispatchers.IO) {
            val text =
                downloadText(
                    manifestUrl
                )

            json.decodeFromString(
                text
            )
        }

    // download the actual model file and save it to the phone.
    suspend fun downloadFile(
        sourceUrl: String,
        destination: File
    ) = withContext(Dispatchers.IO) {
        val connection =
            openConnection(
                sourceUrl
            )

        try {
            destination.parentFile
                ?.mkdirs()

            connection.inputStream
                .buffered()
                .use { input ->
                    destination
                        .outputStream()
                        .buffered()
                        .use { output ->
                            input.copyTo(output)
                        }
                }
        } finally {
            connection.disconnect()
        }
    }

    // helper to just download some text from a URL.
    private fun downloadText(
        sourceUrl: String
    ): String {
        val connection =
            openConnection(
                sourceUrl
            )

        return try {
            connection.inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }
        } finally {
            connection.disconnect()
        }
    }

    // sets up the HTTP connection with the right settings and error checking.
    private fun openConnection(
        sourceUrl: String
    ): HttpURLConnection {
        require(
            sourceUrl.startsWith(
                "https://"
            )
        ) {
            "Model update URLs must use HTTPS."
        }

        val connection =
            URL(sourceUrl)
                .openConnection() as
                HttpURLConnection

        connection.connectTimeout =
            CONNECT_TIMEOUT_MILLIS

        connection.readTimeout =
            READ_TIMEOUT_MILLIS

        connection.instanceFollowRedirects =
            true

        connection.requestMethod =
            "GET"

        connection.setRequestProperty(
            "Accept",
            "application/json, application/octet-stream"
        )

        val responseCode =
            connection.responseCode

        // if the server says no, we stop and throw an error.
        if (
            responseCode !in
            200..299
        ) {
            connection.disconnect()

            throw IllegalStateException(
                "Download failed with HTTP $responseCode."
            )
        }

        return connection
    }

    companion object {
        private const val CONNECT_TIMEOUT_MILLIS =
            15_000

        private const val READ_TIMEOUT_MILLIS =
            60_000
    }
}
