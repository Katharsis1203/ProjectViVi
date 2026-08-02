package com.example.visualvocab.domain.repository

import android.graphics.Bitmap
import com.example.visualvocab.domain.model.DetectionResult
import kotlinx.coroutines.flow.Flow

interface DetectionRepository {
    fun detectObjects(bitmap: Bitmap): Flow<List<DetectionResult>>
    fun labelImage(bitmap: Bitmap): Flow<List<String>>
}
