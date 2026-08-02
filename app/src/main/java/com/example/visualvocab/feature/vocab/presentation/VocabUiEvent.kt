package com.example.visualvocab.feature.vocab.presentation

import android.net.Uri
import com.example.visualvocab.domain.model.AppMode
import com.example.visualvocab.domain.model.DetectionResult

sealed interface VocabUiEvent {

    data class ImageSelected(
        val uri: Uri
    ) : VocabUiEvent

    data object AnalyzeImage :
        VocabUiEvent

    data class ObjectTapped(
        val detection: DetectionResult
    ) : VocabUiEvent

    data class OverlappingDetectionsChanged(
        val detections:
        List<DetectionResult>
    ) : VocabUiEvent

    data object RegenerateSentence :
        VocabUiEvent

    data object MakeSentenceEasier :
        VocabUiEvent

    data object MakeSentenceHarder :
        VocabUiEvent

    data object ClearSelectedObject :
        VocabUiEvent

    data class ChangeMode(
        val mode: AppMode
    ) : VocabUiEvent

    data class SelectTrainingAnnotation(
        val annotationId: String
    ) : VocabUiEvent

    data object DismissTrainingAnnotation :
        VocabUiEvent

    data class UpdateTrainingLabel(
        val annotationId: String,
        val label: String
    ) : VocabUiEvent

    data class ConfirmTrainingAnnotation(
        val annotationId: String
    ) : VocabUiEvent

    data class DeleteTrainingAnnotation(
        val annotationId: String
    ) : VocabUiEvent

    data object SaveTrainingExample :
        VocabUiEvent

    data class ExportTrainingDataset(
        val destination: Uri
    ) : VocabUiEvent

    data object ClearTrainingMessage :
        VocabUiEvent
}