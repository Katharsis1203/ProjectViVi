package com.example.visualvocab.feature.vocab.presentation

import android.net.Uri
import com.example.visualvocab.domain.model.AppMode
import com.example.visualvocab.domain.model.DetectionResult

sealed interface VocabUiEvent {
    data class ImageSelected(val uri: Uri) : VocabUiEvent
    data object AnalyzeImage : VocabUiEvent
    data class ObjectTapped(val detection: DetectionResult) : VocabUiEvent
    data class OverlappingDetectionsChanged(val detections: List<DetectionResult>) : VocabUiEvent

    data object RegenerateSentence : VocabUiEvent
    data object MakeSentenceEasier : VocabUiEvent
    data object MakeSentenceHarder : VocabUiEvent
    data object ClearSelectedObject : VocabUiEvent
    data class LessonAnswerSelected(val answer: String) : VocabUiEvent
    data object ContinueLesson : VocabUiEvent
    data object RestartLesson : VocabUiEvent

    data class ChangeMode(val mode: AppMode) : VocabUiEvent

    // Annotation Events
    data class ChangeAnnotationTool(val tool: AnnotationTool) : VocabUiEvent
    data class AddDetectionAnnotation(val detection: DetectionResult) : VocabUiEvent
    data class AddManualAnnotation(val left: Float, val top: Float, val right: Float, val bottom: Float) : VocabUiEvent
    data class UpdateAnnotationBox(val id: String, val left: Float, val top: Float, val right: Float, val bottom: Float) : VocabUiEvent
    data class SelectTrainingAnnotation(val annotationId: String) : VocabUiEvent
    data object DismissTrainingAnnotation : VocabUiEvent
    data class UpdateTrainingLabel(val annotationId: String, val label: String) : VocabUiEvent
    data class ConfirmTrainingAnnotation(val annotationId: String) : VocabUiEvent
    data class DeleteTrainingAnnotation(val annotationId: String) : VocabUiEvent
    data object SaveTrainingExample : VocabUiEvent
    data class ExportTrainingDataset(val destination: Uri) : VocabUiEvent
    data object ClearTrainingMessage : VocabUiEvent

    // Optional Upload
    data object UploadTrainingDataset : VocabUiEvent
    data object ClearDatasetUploadMessage : VocabUiEvent
}