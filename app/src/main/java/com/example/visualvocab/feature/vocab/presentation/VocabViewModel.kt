package com.example.visualvocab.feature.vocab.presentation

import android.graphics.RectF
import android.net.Uri
import java.text.Normalizer
import java.util.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visualvocab.core.image.BitmapLoader
import com.example.visualvocab.data.training.TrainingExportManager
import com.example.visualvocab.data.datasetupload.DatasetUploadManager
import com.example.visualvocab.data.progress.DayKeys
import com.example.visualvocab.data.progress.PlayerProgress
import com.example.visualvocab.data.progress.PlayerProgressStore
import com.example.visualvocab.data.progress.WordProgress
import com.example.visualvocab.domain.model.AppMode
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.training.TrainingAnnotation
import com.example.visualvocab.domain.repository.TrainingRepository
import com.example.visualvocab.domain.usecase.DetectObjectsUseCase
import com.example.visualvocab.domain.usecase.GenerateVocabularyUseCase
import com.example.visualvocab.domain.usecase.RegenerateSentenceUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VocabViewModel(
    private val bitmapLoader: BitmapLoader,
    private val detectObjectsUseCase: DetectObjectsUseCase,
    private val generateVocabularyUseCase: GenerateVocabularyUseCase,
    private val regenerateSentenceUseCase: RegenerateSentenceUseCase,
    private val trainingRepository: TrainingRepository,
    private val trainingExportManager: TrainingExportManager,
    private val datasetUploadManager: DatasetUploadManager,
    private val playerProgressStore: PlayerProgressStore
) : ViewModel() {

    private val initialPlayerProgress = runCatching { playerProgressStore.load() }
        .getOrDefault(PlayerProgress())

    private val _uiState = MutableStateFlow(
        VocabUiState(
            playerProgress = initialPlayerProgress,
            isProgressLoaded = true
        )
    )
    val uiState: StateFlow<VocabUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<VocabUiEffect>()
    val uiEffect: SharedFlow<VocabUiEffect> = _uiEffect.asSharedFlow()

    private var imageLoadingJob: Job? = null
    private var detectionJob: Job? = null
    private var generationJob: Job? = null
    private var trainingJob: Job? = null

    init {
        refreshTrainingExampleCount()
    }

    fun onEvent(event: VocabUiEvent) {
        when (event) {
            is VocabUiEvent.ImageSelected -> handleImageSelected(event.uri)
            VocabUiEvent.AnalyzeImage -> analyzeImage()
            is VocabUiEvent.ObjectTapped -> handleObjectTapped(event.detection)
            is VocabUiEvent.OverlappingDetectionsChanged -> _uiState.update { it.copy(overlappingDetections = event.detections) }
            VocabUiEvent.RegenerateSentence -> regenerateSentence()
            VocabUiEvent.MakeSentenceEasier -> changeDifficulty(false)
            VocabUiEvent.MakeSentenceHarder -> changeDifficulty(true)
            VocabUiEvent.ClearSelectedObject -> clearSelectedObject()
            is VocabUiEvent.LessonAnswerSelected -> selectLessonAnswer(event.answer)
            VocabUiEvent.ContinueLesson -> continueLesson()
            VocabUiEvent.RestartLesson -> restartLesson()
            is VocabUiEvent.ChangeMode -> changeMode(event.mode)

            // Annotation Events
            is VocabUiEvent.ChangeAnnotationTool -> _uiState.update { it.copy(annotationTool = event.tool, selectedTrainingAnnotationId = null) }
            is VocabUiEvent.AddDetectionAnnotation -> addDetectionAnnotation(event.detection)
            is VocabUiEvent.AddManualAnnotation -> addManualAnnotation(event.left, event.top, event.right, event.bottom)
            is VocabUiEvent.UpdateAnnotationBox -> updateAnnotationBox(event.id, event.left, event.top, event.right, event.bottom)
            is VocabUiEvent.SelectTrainingAnnotation -> selectTrainingAnnotation(event.annotationId)
            VocabUiEvent.DismissTrainingAnnotation -> _uiState.update { it.copy(selectedTrainingAnnotationId = null) }
            is VocabUiEvent.UpdateTrainingLabel -> updateTrainingLabel(event.annotationId, event.label)
            is VocabUiEvent.ConfirmTrainingAnnotation -> confirmTrainingAnnotation(event.annotationId)
            is VocabUiEvent.DeleteTrainingAnnotation -> deleteTrainingAnnotation(event.annotationId)
            VocabUiEvent.SaveTrainingExample -> saveTrainingExample()
            is VocabUiEvent.ExportTrainingDataset -> exportTrainingDataset(event.destination)
            VocabUiEvent.ClearTrainingMessage -> _uiState.update { it.copy(trainingMessage = null) }

            VocabUiEvent.UploadTrainingDataset -> uploadTrainingDataset()
            VocabUiEvent.ClearDatasetUploadMessage -> _uiState.update { it.copy(datasetUploadMessage = null) }
        }
    }

    private fun handleImageSelected(uri: Uri) {
        imageLoadingJob?.cancel()
        detectionJob?.cancel()
        generationJob?.cancel()

        val currentState = _uiState.value
        val currentMode = currentState.appMode
        val currentCount = currentState.trainingExampleCount
        val targetWord = currentState.targetWord
        val playerProgress = currentState.playerProgress
        val isProgressLoaded = currentState.isProgressLoaded

        imageLoadingJob = viewModelScope.launch {
            try {
                _uiState.value = VocabUiState(
                    selectedImageUri = uri,
                    isDetecting = true,
                    appMode = currentMode,
                    trainingExampleCount = currentCount,
                    targetWord = targetWord,
                    playerProgress = playerProgress,
                    isProgressLoaded = isProgressLoaded
                )

                val bitmap = bitmapLoader.loadFromUri(uri)
                if (bitmap == null) {
                    _uiState.value = VocabUiState(
                        selectedImageUri = uri,
                        appMode = currentMode,
                        trainingExampleCount = currentCount,
                        targetWord = targetWord,
                        playerProgress = playerProgress,
                        isProgressLoaded = isProgressLoaded,
                        errorMessage = "Failed to load image."
                    )
                    return@launch
                }

                _uiState.value = VocabUiState(
                    selectedImageUri = uri,
                    bitmap = bitmap,
                    isDetecting = true,
                    appMode = currentMode,
                    trainingExampleCount = currentCount,
                    targetWord = targetWord,
                    playerProgress = playerProgress,
                    isProgressLoaded = isProgressLoaded
                )

                analyzeImage()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isDetecting = false,
                        errorMessage = exception.message?.takeIf(String::isNotBlank) ?: "Failed to load image."
                    )
                }
            }
        }
    }

    private fun analyzeImage() {
        val bitmap = _uiState.value.bitmap ?: return

        detectionJob?.cancel()
        generationJob?.cancel()

        _uiState.update {
            it.copy(
                detections = emptyList(),
                generalLabels = emptyList(),
                selectedDetection = null,
                overlappingDetections = emptyList(),
                vocabulary = null,
                lessonTargetCount = 0,
                lessonAnsweredCount = 0,
                lessonCorrectCount = 0,
                lessonOptions = emptyList(),
                selectedLessonAnswer = null,
                lessonAnswerResult = null,
                completedLessonDetections = emptyList(),
                isLessonComplete = false,
                lessonXpEarned = 0,
                lessonNewWords = 0,
                recentXpAward = 0,
                editableTrainingAnnotations = emptyList(),
                selectedTrainingAnnotationId = null,
                isDetecting = true,
                isGenerating = false,
                errorMessage = null,
                trainingMessage = null
            )
        }

        detectionJob = viewModelScope.launch {
            try {
                detectObjectsUseCase(bitmap).collect { output ->
                    val distinctDetections = output.detections.distinctBy(::detectionKey)
                    _uiState.update {
                        it.copy(
                            detections = distinctDetections,
                            generalLabels = output.labels,
                            lessonTargetCount = minOf(LESSON_QUESTION_LIMIT, distinctDetections.size),
                            editableTrainingAnnotations = emptyList(),
                            selectedTrainingAnnotationId = null,
                            isDetecting = false,
                            errorMessage = if (distinctDetections.isEmpty()) "No objects were detected." else null
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isDetecting = false,
                        errorMessage = exception.message?.takeIf(String::isNotBlank) ?: "Object detection failed."
                    )
                }
            }
        }
    }

    private fun handleObjectTapped(detection: DetectionResult) {
        val current = _uiState.value

        if (current.appMode == AppMode.TEACH) {
            addDetectionAnnotation(detection)
            return
        }

        if (
            current.isLessonComplete ||
            current.isGenerating ||
            current.selectedDetection != null ||
            current.completedLessonDetections.any { detectionKey(it) == detectionKey(detection) }
        ) {
            return
        }

        generationJob?.cancel()
        val word = detection.label.trim().lowercase()
        if (word.isBlank()) return

        _uiState.update {
            it.copy(
                selectedDetection = detection,
                overlappingDetections = emptyList(),
                vocabulary = null,
                sentenceDifficulty = SentenceDifficulty.MEDIUM,
                lessonOptions = emptyList(),
                selectedLessonAnswer = null,
                lessonAnswerResult = null,
                isGenerating = true,
                errorMessage = null
            )
        }

        generationJob = viewModelScope.launch {
            try {
                generateVocabularyUseCase(
                    word = word,
                    difficulty = SentenceDifficulty.MEDIUM
                ).collect { vocabulary ->
                    if (_uiState.value.selectedDetection != detection) return@collect

                    val spanishAnswer = vocabulary.spanishWord.trim()
                    if (spanishAnswer.isBlank()) {
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                errorMessage = "Vivi could not create a translation for this object."
                            )
                        }
                        return@collect
                    }

                    _uiState.update {
                        it.copy(
                            vocabulary = vocabulary,
                            lessonOptions = createLessonOptions(spanishAnswer),
                            isGenerating = false,
                            errorMessage = null
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = exception.message
                            ?.takeIf(String::isNotBlank)
                            ?: "Vocabulary generation failed."
                    )
                }
            }
        }
    }

    private fun changeDifficulty(makeHarder: Boolean) {
        val current = _uiState.value
        val vocabulary = current.vocabulary ?: return
        val difficulty = if (makeHarder) current.sentenceDifficulty.harder() else current.sentenceDifficulty.easier()

        _uiState.update { it.copy(sentenceDifficulty = difficulty) }
        regenerateSentence(
            word = vocabulary.englishWord,
            previousSentence = vocabulary.englishSentence,
            difficulty = difficulty
        )
    }

    private fun regenerateSentence() {
        val state = _uiState.value
        val vocabulary = state.vocabulary ?: return
        regenerateSentence(
            word = vocabulary.englishWord,
            previousSentence = vocabulary.englishSentence,
            difficulty = state.sentenceDifficulty
        )
    }

    private fun regenerateSentence(word: String, previousSentence: String, difficulty: SentenceDifficulty) {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
                regenerateSentenceUseCase(
                    word = word,
                    previousSentence = previousSentence,
                    difficulty = difficulty
                ).collect { vocabulary ->
                    if (!_uiState.value.hasSelectedObject) return@collect
                    _uiState.update {
                        it.copy(vocabulary = vocabulary, isGenerating = false, errorMessage = null)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = exception.message?.takeIf(String::isNotBlank) ?: "Sentence generation failed."
                    )
                }
            }
        }
    }

    private fun clearSelectedObject() {
        generationJob?.cancel()
        generationJob = null
        _uiState.update {
            it.copy(
                selectedDetection = null,
                overlappingDetections = emptyList(),
                vocabulary = null,
                lessonOptions = emptyList(),
                selectedLessonAnswer = null,
                lessonAnswerResult = null,
                isGenerating = false,
                errorMessage = null
            )
        }
    }

    private fun selectLessonAnswer(answer: String) {
        val current = _uiState.value
        val vocabulary = current.vocabulary ?: return

        if (
            current.appMode != AppMode.LEARN ||
            current.isGenerating ||
            current.lessonAnswerResult != null ||
            current.isLessonComplete ||
            answer !in current.lessonOptions
        ) {
            return
        }

        val result = if (answersMatch(answer, vocabulary.spanishWord)) {
            LessonAnswerResult.CORRECT
        } else {
            LessonAnswerResult.INCORRECT
        }

        val today = DayKeys.today()
        val wordKey = normalizeAnswer(vocabulary.englishWord)
        val progress = current.playerProgress.normalizedForToday(today)
        val existingWord = progress.words.firstOrNull { it.key == wordKey }
        val isNewWord = existingWord == null
        val previousMastery = existingWord?.masteryPoints ?: 0
        val nextMastery = if (result == LessonAnswerResult.CORRECT) {
            (previousMastery + MASTERY_GAIN_CORRECT).coerceAtMost(MAX_MASTERY_POINTS)
        } else {
            (previousMastery - MASTERY_LOSS_INCORRECT).coerceAtLeast(0)
        }
        val reviewDelayDays = if (result == LessonAnswerResult.CORRECT) {
            reviewDelayDays(nextMastery)
        } else {
            0
        }

        val updatedWord = WordProgress(
            key = wordKey,
            english = vocabulary.englishWord.trim(),
            spanish = vocabulary.spanishWord.trim(),
            timesSeen = (existingWord?.timesSeen ?: 0) + 1,
            correctAnswers = (existingWord?.correctAnswers ?: 0) +
                    if (result == LessonAnswerResult.CORRECT) 1 else 0,
            incorrectAnswers = (existingWord?.incorrectAnswers ?: 0) +
                    if (result == LessonAnswerResult.INCORRECT) 1 else 0,
            masteryPoints = nextMastery,
            lastPractisedDay = today,
            nextReviewDay = DayKeys.daysFromToday(reviewDelayDays),
            discoveredAtMillis = existingWord?.discoveredAtMillis
                ?.takeIf { it > 0L }
                ?: System.currentTimeMillis()
        )

        val updatedWords = progress.words
            .filterNot { it.key == wordKey }
            .plus(updatedWord)
            .sortedByDescending { it.lastPractisedDay }

        val answerXp = if (result == LessonAnswerResult.CORRECT) XP_CORRECT_ANSWER else 0
        val discoveryXp = if (isNewWord) XP_NEW_WORD else 0
        val totalAward = answerXp + discoveryXp

        val updatedProgress = awardXp(
            progress.copy(
                totalCorrectAnswers = progress.totalCorrectAnswers +
                        if (result == LessonAnswerResult.CORRECT) 1 else 0,
                totalAnswers = progress.totalAnswers + 1,
                words = updatedWords
            ),
            totalAward
        )

        _uiState.update {
            it.copy(
                selectedLessonAnswer = answer,
                lessonAnswerResult = result,
                lessonXpEarned = it.lessonXpEarned + totalAward,
                lessonNewWords = it.lessonNewWords + if (isNewWord) 1 else 0,
                playerProgress = updatedProgress,
                recentXpAward = totalAward,
                xpAnimationToken = if (totalAward > 0) {
                    it.xpAnimationToken + 1L
                } else {
                    it.xpAnimationToken
                },
                errorMessage = null
            )
        }
        savePlayerProgress(updatedProgress)
    }

    private fun continueLesson() {
        val current = _uiState.value
        val detection = current.selectedDetection ?: return
        val answerResult = current.lessonAnswerResult ?: return

        val nextAnsweredCount = (current.lessonAnsweredCount + 1)
            .coerceAtMost(current.lessonTargetCount)
        val nextCorrectCount = current.lessonCorrectCount +
                if (answerResult == LessonAnswerResult.CORRECT) 1 else 0
        val completedDetections = (
                current.completedLessonDetections + detection
                ).distinctBy(::detectionKey)
        val lessonComplete = current.lessonTargetCount > 0 &&
                nextAnsweredCount >= current.lessonTargetCount
        val completionBonus = if (lessonComplete) XP_LESSON_COMPLETE else 0
        val updatedProgress = if (lessonComplete) {
            awardXp(
                current.playerProgress.copy(
                    lessonsCompleted = current.playerProgress.lessonsCompleted + 1
                ),
                completionBonus
            )
        } else {
            current.playerProgress
        }

        _uiState.update {
            it.copy(
                selectedDetection = null,
                overlappingDetections = emptyList(),
                vocabulary = null,
                lessonAnsweredCount = nextAnsweredCount,
                lessonCorrectCount = nextCorrectCount,
                lessonOptions = emptyList(),
                selectedLessonAnswer = null,
                lessonAnswerResult = null,
                completedLessonDetections = completedDetections,
                isLessonComplete = lessonComplete,
                lessonXpEarned = it.lessonXpEarned + completionBonus,
                playerProgress = updatedProgress,
                recentXpAward = completionBonus,
                xpAnimationToken = if (completionBonus > 0) {
                    it.xpAnimationToken + 1L
                } else {
                    it.xpAnimationToken
                },
                isGenerating = false,
                errorMessage = null
            )
        }

        if (lessonComplete) {
            savePlayerProgress(updatedProgress)
        }
    }

    private fun restartLesson() {
        generationJob?.cancel()
        generationJob = null

        _uiState.update {
            it.copy(
                selectedDetection = null,
                overlappingDetections = emptyList(),
                vocabulary = null,
                lessonTargetCount = minOf(LESSON_QUESTION_LIMIT, it.detections.size),
                lessonAnsweredCount = 0,
                lessonCorrectCount = 0,
                lessonOptions = emptyList(),
                selectedLessonAnswer = null,
                lessonAnswerResult = null,
                completedLessonDetections = emptyList(),
                isLessonComplete = false,
                lessonXpEarned = 0,
                lessonNewWords = 0,
                recentXpAward = 0,
                isGenerating = false,
                errorMessage = null
            )
        }
    }

    private fun changeMode(mode: AppMode) {
        generationJob?.cancel()
        _uiState.update {
            it.copy(
                appMode = mode,
                selectedDetection = null,
                vocabulary = null,
                lessonTargetCount = if (mode == AppMode.LEARN) {
                    minOf(LESSON_QUESTION_LIMIT, it.detections.size)
                } else {
                    it.lessonTargetCount
                },
                lessonAnsweredCount = if (mode == AppMode.LEARN) 0 else it.lessonAnsweredCount,
                lessonCorrectCount = if (mode == AppMode.LEARN) 0 else it.lessonCorrectCount,
                lessonOptions = emptyList(),
                selectedLessonAnswer = null,
                lessonAnswerResult = null,
                completedLessonDetections = if (mode == AppMode.LEARN) {
                    emptyList()
                } else {
                    it.completedLessonDetections
                },
                isLessonComplete = false,
                lessonXpEarned = if (mode == AppMode.LEARN) 0 else it.lessonXpEarned,
                lessonNewWords = if (mode == AppMode.LEARN) 0 else it.lessonNewWords,
                recentXpAward = if (mode == AppMode.LEARN) 0 else it.recentXpAward,
                isGenerating = false,
                selectedTrainingAnnotationId = null,
                trainingMessage = null,
                targetWord = if (mode == AppMode.TEACH) pickRandomTargetWord() else null
            )
        }
    }

    private fun pickRandomTargetWord(): String {
        val words = listOf("stapler", "calculator", "bottle", "keyboard", "mouse", "lamp", "chair")
        return words.random()
    }


    private fun addDetectionAnnotation(detection: DetectionResult) {
        val existing = _uiState.value.editableTrainingAnnotations.firstOrNull { annotation ->
            annotation.originalLabel == detection.label &&
                    annotation.originalConfidence == detection.score &&
                    boxesNearlyEqual(annotation.boundingBox, detection.boundingBox)
        }

        if (existing != null) {
            _uiState.update { it.copy(selectedTrainingAnnotationId = existing.id) }
            return
        }

        val annotation = EditableTrainingAnnotation(
            label = detection.label,
            boundingBox = RectF(detection.boundingBox),
            originalLabel = detection.label,
            originalConfidence = detection.score,
            isConfirmed = false
        )

        _uiState.update { state ->
            state.copy(
                editableTrainingAnnotations = state.editableTrainingAnnotations + annotation,
                selectedTrainingAnnotationId = annotation.id,
                trainingMessage = null
            )
        }
    }

    private fun boxesNearlyEqual(first: RectF, second: RectF, tolerance: Float = 1f): Boolean =
        kotlin.math.abs(first.left - second.left) <= tolerance &&
                kotlin.math.abs(first.top - second.top) <= tolerance &&
                kotlin.math.abs(first.right - second.right) <= tolerance &&
                kotlin.math.abs(first.bottom - second.bottom) <= tolerance

    private fun addManualAnnotation(left: Float, top: Float, right: Float, bottom: Float) {
        val bitmap = _uiState.value.bitmap ?: return
        val newAnnotation = EditableTrainingAnnotation(
            label = "",
            boundingBox = RectF(
                left * bitmap.width,
                top * bitmap.height,
                right * bitmap.width,
                bottom * bitmap.height
            ),
            originalLabel = null,
            originalConfidence = null,
            isConfirmed = false
        )

        _uiState.update { state ->
            state.copy(
                editableTrainingAnnotations = state.editableTrainingAnnotations + newAnnotation,
                selectedTrainingAnnotationId = newAnnotation.id,
                trainingMessage = null
            )
        }
    }

    private fun updateAnnotationBox(id: String, left: Float, top: Float, right: Float, bottom: Float) {
        val bitmap = _uiState.value.bitmap ?: return
        _uiState.update { state ->
            state.copy(
                editableTrainingAnnotations = state.editableTrainingAnnotations.map {
                    if (it.id == id) {
                        it.copy(
                            boundingBox = RectF(
                                left * bitmap.width,
                                top * bitmap.height,
                                right * bitmap.width,
                                bottom * bitmap.height
                            )
                        )
                    } else it
                }
            )
        }
    }

    private fun selectTrainingAnnotation(annotationId: String) {
        _uiState.update { it.copy(selectedTrainingAnnotationId = annotationId) }
    }

    private fun updateTrainingLabel(annotationId: String, label: String) {
        _uiState.update { state ->
            state.copy(
                editableTrainingAnnotations = state.editableTrainingAnnotations.map { annotation ->
                    if (annotation.id == annotationId) {
                        annotation.copy(label = label, isConfirmed = false)
                    } else annotation
                }
            )
        }
    }

    private fun confirmTrainingAnnotation(annotationId: String) {
        val annotation = _uiState.value.editableTrainingAnnotations.firstOrNull { it.id == annotationId } ?: return
        if (annotation.label.trim().isBlank()) {
            _uiState.update { it.copy(trainingMessage = "The object label cannot be blank.") }
            return
        }

        _uiState.update { state ->
            state.copy(
                editableTrainingAnnotations = state.editableTrainingAnnotations.map {
                    if (it.id == annotationId) {
                        it.copy(label = it.label.trim().lowercase(), isConfirmed = true)
                    } else it
                },
                selectedTrainingAnnotationId = null,
                trainingMessage = "Object confirmed."
            )
        }
    }

    private fun deleteTrainingAnnotation(annotationId: String) {
        _uiState.update { state ->
            state.copy(
                editableTrainingAnnotations = state.editableTrainingAnnotations.filterNot { it.id == annotationId },
                selectedTrainingAnnotationId = null,
                trainingMessage = "Detection removed."
            )
        }
    }

    private fun saveTrainingExample() {
        val state = _uiState.value
        val bitmap = state.bitmap ?: return
        val confirmed = state.editableTrainingAnnotations.filter { it.isConfirmed && it.label.isNotBlank() }

        if (confirmed.isEmpty()) {
            _uiState.update { it.copy(trainingMessage = "Confirm at least one object first.") }
            return
        }

        trainingJob?.cancel()
        trainingJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSavingTrainingExample = true, trainingMessage = null) }

                val annotations = confirmed.map { annotation ->
                    val box = annotation.boundingBox
                    TrainingAnnotation(
                        label = annotation.label.trim().lowercase(),
                        left = (box.left / bitmap.width).coerceIn(0f, 1f),
                        top = (box.top / bitmap.height).coerceIn(0f, 1f),
                        right = (box.right / bitmap.width).coerceIn(0f, 1f),
                        bottom = (box.bottom / bitmap.height).coerceIn(0f, 1f),
                        originalLabel = annotation.originalLabel,
                        originalConfidence = annotation.originalConfidence
                    )
                }

                trainingRepository.saveExample(bitmap, annotations)
                val count = trainingRepository.getExampleCount()

                _uiState.update {
                    it.copy(
                        isSavingTrainingExample = false,
                        trainingExampleCount = count,
                        trainingMessage = "Training example saved.",
                        editableTrainingAnnotations = emptyList(), // Reset for next image
                        selectedImageUri = null,
                        bitmap = null,
                        targetWord = if (state.appMode == AppMode.TEACH) pickRandomTargetWord() else null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingTrainingExample = false,
                        trainingMessage = exception.message ?: "Could not save the training example."
                    )
                }
            }
        }
    }

    private fun exportTrainingDataset(destination: Uri) {
        trainingJob?.cancel()
        trainingJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExportingTrainingDataset = true, trainingMessage = null) }
                trainingExportManager.export(destination)
                _uiState.update {
                    it.copy(isExportingTrainingDataset = false, trainingMessage = "Training dataset exported.")
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isExportingTrainingDataset = false,
                        trainingMessage = exception.message ?: "Could not export the training dataset."
                    )
                }
            }
        }
    }

    private fun uploadTrainingDataset() {
        if (_uiState.value.trainingExampleCount <= 0) {
            _uiState.update { it.copy(datasetUploadMessage = "Save at least one training example first.") }
            return
        }

        trainingJob?.cancel()
        trainingJob = viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isUploadingTrainingDataset = true, datasetUploadMessage = "Preparing and uploading dataset…")
                }

                val result = datasetUploadManager.uploadDataset()
                _uiState.update {
                    it.copy(
                        isUploadingTrainingDataset = false,
                        datasetUploadMessage = buildString {
                            append(result.message)
                            result.uploadId?.takeIf(String::isNotBlank)?.let {
                                append(" Upload ID: ")
                                append(it)
                            }
                        }
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isUploadingTrainingDataset = false,
                        datasetUploadMessage = exception.message ?: "Could not upload the training dataset."
                    )
                }
            }
        }
    }

    private fun detectionKey(detection: DetectionResult): String = buildString {
        append(detection.label.trim().lowercase(Locale.ROOT))
        append('|')
        append(detection.boundingBox.left.toInt())
        append('|')
        append(detection.boundingBox.top.toInt())
        append('|')
        append(detection.boundingBox.right.toInt())
        append('|')
        append(detection.boundingBox.bottom.toInt())
    }

    private fun createLessonOptions(correctAnswer: String): List<String> {
        val cleanedAnswer = correctAnswer.trim()
        val correctConcept = answerConceptKey(cleanedAnswer)

        val distractors = SPANISH_DISTRACTORS
            .shuffled()
            .filter { answerConceptKey(it) != correctConcept }
            .distinctBy(::answerConceptKey)
            .take(LESSON_OPTION_COUNT - 1)

        return (distractors + cleanedAnswer).shuffled()
    }

    private fun answersMatch(first: String, second: String): Boolean =
        normalizeAnswer(first) == normalizeAnswer(second)

    private fun answerConceptKey(answer: String): String {
        val normalized = normalizeAnswer(answer)
        val words = normalized.split(' ').filter(String::isNotBlank)
        return if (words.firstOrNull() in SPANISH_ARTICLES) {
            words.drop(1).joinToString(" ")
        } else {
            words.joinToString(" ")
        }
    }

    private fun normalizeAnswer(answer: String): String = Normalizer
        .normalize(answer.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(COMBINING_MARKS_REGEX, "")
        .replace(NON_WORD_REGEX, " ")
        .trim()
        .replace(MULTIPLE_SPACES_REGEX, " ")

    private fun refreshTrainingExampleCount() {
        viewModelScope.launch {
            val count = runCatching { trainingRepository.getExampleCount() }.getOrDefault(0)
            _uiState.update { it.copy(trainingExampleCount = count) }
        }
    }

    private fun savePlayerProgress(progress: PlayerProgress) {
        runCatching { playerProgressStore.save(progress) }
    }

    private fun awardXp(progress: PlayerProgress, amount: Int): PlayerProgress {
        val today = DayKeys.today()
        val normalized = progress.normalizedForToday(today)
        if (amount <= 0) return normalized

        val nextDailyXp = normalized.dailyXp + amount
        val reachedGoalNow = normalized.dailyXp < normalized.dailyGoal &&
                nextDailyXp >= normalized.dailyGoal

        val nextStreak = if (reachedGoalNow) {
            when (normalized.lastGoalDay) {
                today -> normalized.currentStreak
                DayKeys.yesterday() -> normalized.currentStreak + 1
                else -> 1
            }
        } else {
            normalized.currentStreak
        }

        return normalized.copy(
            totalXp = normalized.totalXp + amount,
            dailyXp = nextDailyXp,
            dailyXpDay = today,
            currentStreak = nextStreak,
            longestStreak = maxOf(normalized.longestStreak, nextStreak),
            lastGoalDay = if (reachedGoalNow) today else normalized.lastGoalDay
        )
    }

    private fun reviewDelayDays(masteryPoints: Int): Int = when {
        masteryPoints >= 8 -> 7
        masteryPoints >= 5 -> 4
        masteryPoints >= 4 -> 2
        else -> 1
    }

    private companion object {
        const val LESSON_QUESTION_LIMIT = 5
        const val LESSON_OPTION_COUNT = 4
        const val XP_CORRECT_ANSWER = 10
        const val XP_NEW_WORD = 10
        const val XP_LESSON_COMPLETE = 20
        const val MASTERY_GAIN_CORRECT = 2
        const val MASTERY_LOSS_INCORRECT = 1
        const val MAX_MASTERY_POINTS = 12

        val COMBINING_MARKS_REGEX = "\\p{Mn}+".toRegex()
        val NON_WORD_REGEX = "[^\\p{L}\\p{N}]+".toRegex()
        val MULTIPLE_SPACES_REGEX = "\\s+".toRegex()

        val SPANISH_ARTICLES = setOf(
            "el", "la", "los", "las", "un", "una", "unos", "unas"
        )

        val SPANISH_DISTRACTORS = listOf(
            "la mesa",
            "la silla",
            "la botella",
            "el libro",
            "la taza",
            "la puerta",
            "la ventana",
            "el teléfono",
            "el teclado",
            "el ratón",
            "la lámpara",
            "la mochila",
            "el zapato",
            "el reloj",
            "la cuchara",
            "el plato",
            "el ordenador",
            "el sofá",
            "la cama",
            "la bolsa",
            "el vaso",
            "la lata",
            "la caja",
            "el bolígrafo",
            "la pantalla",
            "la chaqueta",
            "el sombrero",
            "la bicicleta",
            "el coche",
            "la planta"
        )
    }

    override fun onCleared() {
        imageLoadingJob?.cancel()
        detectionJob?.cancel()
        generationJob?.cancel()
        trainingJob?.cancel()
        super.onCleared()
    }
}