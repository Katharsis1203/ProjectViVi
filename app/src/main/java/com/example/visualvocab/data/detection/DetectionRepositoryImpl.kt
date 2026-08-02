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

class DetectionRepositoryImpl(
    context: Context,
    private val objectDetectorManager:
    ObjectDetectorManager
) : DetectionRepository {

    private val appContext =
        context.applicationContext

    private val yoloDetectorManager =
        try {
            YoloObjectDetectorManager(
                context = appContext
            )
        } catch (e: Exception) {
            null
        }

    private val combinedDetectorManager =
        CombinedObjectDetectorManager(
            efficientDetector =
                objectDetectorManager,
            yoloDetector =
                yoloDetectorManager
        )

    private val labelerOptions =
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()

    private val labeler =
        ImageLabeling.getClient(
            labelerOptions
        )

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