package com.example.visualvocab.feature.vocab.presentation

import android.net.Uri
import com.example.visualvocab.domain.model.AppMode
import com.example.visualvocab.domain.model.DetectionResult

// this is a big list of all the things the user can do in the app.
sealed interface VocabUiEvent {
    // choosing a photo and looking at it.
    data class ImageSelected(val uri: Uri) : VocabUiEvent
    data object AnalyzeImage : VocabUiEvent
    data class ObjectTapped(val detection: DetectionResult) : VocabUiEvent
    data class OverlappingDetectionsChanged(val detections: List<DetectionResult>) : VocabUiEvent

    // playing with the AI and sentences.
    data object RegenerateSentence : VocabUiEvent
    data object MakeSentenceEasier : VocabUiEvent
    data object MakeSentenceHarder : VocabUiEvent
    data object ClearSelectedObject : VocabUiEvent
    data class LessonAnswerSelected(val answer: String) : VocabUiEvent
    data object ContinueLesson : VocabUiEvent
    data object RetryLessonQuestion : VocabUiEvent
    data object RestartLesson : VocabUiEvent

    // reviewing words the user already knows.
    data object StartReview : VocabUiEvent
    data class StartWordReview(val wordKey: String) : VocabUiEvent
    data object ContinueReview : VocabUiEvent
    data object RestartReview : VocabUiEvent
    data object EndReview : VocabUiEvent

    // changing or deleting saved words.
    data class UpdateSavedWord(
        val wordKey: String,
        val english: String,
        val spanish: String
    ) : VocabUiEvent

    data class DeleteSavedWord(val wordKey: String) : VocabUiEvent
    data object CompleteOnboarding : VocabUiEvent

    // switching between learning and teaching.
    data class ChangeMode(val mode: AppMode) : VocabUiEvent

    // things the user does when they are labeling images for training.
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

    // optional upload
    data object UploadTrainingDataset : VocabUiEvent
    data object ClearDatasetUploadMessage : VocabUiEvent
}
