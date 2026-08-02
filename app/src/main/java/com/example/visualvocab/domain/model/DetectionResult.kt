package com.example.visualvocab.domain.model

import android.graphics.RectF

data class DetectionResult(
    val label: String,
    val score: Float,
    val boundingBox: RectF
)
