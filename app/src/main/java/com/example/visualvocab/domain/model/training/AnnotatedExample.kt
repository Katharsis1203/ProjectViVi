package com.example.visualvocab.domain.model.training

// this class represents a single photo that has been labeled for training.
data class AnnotatedExample(
    val id: String,
    val imageFileName: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val createdAt: Long,
    val annotations: List<TrainingAnnotation>
)
