package com.example.visualvocab.domain.usecase

import android.graphics.Bitmap
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.domain.repository.DetectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

// this usecase combines two ways of finding objects in a picture to give a better result.
class DetectObjectsUseCase(
    private val repository: DetectionRepository
) {
    // a simple container for both types of detection results.
    data class DetectionOutput(
        val detections: List<DetectionResult>,
        val labels: List<String>
    )

    // both detection methods are called and wait for both to finish.
    operator fun invoke(bitmap: Bitmap): Flow<DetectionOutput> {
        return combine(
            repository.detectObjects(bitmap),
            repository.labelImage(bitmap)
        ) { detections, labels ->
            DetectionOutput(detections, labels)
        }
    }
}
