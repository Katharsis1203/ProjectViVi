package com.example.visualvocab.domain.model.training

data class TrainingAnnotation(
    val label: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val originalLabel: String? = null,
    val originalConfidence: Float? = null
) {
    init {
        require(label.isNotBlank()) {
            "Annotation label cannot be blank."
        }

        require(left in 0f..1f)
        require(top in 0f..1f)
        require(right in 0f..1f)
        require(bottom in 0f..1f)
        require(left < right)
        require(top < bottom)
    }
}