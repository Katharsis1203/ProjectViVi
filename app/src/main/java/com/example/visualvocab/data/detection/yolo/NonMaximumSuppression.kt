package com.example.visualvocab.data.detection.yolo

import android.graphics.RectF
import com.example.visualvocab.domain.model.DetectionResult
import kotlin.math.max
import kotlin.math.min

internal object NonMaximumSuppression {

    fun apply(
        detections:
            List<DetectionResult>,
        iouThreshold: Float
    ): List<DetectionResult> {
        if (detections.isEmpty()) {
            return emptyList()
        }

        val results =
            mutableListOf<DetectionResult>()

        detections
            .groupBy {
                it.label
                    .trim()
                    .lowercase()
            }
            .values
            .forEach { sameClass ->
                val remaining =
                    sameClass
                        .sortedByDescending {
                            it.score
                        }
                        .toMutableList()

                while (
                    remaining.isNotEmpty()
                ) {
                    val best =
                        remaining.removeAt(0)

                    results += best

                    remaining.removeAll {
                        intersectionOverUnion(
                            best.boundingBox,
                            it.boundingBox
                        ) >= iouThreshold
                    }
                }
            }

        return results
            .sortedByDescending {
                it.score
            }
    }

    fun intersectionOverUnion(
        first: RectF,
        second: RectF
    ): Float {
        val intersectionLeft =
            max(
                first.left,
                second.left
            )

        val intersectionTop =
            max(
                first.top,
                second.top
            )

        val intersectionRight =
            min(
                first.right,
                second.right
            )

        val intersectionBottom =
            min(
                first.bottom,
                second.bottom
            )

        val intersectionWidth =
            max(
                0f,
                intersectionRight -
                    intersectionLeft
            )

        val intersectionHeight =
            max(
                0f,
                intersectionBottom -
                    intersectionTop
            )

        val intersectionArea =
            intersectionWidth *
                intersectionHeight

        if (
            intersectionArea <= 0f
        ) {
            return 0f
        }

        val firstArea =
            max(0f, first.width()) *
                max(0f, first.height())

        val secondArea =
            max(0f, second.width()) *
                max(0f, second.height())

        val unionArea =
            firstArea +
                secondArea -
                intersectionArea

        return if (unionArea > 0f) {
            intersectionArea /
                unionArea
        } else {
            0f
        }
    }
}
