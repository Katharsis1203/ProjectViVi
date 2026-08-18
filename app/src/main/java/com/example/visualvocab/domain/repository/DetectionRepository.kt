package com.example.visualvocab.domain.repository

import android.graphics.Bitmap
import com.example.visualvocab.domain.model.DetectionResult
import kotlinx.coroutines.flow.Flow

// interface for finding objects in photos.
interface DetectionRepository {
    // finds objects and where they are.
    fun detectObjects(bitmap: Bitmap): Flow<List<DetectionResult>>
    // just gives us general labels for the whole picture.
    fun labelImage(bitmap: Bitmap): Flow<List<String>>
}
