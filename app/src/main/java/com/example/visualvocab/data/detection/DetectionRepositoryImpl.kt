package com.example.visualvocab.data.detection

import android.content.Context
import android.graphics.Bitmap
import com.example.visualvocab.data.detection.yolo.YoloObjectDetectorManager
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.domain.repository.DetectionRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

// this repository handles finding things in photos using all our detectors.
class DetectionRepositoryImpl(
    context: Context,
    private val objectDetectorManager:
    ObjectDetectorManager
) : DetectionRepository {

    private val appContext =
        context.applicationContext

    // setting up the YOLO detector, but it might not work on all phones.
    private val yoloDetectorManager =
        try {
            YoloObjectDetectorManager(
                context = appContext
            )
        } catch (e: Exception) {
            null
        }

    // a combined manager handles both detectors at once.
    private val combinedDetectorManager =
        CombinedObjectDetectorManager(
            efficientDetector =
                objectDetectorManager,
            yoloDetector =
                yoloDetectorManager
        )

    // settings for the image labeler.
    private val labelerOptions =
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()

    // ML Kit labeler for general categorization.
    private val labeler =
        ImageLabeling.getClient(
            labelerOptions
        )

    // detect objects and their positions in the picture.
    override fun detectObjects(
        bitmap: Bitmap
    ): Flow<List<DetectionResult>> =
        flow {
            val detections =
                withContext(
                    Dispatchers.Default
                ) {
                    combinedDetectorManager
                        .detect(bitmap)
                }

            emit(detections)
        }

    // just label the whole image without worrying about where things are.
    override fun labelImage(
        bitmap: Bitmap
    ): Flow<List<String>> =
        callbackFlow {
            val inputImage =
                InputImage.fromBitmap(
                    bitmap,
                    0
                )

            labeler.process(inputImage)
                .addOnSuccessListener {
                        labels ->

                    // get the top 10 labels that look good.
                    val result =
                        labels
                            .sortedByDescending {
                                it.confidence
                            }
                            .take(10)
                            .map {
                                it.text
                            }

                    trySend(result)
                }
                .addOnFailureListener {
                    trySend(emptyList())
                }
                .addOnCompleteListener {
                    close()
                }

            awaitClose()
        }
}
