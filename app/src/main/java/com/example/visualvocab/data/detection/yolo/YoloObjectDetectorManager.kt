package com.example.visualvocab.data.detection.yolo

import android.content.Context
import android.graphics.Bitmap
import com.example.visualvocab.data.modelupdate.DefaultYoloModelProvider
import com.example.visualvocab.data.modelupdate.YoloModelProvider
import com.example.visualvocab.data.modelupdate.YoloModelSource
import com.example.visualvocab.domain.model.DetectionResult
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class YoloObjectDetectorManager(
    context: Context,
    private val modelProvider:
    YoloModelProvider =
        DefaultYoloModelProvider(context),
    confidenceThreshold: Float =
        DEFAULT_CONFIDENCE_THRESHOLD,
    nmsThreshold: Float =
        DEFAULT_NMS_THRESHOLD
) : Closeable {

    private val appContext =
        context.applicationContext

    private val activeSource =
        modelProvider.getActiveSource()

    private val metadata =
        YoloModelMetadata.load(
            appContext,
            activeSource
        )

    private val interpreter =
        Interpreter(
            loadMappedModel(
                context = appContext,
                source = activeSource
            ),
            Interpreter.Options()
                .setNumThreads(
                    DEFAULT_THREAD_COUNT
                )
        )

    private val inputTensor =
        interpreter.getInputTensor(0)

    private val outputTensor =
        interpreter.getOutputTensor(0)

    private val inputInfo =
        resolveInputInfo(
            tensor = inputTensor,
            metadataInputSize =
                metadata.input_size
        )

    private val inputSize =
        inputInfo.size

    private val dataLayout =
        inputInfo.layout

    private val outputShape =
        outputTensor.shape()

    private val preprocessor =
        YoloImagePreprocessor(
            inputSize = inputSize,
            dataLayout = dataLayout
        )

    private val decoder =
        YoloOutputDecoder(
            classNames =
                metadata.classNames,
            confidenceThreshold =
                confidenceThreshold,
            nmsThreshold =
                nmsThreshold
        )

    init {
        require(
            inputTensor.dataType()
                .toString() ==
                    "FLOAT32"
        ) {
            "The current YOLO integration expects a FLOAT32 input tensor, but the model uses ${inputTensor.dataType()}."
        }

        require(
            outputTensor.dataType()
                .toString() ==
                    "FLOAT32"
        ) {
            "The current YOLO integration expects a FLOAT32 output tensor, but the model uses ${outputTensor.dataType()}."
        }

        require(
            outputShape.size == 3 &&
                    outputShape[0] == 1
        ) {
            "Unsupported YOLO output shape: ${outputShape.contentToString()}."
        }

        val minimumOutputChannels =
            BOX_CHANNEL_COUNT +
                    metadata.classNames.size

        val outputSupportsClasses =
            outputShape[1] >=
                    minimumOutputChannels ||
                    outputShape[2] >=
                    minimumOutputChannels

        require(outputSupportsClasses) {
            "The YOLO model output ${outputShape.contentToString()} does not contain enough channels for ${metadata.classNames.size} classes."
        }
    }

    @Synchronized
    fun detect(
        bitmap: Bitmap
    ): List<DetectionResult> {
        val prepared =
            preprocessor.process(bitmap)

        val output =
            Array(outputShape[0]) {
                Array(outputShape[1]) {
                    FloatArray(
                        outputShape[2]
                    )
                }
            }

        interpreter.run(
            prepared.buffer,
            output
        )

        return decoder.decode(
            output = output,
            tensorShape = outputShape,
            preprocessing = prepared
        )
    }

    fun describeModel(): String {
        return buildString {
            append("version=")
            append(
                modelProvider
                    .getActiveVersion()
            )
            append(", source=")
            append(
                when (activeSource) {
                    is YoloModelSource.Assets ->
                        "assets"

                    is YoloModelSource.Files ->
                        "downloaded-files"
                }
            )
            append(", input=")
            append(
                inputTensor.shape()
                    .contentToString()
            )
            append(", output=")
            append(
                outputShape
                    .contentToString()
            )
            append(", classes=")
            append(
                metadata.classNames
            )
        }
    }

    override fun close() {
        interpreter.close()
    }

    private data class InputInfo(
        val size: Int,
        val layout:
        YoloImagePreprocessor
        .DataLayout
    )

    private fun resolveInputInfo(
        tensor: Tensor,
        metadataInputSize: Int
    ): InputInfo {
        val shape =
            tensor.shape()

        require(
            shape.size == 4 &&
                    shape[0] == 1
        ) {
            "Unexpected YOLO input shape: ${shape.contentToString()}."
        }

        val layout =
            when {
                shape[3] == 3 ->
                    YoloImagePreprocessor
                        .DataLayout
                        .NHWC

                shape[1] == 3 ->
                    YoloImagePreprocessor
                        .DataLayout
                        .NCHW

                else ->
                    throw IllegalArgumentException(
                        "Unsupported YOLO channel configuration: ${shape.contentToString()}."
                    )
            }

        val size =
            if (
                layout ==
                YoloImagePreprocessor
                    .DataLayout
                    .NHWC
            ) {
                shape[1]
            } else {
                shape[2]
            }

        val isSquare =
            if (
                layout ==
                YoloImagePreprocessor
                    .DataLayout
                    .NHWC
            ) {
                shape[1] == shape[2]
            } else {
                shape[2] == shape[3]
            }

        require(isSquare) {
            "Only square YOLO inputs are currently supported."
        }

        if (
            metadataInputSize > 0 &&
            metadataInputSize != size
        ) {
            throw IllegalStateException(
                "classes.json declares input size $metadataInputSize, but the model expects $size."
            )
        }

        return InputInfo(
            size = size,
            layout = layout
        )
    }

    private fun loadMappedModel(
        context: Context,
        source: YoloModelSource
    ): MappedByteBuffer {
        return when (source) {
            is YoloModelSource.Assets ->
                loadMappedAssetModel(
                    context = context,
                    assetName =
                        source.modelAssetName
                )

            is YoloModelSource.Files ->
                loadMappedFileModel(
                    source.modelFile
                )
        }
    }

    private fun loadMappedAssetModel(
        context: Context,
        assetName: String
    ): MappedByteBuffer {
        val descriptor =
            context.assets.openFd(
                assetName
            )

        FileInputStream(
            descriptor.fileDescriptor
        ).use { stream ->
            return stream.channel.map(
                FileChannel.MapMode
                    .READ_ONLY,
                descriptor.startOffset,
                descriptor.declaredLength
            )
        }
    }

    private fun loadMappedFileModel(
        modelFile: File
    ): MappedByteBuffer {
        require(modelFile.isFile) {
            "Downloaded YOLO model does not exist: ${modelFile.absolutePath}"
        }

        FileInputStream(
            modelFile
        ).use { stream ->
            return stream.channel.map(
                FileChannel.MapMode
                    .READ_ONLY,
                0L,
                modelFile.length()
            )
        }
    }

    companion object {
        private const val DEFAULT_THREAD_COUNT =
            4

        private const val DEFAULT_CONFIDENCE_THRESHOLD =
            0.30f

        private const val DEFAULT_NMS_THRESHOLD =
            0.45f

        private const val BOX_CHANNEL_COUNT =
            4
    }
}