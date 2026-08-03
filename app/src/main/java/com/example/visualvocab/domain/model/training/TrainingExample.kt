package com.example.visualvocab.domain.model.training

// basically the same as annotated example, just used for storing the data.
data class TrainingExample(
    val id: String,
    val imageFileName: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val createdAt: Long,
    val annotations: List<TrainingAnnotation>
)
