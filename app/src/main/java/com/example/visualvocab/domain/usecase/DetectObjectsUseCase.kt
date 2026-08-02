package com.example.visualvocab.domain.usecase

import android.graphics.Bitmap
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.domain.repository.DetectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DetectObjectsUseCase(
    private val repository: DetectionRepository
) {
    data class DetectionOutput(
        val detections: List<DetectionResult>,
        val labels: List<String>
    )

    operator fun invoke(bitmap: Bitmap): Flow<DetectionOutput> {
        return combine(
            repository.detectObjects(bitmap),
            repository.labelImage(bitmap)
        ) { detections, labels ->
            DetectionOutput(detections, labels)
        }
    }
}
