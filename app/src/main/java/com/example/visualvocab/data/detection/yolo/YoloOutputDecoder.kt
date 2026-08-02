package com.example.visualvocab.data.detection.yolo

import android.graphics.RectF
import com.example.visualvocab.domain.model.DetectionResult
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

internal class YoloOutputDecoder(
    private val classNames:
        List<String>,
    private val confidenceThreshold:
        Float,
    private val nmsThreshold: Float
) {

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

            if (
                bestClassIndex < 0 ||
                bestScore <
                confidenceThreshold ||
                width <= 0f ||
                height <= 0f
            ) {
                continue
            }

            /*
             * Ultralytics YOLO TFLite exports normally produce xywh values
             * in model-input pixels. This also tolerates normalized xywh.
             */
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

        return NonMaximumSuppression
            .apply(
                detections =
                    candidates,
                iouThreshold =
                    nmsThreshold
            )
    }

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

    /*
     * ByteBuffer does not expose the input image size directly, but for
     * normalized-output fallback the square size can be derived from:
     * bytes / float bytes / RGB channels.
     */
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
