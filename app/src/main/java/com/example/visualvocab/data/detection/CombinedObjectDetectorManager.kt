package com.example.visualvocab.data.detection

import android.graphics.Bitmap
import com.example.visualvocab.data.detection.yolo.NonMaximumSuppression
import com.example.visualvocab.data.detection.yolo.YoloObjectDetectorManager
import com.example.visualvocab.domain.model.DetectionResult

class CombinedObjectDetectorManager(
    private val efficientDetector:
        ObjectDetectorManager,
    private val yoloDetector:
        YoloObjectDetectorManager?
) {

    fun detect(
        bitmap: Bitmap
    ): List<DetectionResult> {
        val efficientDetections =
            runCatching {
                efficientDetector
                    .detect(bitmap)
            }.getOrDefault(
                emptyList()
            )

        val yoloDetections =
            yoloDetector?.let { detector ->
                runCatching {
                    detector.detect(bitmap)
                }.getOrDefault(
                    emptyList()
                )
            } ?: emptyList()

        return merge(
            efficientDetections =
                efficientDetections,
            yoloDetections =
                yoloDetections
        )
    }

    private fun merge(
        efficientDetections:
            List<DetectionResult>,
        yoloDetections:
            List<DetectionResult>
    ): List<DetectionResult> {
        val combined =
            (
                efficientDetections +
                    yoloDetections
                )
                .sortedByDescending {
                    it.score
                }

        val merged =
            mutableListOf<DetectionResult>()

        combined.forEach { candidate ->
            val duplicateIndex =
                merged.indexOfFirst {
                        existing ->

                    sameLabel(
                        existing.label,
                        candidate.label
                    ) &&
                        NonMaximumSuppression
                            .intersectionOverUnion(
                                existing.boundingBox,
                                candidate.boundingBox
                            ) >=
                        DUPLICATE_IOU_THRESHOLD
                }

            if (duplicateIndex < 0) {
                merged += candidate
            } else if (
                candidate.score >
                merged[duplicateIndex].score
            ) {
                merged[duplicateIndex] =
                    candidate
            }
        }

        return merged
            .sortedByDescending {
                it.score
            }
            .take(MAX_RESULTS)
    }

    private fun sameLabel(
        first: String,
        second: String
    ): Boolean {
        return first
            .trim()
            .equals(
                second.trim(),
                ignoreCase = true
            )
    }

    companion object {
        private const val DUPLICATE_IOU_THRESHOLD =
            0.55f

        private const val MAX_RESULTS =
            30
    }
}
