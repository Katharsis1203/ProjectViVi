package com.example.visualvocab.feature.vocab.presentation

import android.graphics.RectF
import java.util.UUID

// this class is for a label that we are currently editing or confirming.
data class EditableTrainingAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val boundingBox: RectF,
    val originalLabel: String?,
    val originalConfidence: Float?,
    val isConfirmed: Boolean = false
)
