package com.example.visualvocab.domain.repository

import android.graphics.Bitmap
import com.example.visualvocab.domain.model.training.TrainingAnnotation
import java.io.OutputStream

interface TrainingRepository {

    suspend fun saveExample(
        bitmap: Bitmap,
        annotations: List<TrainingAnnotation>
    )

    suspend fun exportDataset(
        outputStream: OutputStream
    )

    suspend fun exportDatasetToByteArray(): ByteArray

    suspend fun getExampleCount(): Int
}