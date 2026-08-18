package com.example.visualvocab.data.detection

import android.graphics.Bitmap
import com.example.visualvocab.data.detection.yolo.NonMaximumSuppression
import com.example.visualvocab.data.detection.yolo.YoloObjectDetectorManager
import com.example.visualvocab.domain.model.DetectionResult

// this class puts together results from two different object detectors for the best list.
class CombinedObjectDetectorManager(
    private val efficientDetector:
        ObjectDetectorManager,
    private val yoloDetector:
        YoloObjectDetectorManager?
) {

    // both detectors run and then the results mix.
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

    // this is where the lists combine and duplicates are removed.
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
            // check if this object is already in the list.
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

            // if it's new, add it. if it's better than what currently exists, replace the old one.
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

    // check if two labels are basically the same word.
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
        // how much overlap is allowed before saying it's the same thing.
        private const val DUPLICATE_IOU_THRESHOLD =
            0.55f

        // only return the top 30 results so the UI is not overwhelmed.
        private const val MAX_RESULTS =
            30
    }
}
