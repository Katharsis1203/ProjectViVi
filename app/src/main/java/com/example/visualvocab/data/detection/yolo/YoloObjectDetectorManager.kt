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

// this manager handles running the YOLO model using the TensorFlow Lite interpreter.
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
    // i'm using 0.45 for the nms threshold because the yolov8 docs say that's 
    // the best balance for speed on mobile. tried 0.5 but it kept double-detecting 
    // the same coffee mug.

    private val appContext =
        context.applicationContext

    // figure out if the built-in model or a new downloaded one is active.
    private val activeSource =
        modelProvider.getActiveSource()

    // load the list of labels for this model.
    private val metadata =
        YoloModelMetadata.load(
            appContext,
            activeSource
        )

    // the actual engine that runs the model.
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

    // get info about what the model expects as input.
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

    // this gets the image ready for the model.
    private val preprocessor =
        YoloImagePreprocessor(
            inputSize = inputSize,
            dataLayout = dataLayout
        )

    // this turns the model's output back into a list of things it found.
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
        // make sure the model is actually compatible with expectations.
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

    // this is the main function to find objects in a picture.
    @Synchronized
    fun detect(
        bitmap: Bitmap
    ): List<DetectionResult> {
        val prepared =
            preprocessor.process(bitmap)

        // allocate space for the model's results.
        val output =
            Array(outputShape[0]) {
                Array(outputShape[1]) {
                    FloatArray(
                        outputShape[2]
                    )
                }
            }

        // actually run the model.
        // i spent way too long trying to get the GPU delegate working here but 
        // it kept crashing on my older phone, so sticking with CPU for now.
        interpreter.run(
            prepared.buffer,
            output
        )

        // decode the raw numbers into a nice list of results.
        return decoder.decode(
            output = output,
            tensorShape = outputShape,
            preprocessing = prepared
        )
    }

    // just a helper to see which model is active.
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

    // clean up when finished.
    override fun close() {
        interpreter.close()
    }

    private data class InputInfo(
        val size: Int,
        val layout:
        YoloImagePreprocessor
        .DataLayout
    )

    // check if the model's input shape makes sense to us.
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

        // figure out if it wants colors first or pixel positions first.
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

    // helper to map the model file into memory so it runs faster.
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
