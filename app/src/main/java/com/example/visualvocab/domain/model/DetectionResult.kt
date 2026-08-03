package com.example.visualvocab.domain.model

import android.graphics.RectF

// this data class stores info about an object the app found, like what it is and where it is.
data class DetectionResult(
    val label: String,
    val score: Float,
    val boundingBox: RectF
)
