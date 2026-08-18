package com.example.visualvocab.data.modelupdate

import android.content.Context
import com.example.visualvocab.data.detection.yolo.YoloModelMetadata
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

// this class checks if a downloaded model is actually what it claims to be and works correctly.
class ModelPackageValidator(
    context: Context
) {
    private val appContext =
        context.applicationContext

    // run several checks on the model and metadata files.
    fun validate(
        manifest: ModelManifest,
        modelFile: File,
        metadataFile: File
    ) {
        require(modelFile.isFile) {
            "Downloaded model file is missing."
        }

        require(metadataFile.isFile) {
            "Downloaded classes file is missing."
        }

        // check the hash to make sure the file didn't get corrupted during download.
        require(
            sha256(modelFile)
                .equals(
                    manifest.modelSha256,
                    ignoreCase = true
                )
        ) {
            "The model checksum does not match the manifest."
        }

        require(
            sha256(metadataFile)
                .equals(
                    manifest.metadataSha256,
                    ignoreCase = true
                )
        ) {
            "The classes checksum does not match the manifest."
        }

        val source =
            YoloModelSource.Files(
                modelFile = modelFile,
                metadataFile = metadataFile
            )

        // try loading the labels to see if they are valid.
        val metadata =
            YoloModelMetadata.load(
                appContext,
                source
            )

        require(
            metadata.classNames
                .isNotEmpty()
        ) {
            "The downloaded model contains no class labels."
        }

        // try opening the model with the interpreter to see if it's a real TFLite file.
        Interpreter(
            modelFile,
            Interpreter.Options()
                .setNumThreads(1)
        ).use { interpreter ->
            val inputShape =
                interpreter
                    .getInputTensor(0)
                    .shape()

            val outputShape =
                interpreter
                    .getOutputTensor(0)
                    .shape()

            // make sure the input and output shapes match expectations.
            require(
                inputShape.size == 4 &&
                    inputShape[0] == 1
            ) {
                "Unsupported downloaded model input shape: ${inputShape.contentToString()}."
            }

            require(
                outputShape.size == 3 &&
                    outputShape[0] == 1
            ) {
                "Unsupported downloaded model output shape: ${outputShape.contentToString()}."
            }

            val modelInputSize =
                when {
                    inputShape[1] == 3 ->
                        inputShape[2]

                    inputShape[3] == 3 ->
                        inputShape[1]

                    else ->
                        throw IllegalStateException(
                            "Unsupported downloaded model channel layout: ${inputShape.contentToString()}."
                        )
                }

            require(
                metadata.input_size ==
                    modelInputSize
            ) {
                "classes.json declares ${metadata.input_size}, but the model expects $modelInputSize."
            }

            val requiredChannels =
                4 +
                    metadata.classNames.size

            require(
                outputShape[1] >=
                    requiredChannels ||
                    outputShape[2] >=
                    requiredChannels
            ) {
                "The downloaded model output does not match its class list."
            }
        }
    }

    // calculate the SHA-256 hash of a file.
    private fun sha256(
        file: File
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        FileInputStream(file)
            .use { input ->
                val buffer =
                    ByteArray(8192)

                while (true) {
                    val count =
                        input.read(buffer)

                    if (count <= 0) {
                        break
                    }

                    digest.update(
                        buffer,
                        0,
                        count
                    )
                }
            }

        return digest
            .digest()
            .joinToString("") {
                "%02x".format(it)
            }
    }
}
