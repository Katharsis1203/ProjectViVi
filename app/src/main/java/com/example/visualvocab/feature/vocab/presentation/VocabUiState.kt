package com.example.visualvocab.feature.vocab.presentation

import android.graphics.Bitmap
import android.net.Uri
import com.example.visualvocab.domain.model.AppMode
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.Vocabulary

data class VocabUiState(
    val selectedImageUri: Uri? = null,
    val bitmap: Bitmap? = null,
    val detections: List<DetectionResult> =
        emptyList(),
    val generalLabels: List<String> =
        emptyList(),
    val selectedDetection:
    DetectionResult? = null,
    val overlappingDetections:
    List<DetectionResult> = emptyList(),
    val vocabulary: Vocabulary? = null,
    val sentenceDifficulty:
    SentenceDifficulty =
        SentenceDifficulty.MEDIUM,
    val isDetecting: Boolean = false,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,

    val appMode: AppMode =
        AppMode.LEARN,

    val editableTrainingAnnotations:
    List<EditableTrainingAnnotation> =
        emptyList(),

    val selectedTrainingAnnotationId:
    String? = null,

    val trainingExampleCount: Int = 0,
    val isSavingTrainingExample:
    Boolean = false,
    val isExportingTrainingDataset:
    Boolean = false,
    val trainingMessage: String? = null
) {
    val isProcessing: Boolean
        get() =
            isDetecting ||
                    isGenerating ||
                    isSavingTrainingExample ||
                    isExportingTrainingDataset

    val hasSelectedObject: Boolean
        get() =
            selectedDetection != null

    val selectedTrainingAnnotation:
            EditableTrainingAnnotation?
        get() =
            editableTrainingAnnotations
                .firstOrNull {
                    it.id ==
                            selectedTrainingAnnotationId
                }
}