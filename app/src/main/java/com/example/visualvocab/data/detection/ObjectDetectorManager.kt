package com.example.visualvocab.data.detection

import android.content.Context
import android.graphics.Bitmap
import com.example.visualvocab.domain.model.DetectionResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import java.util.Locale

// this class manages the efficient object detector from MediaPipe.
class ObjectDetectorManager(private val context: Context) {

    private var detector: ObjectDetector? = null

    init {
        setupDetector()
    }

    // set up the detector with our model file.
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

    // run the detection on a bitmap.
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        // the bitmap needs to be in the right format.
        val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val mpImage = BitmapImageBuilder(argbBitmap).build()
        val detectionResult = detector?.detect(mpImage)

        // turn the results into our domain objects.
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

    // free up memory when finished.
    fun close() {
        detector?.close()
        detector = null
    }

    // fix some labels so they sound more like what a person would say.
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
