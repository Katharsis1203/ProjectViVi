package com.example.visualvocab.domain.model.training

data class TrainingExample(
    val id: String,
    val imageFileName: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val createdAt: Long,
    val annotations: List<TrainingAnnotation>
)