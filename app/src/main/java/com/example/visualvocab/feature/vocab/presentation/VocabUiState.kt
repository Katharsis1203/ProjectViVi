package com.example.visualvocab.feature.vocab.presentation

import android.graphics.Bitmap
import android.net.Uri
import com.example.visualvocab.data.progress.PlayerProgress
import com.example.visualvocab.data.progress.WordProgress
import com.example.visualvocab.domain.model.AppMode
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.Vocabulary

enum class AnnotationTool {
    SELECT, DRAW
}

enum class LessonAnswerResult {
    CORRECT,
    INCORRECT
}

enum class LearningSessionMode {
    SCAN,
    REVIEW
}

enum class LessonQuestionType {
    ENGLISH_TO_SPANISH,
    SPANISH_TO_ENGLISH,
    TAP_OBJECT
}

// this big class stores everything the UI needs to know to show the right screen.
data class VocabUiState(
    val selectedImageUri: Uri? = null,
    val bitmap: Bitmap? = null,
    val detections: List<DetectionResult> = emptyList(),
    val generalLabels: List<String> = emptyList(),
    val selectedDetection: DetectionResult? = null,
    val lessonTargetDetection: DetectionResult? = null,
    val overlappingDetections: List<DetectionResult> = emptyList(),
    val vocabulary: Vocabulary? = null,
    val sentenceDifficulty: SentenceDifficulty = SentenceDifficulty.MEDIUM,
    val isDetecting: Boolean = false,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,

    val appMode: AppMode = AppMode.LEARN,

    // stuff for the current lesson or review session.
    val learningSessionMode: LearningSessionMode = LearningSessionMode.SCAN,
    val lessonQuestionType: LessonQuestionType = LessonQuestionType.ENGLISH_TO_SPANISH,
    val lessonPrompt: String = "",
    val lessonCorrectAnswer: String = "",
    val lessonTargetCount: Int = 0,
    val lessonAnsweredCount: Int = 0,
    val lessonCorrectCount: Int = 0,
    val lessonOptions: List<String> = emptyList(),
    val selectedLessonAnswer: String? = null,
    val lessonAnswerResult: LessonAnswerResult? = null,
    val completedLessonDetections: List<DetectionResult> = emptyList(),
    val isLessonComplete: Boolean = false,
    val lessonXpEarned: Int = 0,
    val lessonNewWords: Int = 0,
    val reviewQueue: List<WordProgress> = emptyList(),
    val reviewCurrentIndex: Int = 0,

    // persistent player progression
    val playerProgress: PlayerProgress = PlayerProgress(),
    val isProgressLoaded: Boolean = false,
    val recentXpAward: Int = 0,
    val xpAnimationToken: Long = 0L,

    // stuff for teach mode where we label images.
    val targetWord: String? = null,
    val annotationTool: AnnotationTool = AnnotationTool.SELECT,
    val editableTrainingAnnotations: List<EditableTrainingAnnotation> = emptyList(),
    val selectedTrainingAnnotationId: String? = null,
    val trainingExampleCount: Int = 0,
    val isSavingTrainingExample: Boolean = false,
    val isExportingTrainingDataset: Boolean = false,
    val trainingMessage: String? = null,

    // optional upload
    val isUploadingTrainingDataset: Boolean = false,
    val datasetUploadMessage: String? = null
) {
    // tells us if the app is busy doing something in the background.
    val isProcessing: Boolean
        get() = isDetecting ||
                isGenerating ||
                isSavingTrainingExample ||
                isExportingTrainingDataset ||
                isUploadingTrainingDataset

    val hasSelectedObject: Boolean
        get() = selectedDetection != null

    val hasActiveQuestion: Boolean
        get() = vocabulary != null || isGenerating || lessonTargetDetection != null

    // helper to show the current question number.
    val currentLessonQuestionNumber: Int
        get() = if (lessonTargetCount <= 0) {
            0
        } else {
            (lessonAnsweredCount + 1).coerceAtMost(lessonTargetCount)
        }

    val remainingLessonQuestionCount: Int
        get() = (lessonTargetCount - lessonAnsweredCount).coerceAtLeast(0)

    val currentReviewWord: WordProgress?
        get() = reviewQueue.getOrNull(reviewCurrentIndex)

    val selectedTrainingAnnotation: EditableTrainingAnnotation?
        get() = editableTrainingAnnotations.firstOrNull { it.id == selectedTrainingAnnotationId }
}
