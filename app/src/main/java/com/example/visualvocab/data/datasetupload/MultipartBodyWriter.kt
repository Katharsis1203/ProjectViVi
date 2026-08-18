package com.example.visualvocab.data.datasetupload

import java.io.OutputStream
import java.nio.charset.StandardCharsets

// this internal class helps us format the multipart HTTP request correctly.
internal class MultipartBodyWriter(
    private val output: OutputStream,
    private val boundary: String
) {
    // writes a simple text part to the stream.
    fun writeTextPart(name: String, value: String) {
        writeAscii("--$boundary\r\n")
        writeAscii("Content-Disposition: form-data; name=\"$name\"\r\n")
        writeAscii("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
        output.write(value.toByteArray(StandardCharsets.UTF_8))
        writeAscii("\r\n")
    }

    // writes the actual ZIP file data to the stream.
    fun writeFilePart(
        name: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray
    ) {
        writeAscii("--$boundary\r\n")
        writeAscii("Content-Disposition: form-data; name=\"$name\"; filename=\"$fileName\"\r\n")
        writeAscii("Content-Type: $contentType\r\n")
        writeAscii("Content-Length: ${bytes.size}\r\n\r\n")
        output.write(bytes)
        writeAscii("\r\n")
    }

    // tells the server all data is sent.
    fun finish() {
        writeAscii("--$boundary--\r\n")
        output.flush()
    }

    // helper to write simple ASCII strings.
    private fun writeAscii(value: String) {
        output.write(value.toByteArray(StandardCharsets.UTF_8))
    }
}
