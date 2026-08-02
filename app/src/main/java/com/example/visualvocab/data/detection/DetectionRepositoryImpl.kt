package com.example.visualvocab.data.detection

import android.content.Context
import android.graphics.Bitmap
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.domain.repository.DetectionRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

class DetectionRepositoryImpl(
    private val context: Context,
    private val objectDetectorManager: ObjectDetectorManager
) : DetectionRepository {

    private val labelerOptions = ImageLabelerOptions.Builder()
        .setConfidenceThreshold(0.5f)
        .build()
    private val labeler = ImageLabeling.getClient(labelerOptions)

    override fun detectObjects(bitmap: Bitmap): Flow<List<DetectionResult>> = flow {
        emit(objectDetectorManager.detect(bitmap))
    }

    override fun labelImage(bitmap: Bitmap): Flow<List<String>> = callbackFlow {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        labeler.process(inputImage)
            .addOnSuccessListener { labels ->
                val result = labels
                    .sortedByDescending { it.confidence }
                    .take(10)
                    .map { it.text }
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
