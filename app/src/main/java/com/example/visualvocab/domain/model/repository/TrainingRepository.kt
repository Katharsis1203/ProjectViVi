package com.example.visualvocab.domain.repository

import android.graphics.Bitmap
import com.example.visualvocab.domain.model.training.AnnotatedExample
import com.example.visualvocab.domain.model.training.TrainingAnnotation
import java.io.OutputStream

// this interface defines how we save and get training data for our AI.
interface TrainingRepository {

    // saves a photo and the labels the user added.
    suspend fun saveExample(
        bitmap: Bitmap,
        annotations: List<TrainingAnnotation>
    )

    // gets all the training examples we've saved.
    suspend fun getExamples(): List<AnnotatedExample>

    // deletes a specific training example.
    suspend fun deleteExample(id: String)

    // bundles up the dataset into a ZIP to share it.
    suspend fun exportDataset(
        outputStream: OutputStream
    )

    // tells us how many examples we have in total.
    suspend fun getExampleCount(): Int
}
