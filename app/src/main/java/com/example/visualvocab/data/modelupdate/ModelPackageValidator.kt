package com.example.visualvocab.data.modelupdate

import android.content.Context
import com.example.visualvocab.data.detection.yolo.YoloModelMetadata
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class ModelPackageValidator(
    context: Context
) {
    private val appContext =
        context.applicationContext

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
