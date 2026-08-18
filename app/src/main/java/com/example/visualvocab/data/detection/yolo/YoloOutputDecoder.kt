package com.example.visualvocab.data.detection.yolo

import android.graphics.RectF
import com.example.visualvocab.domain.model.DetectionResult
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// this class turns the raw numbers from the YOLO model into something understandable.
internal class YoloOutputDecoder(
    private val classNames:
        List<String>,
    private val confidenceThreshold:
        Float,
    private val nmsThreshold: Float
) {

    // this is the main decoding function.
    fun decode(
        output: Array<Array<FloatArray>>,
        tensorShape: IntArray,
        preprocessing:
            YoloImagePreprocessor.Result
    ): List<DetectionResult> {
        require(
            tensorShape.size == 3 &&
                tensorShape[0] == 1
        ) {
            "Expected a three-dimensional YOLO output tensor, but received ${tensorShape.contentToString()}."
        }

        val firstDimension =
            tensorShape[1]

        val secondDimension =
            tensorShape[2]

        val minimumChannels =
            BOX_CHANNELS +
                classNames.size

        // figure out how the model packed its output data.
        val channelFirst =
            when {
                firstDimension ==
                    minimumChannels -> true

                secondDimension ==
                    minimumChannels -> false

                firstDimension <
                    secondDimension -> true

                else -> false
            }

        val channels =
            if (channelFirst) {
                firstDimension
            } else {
                secondDimension
            }

        val candidateCount =
            if (channelFirst) {
                secondDimension
            } else {
                firstDimension
            }

        require(
            channels >= minimumChannels
        ) {
            "YOLO output has $channels channels, but at least $minimumChannels are required for ${classNames.size} classes."
        }

        val candidates =
            ArrayList<DetectionResult>(
                candidateCount
            )

        // go through each thing the model thinks it found.
        for (
            candidateIndex in
            0 until candidateCount
        ) {
            fun value(
                channelIndex: Int
            ): Float {
                return if (channelFirst) {
                    output[0][channelIndex][candidateIndex]
                } else {
                    output[0][candidateIndex][channelIndex]
                }
            }

            // read the box coordinates.
            val centerX =
                value(0)

            val centerY =
                value(1)

            val width =
                value(2)

            val height =
                value(3)

            var bestClassIndex = -1
            var bestScore = 0f

            // find out which label matches best.
            classNames.indices
                .forEach { classIndex ->
                    val score =
                        value(
                            BOX_CHANNELS +
                                classIndex
                        )

                    if (score > bestScore) {
                        bestScore = score
                        bestClassIndex =
                            classIndex
                    }
                }

            // skip it if the score is too low or the box is weird.
            if (
                bestClassIndex < 0 ||
                bestScore <
                confidenceThreshold ||
                width <= 0f ||
                height <= 0f
            ) {
                continue
            }

            // sometimes models give coordinates as percentages, sometimes as pixels.
            val valuesAreNormalized =
                max(
                    max(centerX, centerY),
                    max(width, height)
                ) <= NORMALIZED_COORDINATE_LIMIT

            val inputSize =
                if (
                    valuesAreNormalized
                ) {
                    preprocessing
                        .bufferInputSize()
                        .toFloat()
                } else {
                    1f
                }

            // convert box to pixel coordinates on the model's input image.
            val modelCenterX =
                centerX * inputSize

            val modelCenterY =
                centerY * inputSize

            val modelWidth =
                width * inputSize

            val modelHeight =
                height * inputSize

            val modelLeft =
                modelCenterX -
                    modelWidth / 2f

            val modelTop =
                modelCenterY -
                    modelHeight / 2f

            val modelRight =
                modelCenterX +
                    modelWidth / 2f

            val modelBottom =
                modelCenterY +
                    modelHeight / 2f

            // now translate those coordinates back to the original photo's size.
            val originalLeft =
                (
                    modelLeft -
                        preprocessing.paddingX
                    ) /
                    preprocessing.scale

            val originalTop =
                (
                    modelTop -
                        preprocessing.paddingY
                    ) /
                    preprocessing.scale

            val originalRight =
                (
                    modelRight -
                        preprocessing.paddingX
                    ) /
                    preprocessing.scale

            val originalBottom =
                (
                    modelBottom -
                        preprocessing.paddingY
                    ) /
                    preprocessing.scale

            // make sure the box stays inside the photo.
            val clampedBox =
                RectF(
                    originalLeft.coerceIn(
                        0f,
                        preprocessing
                            .originalWidth
                            .toFloat()
                    ),
                    originalTop.coerceIn(
                        0f,
                        preprocessing
                            .originalHeight
                            .toFloat()
                    ),
                    originalRight.coerceIn(
                        0f,
                        preprocessing
                            .originalWidth
                            .toFloat()
                    ),
                    originalBottom.coerceIn(
                        0f,
                        preprocessing
                            .originalHeight
                            .toFloat()
                    )
                )

            // skip tiny boxes that are probably mistakes.
            if (
                clampedBox.width() <
                    MINIMUM_BOX_SIZE ||
                clampedBox.height() <
                    MINIMUM_BOX_SIZE
            ) {
                continue
            }

            candidates +=
                DetectionResult(
                    label =
                        makeFriendlyLabel(
                            classNames[
                                bestClassIndex
                            ]
                        ),
                    score =
                        bestScore,
                    boundingBox =
                        clampedBox
                )
        }

        // clean up the list one last time to remove duplicates.
        return NonMaximumSuppression
            .apply(
                detections =
                    candidates,
                iouThreshold =
                    nmsThreshold
            )
    }

    // helper to fix labels so they aren't just technical terms.
    private fun makeFriendlyLabel(
        rawLabel: String
    ): String {
        val normalized =
            when (
                rawLabel
                    .trim()
                    .lowercase(
                        Locale.getDefault()
                    )
            ) {
                "tv" -> "Television"
                "cell phone" ->
                    "Mobile phone"

                "couch" -> "Sofa"
                "dining table" ->
                    "Table"

                else -> rawLabel.trim()
            }

        return normalized
            .replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(
                        Locale.getDefault()
                    )
                } else {
                    it.toString()
                }
            }
    }

    // internal helper to figure out how big the model input was.
    private fun YoloImagePreprocessor.Result
        .bufferInputSize(): Int {
        val pixelCount =
            buffer.capacity() /
                FLOAT_BYTES /
                RGB_CHANNELS

        return kotlin.math.sqrt(
            pixelCount.toDouble()
        ).toInt()
    }

    companion object {
        private const val BOX_CHANNELS =
            4

        private const val FLOAT_BYTES =
            4

        private const val RGB_CHANNELS =
            3

        private const val NORMALIZED_COORDINATE_LIMIT =
            2f

        private const val MINIMUM_BOX_SIZE =
            2f
    }
}
