package com.example.visualvocab.data.training

import android.content.Context
import android.net.Uri
import com.example.visualvocab.domain.repository.TrainingRepository

class TrainingExportManager(
    context: Context,
    private val repository:
    TrainingRepository
) {
    private val appContext =
        context.applicationContext

    suspend fun export(
        destination: Uri
    ) {
        val outputStream =
            appContext.contentResolver
                .openOutputStream(destination)
                ?: throw IllegalStateException(
                    "Could not open the export destination."
                )

        outputStream.use {
            repository.exportDataset(it)
        }
    }
}