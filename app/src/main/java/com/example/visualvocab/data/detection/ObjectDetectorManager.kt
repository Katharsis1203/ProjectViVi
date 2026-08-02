package com.example.visualvocab.data.detection

import android.content.Context
import android.graphics.Bitmap
import com.example.visualvocab.domain.model.DetectionResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import java.util.Locale

class ObjectDetectorManager(private val context: Context) {

    private var detector: ObjectDetector? = null

    init {
        setupDetector()
    }

    private fun setupDetector() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("efficient2.tflite")
            .build()

        val detectorOptions = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setScoreThreshold(0.35f)
            .setMaxResults(20)
            .build()

        detector = ObjectDetector.createFromOptions(context, detectorOptions)
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val mpImage = BitmapImageBuilder(argbBitmap).build()
        val detectionResult = detector?.detect(mpImage)

        return detectionResult?.detections()?.mapNotNull { detection ->
            val bestCategory = detection.categories().maxByOrNull { it.score() }
            bestCategory?.let { category ->
                DetectionResult(
                    label = makeFriendlyLabel(category.categoryName()),
                    score = category.score(),
                    boundingBox = detection.boundingBox()
                )
            }
        } ?: emptyList()
    }

    fun close() {
        detector?.close()
        detector = null
    }

    private fun makeFriendlyLabel(rawLabel: String): String {
        val normalisedLabel = when (rawLabel.lowercase(Locale.getDefault())) {
            "tv" -> "Television"
            "cell phone" -> "Mobile phone"
            "couch" -> "Sofa"
            "dining table" -> "Table"
            else -> rawLabel
        }
        return normalisedLabel.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}
