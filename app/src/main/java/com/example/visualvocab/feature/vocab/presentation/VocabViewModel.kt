package com.example.visualvocab.feature.vocab.presentation

import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visualvocab.core.image.BitmapLoader
import com.example.visualvocab.data.datasetupload.DatasetUploadManager
import com.example.visualvocab.data.progress.DailyQuestProgress
import com.example.visualvocab.data.progress.DayKeys
import com.example.visualvocab.data.progress.PlayerProgress
import com.example.visualvocab.data.progress.PlayerProgressStore
import com.example.visualvocab.data.progress.WordProgress
import com.example.visualvocab.data.training.TrainingExportManager
import com.example.visualvocab.domain.model.AppMode
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.Vocabulary
import com.example.visualvocab.domain.model.training.TrainingAnnotation
import com.example.visualvocab.domain.repository.TrainingRepository
import com.example.visualvocab.domain.usecase.DetectObjectsUseCase
import com.example.visualvocab.domain.usecase.GenerateVocabularyUseCase
import com.example.visualvocab.domain.usecase.RegenerateSentenceUseCase
import java.text.Normalizer
import java.util.Locale
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
            is VocabUiEvent.OverlappingDetectionsChanged -> {
                _uiState.update { it.copy(overlappingDetections = event.detections) }
            }

            VocabUiEvent.RegenerateSentence -> regenerateSentence()
            VocabUiEvent.MakeSentenceEasier -> changeDifficulty(false)
            VocabUiEvent.MakeSentenceHarder -> changeDifficulty(true)
            VocabUiEvent.ClearSelectedObject -> clearSelectedObject()
            is VocabUiEvent.LessonAnswerSelected -> selectLessonAnswer(event.answer)
            VocabUiEvent.ContinueLesson -> continueLesson()
            VocabUiEvent.RetryLessonQuestion -> retryLessonQuestion()
            VocabUiEvent.RestartLesson -> restartLesson()

            VocabUiEvent.StartReview -> startReview()
            is VocabUiEvent.StartWordReview -> startReview(event.wordKey)
            VocabUiEvent.ContinueReview -> continueReview()
            VocabUiEvent.RestartReview -> restartReview()
            VocabUiEvent.EndReview -> endReview()

            is VocabUiEvent.UpdateSavedWord -> updateSavedWord(
                wordKey = event.wordKey,
                english = event.english,
                spanish = event.spanish
            )
            is VocabUiEvent.DeleteSavedWord -> deleteSavedWord(event.wordKey)
            VocabUiEvent.CompleteOnboarding -> completeOnboarding()

            is VocabUiEvent.ChangeMode -> changeMode(event.mode)

            is VocabUiEvent.ChangeAnnotationTool -> {
                _uiState.update {
                    it.copy(
                        annotationTool = event.tool,
                        selectedTrainingAnnotationId = null
                    )
                }
            }
            is VocabUiEvent.AddDetectionAnnotation -> addDetectionAnnotation(event.detection)
            is VocabUiEvent.AddManualAnnotation -> addManualAnnotation(
                event.left,
                event.top,
                event.right,
                event.bottom
            )
            is VocabUiEvent.UpdateAnnotationBox -> updateAnnotationBox(
                event.id,
                event.left,
                event.top,
                event.right,
                event.bottom
            )
            is VocabUiEvent.SelectTrainingAnnotation -> selectTrainingAnnotation(event.annotationId)
            VocabUiEvent.DismissTrainingAnnotation -> {
                _uiState.update { it.copy(selectedTrainingAnnotationId = null) }
            }
            is VocabUiEvent.UpdateTrainingLabel -> updateTrainingLabel(
                event.annotationId,
                event.label
            )
            is VocabUiEvent.ConfirmTrainingAnnotation -> confirmTrainingAnnotation(event.annotationId)
            is VocabUiEvent.DeleteTrainingAnnotation -> deleteTrainingAnnotation(event.annotationId)
            VocabUiEvent.SaveTrainingExample -> saveTrainingExample()
            is VocabUiEvent.ExportTrainingDataset -> exportTrainingDataset(event.destination)
            VocabUiEvent.ClearTrainingMessage -> {
                _uiState.update { it.copy(trainingMessage = null) }
            }

            VocabUiEvent.UploadTrainingDataset -> uploadTrainingDataset()
            VocabUiEvent.ClearDatasetUploadMessage -> {
                _uiState.update { it.copy(datasetUploadMessage = null) }
            }
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
                        errorMessage = exception.message
                            ?.takeIf(String::isNotBlank)
                            ?: "Failed to load image."
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
                lessonTargetDetection = null,
                overlappingDetections = emptyList(),
                vocabulary = null,
                learningSessionMode = LearningSessionMode.SCAN,
                lessonQuestionType = LessonQuestionType.ENGLISH_TO_SPANISH,
                lessonPrompt = "",
                lessonCorrectAnswer = "",
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
                reviewQueue = emptyList(),
                reviewCurrentIndex = 0,
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
                            errorMessage = if (distinctDetections.isEmpty()) {
                                "No objects were detected."
                            } else {
                                null
                            }
                        )
                    }
                    prepareAutomaticScanQuestion()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isDetecting = false,
                        errorMessage = exception.message
                            ?.takeIf(String::isNotBlank)
                            ?: "Object detection failed."
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

        if (current.learningSessionMode != LearningSessionMode.SCAN) return

        if (
            current.lessonQuestionType == LessonQuestionType.TAP_OBJECT &&
            current.lessonTargetDetection != null &&
            current.vocabulary != null &&
            current.lessonAnswerResult == null
        ) {
            evaluateTapObjectAnswer(detection)
            return
        }

        if (
            current.isLessonComplete ||
            current.isGenerating ||
            current.hasActiveQuestion ||
            current.completedLessonDetections.any {
                detectionKey(it) == detectionKey(detection)
            }
        ) {
            return
        }

        val questionType = questionTypeForScanIndex(
            index = current.lessonAnsweredCount,
            detectionCount = current.detections.size
        )

        // Image-wide questions choose their target before the learner taps anything.
        if (questionType == LessonQuestionType.TAP_OBJECT) {
            prepareAutomaticScanQuestion()
            return
        }

        startScanQuestion(
            detection = detection,
            questionType = questionType
        )
    }

    private fun startScanQuestion(
        detection: DetectionResult,
        questionType: LessonQuestionType
    ) {
        generationJob?.cancel()
        val word = detection.label.trim().lowercase(Locale.ROOT)
        if (word.isBlank()) return

        _uiState.update {
            it.copy(
                selectedDetection = if (questionType == LessonQuestionType.TAP_OBJECT) {
                    null
                } else {
                    detection
                },
                lessonTargetDetection = detection,
                overlappingDetections = emptyList(),
                vocabulary = null,
                sentenceDifficulty = SentenceDifficulty.MEDIUM,
                lessonQuestionType = questionType,
                lessonPrompt = "",
                lessonCorrectAnswer = "",
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
                    val active = _uiState.value
                    if (
                        active.lessonTargetDetection == null ||
                        detectionKey(active.lessonTargetDetection) != detectionKey(detection)
                    ) {
                        return@collect
                    }

                    val english = vocabulary.englishWord.trim()
                    val spanish = vocabulary.spanishWord.trim()
                    if (english.isBlank() || spanish.isBlank()) {
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                errorMessage = "Vivi could not create a translation for this object."
                            )
                        }
                        return@collect
                    }

                    val question = createQuestion(
                        type = questionType,
                        vocabulary = vocabulary,
                        progress = active.playerProgress
                    )

                    _uiState.update {
                        it.copy(
                            vocabulary = vocabulary,
                            selectedDetection = if (
                                questionType == LessonQuestionType.TAP_OBJECT
                            ) null else detection,
                            lessonPrompt = question.prompt,
                            lessonCorrectAnswer = question.correctAnswer,
                            lessonOptions = question.options,
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

    private fun prepareAutomaticScanQuestion() {
        val current = _uiState.value
        if (
            current.appMode != AppMode.LEARN ||
            current.learningSessionMode != LearningSessionMode.SCAN ||
            current.isLessonComplete ||
            current.isGenerating ||
            current.hasActiveQuestion
        ) {
            return
        }

        val questionType = questionTypeForScanIndex(
            index = current.lessonAnsweredCount,
            detectionCount = current.detections.size
        )
        if (questionType != LessonQuestionType.TAP_OBJECT) return

        val remainingDetections = current.detections.filterNot { candidate ->
            current.completedLessonDetections.any { completed ->
                detectionKey(candidate) == detectionKey(completed)
            }
        }

        val target = remainingDetections.maxByOrNull { it.score } ?: return
        startScanQuestion(
            detection = target,
            questionType = LessonQuestionType.TAP_OBJECT
        )
    }

    private fun retryLessonQuestion() {
        val current = _uiState.value
        if (
            current.learningSessionMode != LearningSessionMode.SCAN ||
            current.isLessonComplete ||
            current.isGenerating
        ) {
            return
        }

        val target = current.lessonTargetDetection ?: current.selectedDetection
        if (target != null) {
            startScanQuestion(target, current.lessonQuestionType)
        } else {
            prepareAutomaticScanQuestion()
        }
    }

    private fun changeDifficulty(makeHarder: Boolean) {
        val current = _uiState.value
        val vocabulary = current.vocabulary ?: return
        val difficulty = if (makeHarder) {
            current.sentenceDifficulty.harder()
        } else {
            current.sentenceDifficulty.easier()
        }

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

    private fun regenerateSentence(
        word: String,
        previousSentence: String,
        difficulty: SentenceDifficulty
    ) {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
                regenerateSentenceUseCase(
                    word = word,
                    previousSentence = previousSentence,
                    difficulty = difficulty
                ).collect { vocabulary ->
                    if (!_uiState.value.hasActiveQuestion) return@collect
                    _uiState.update {
                        it.copy(
                            vocabulary = vocabulary,
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
                            ?: "Sentence generation failed."
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
                lessonTargetDetection = null,
                overlappingDetections = emptyList(),
                vocabulary = null,
                lessonPrompt = "",
                lessonCorrectAnswer = "",
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
            current.lessonQuestionType == LessonQuestionType.TAP_OBJECT ||
            current.lessonAnswerResult != null ||
            current.isLessonComplete ||
            answer !in current.lessonOptions
        ) {
            return
        }

        submitAnswer(
            selectedAnswer = answer,
            isCorrect = answersMatch(answer, current.lessonCorrectAnswer),
            vocabulary = vocabulary
        )
    }

    private fun evaluateTapObjectAnswer(detection: DetectionResult) {
        val current = _uiState.value
        val vocabulary = current.vocabulary ?: return
        val target = current.lessonTargetDetection ?: return

        submitAnswer(
            selectedAnswer = detection.label,
            isCorrect = detectionKey(detection) == detectionKey(target),
            vocabulary = vocabulary,
            selectedDetection = detection
        )
    }

    private fun submitAnswer(
        selectedAnswer: String,
        isCorrect: Boolean,
        vocabulary: Vocabulary,
        selectedDetection: DetectionResult? = _uiState.value.selectedDetection
    ) {
        val current = _uiState.value
        if (current.lessonAnswerResult != null || current.isLessonComplete) return

        val result = if (isCorrect) {
            LessonAnswerResult.CORRECT
        } else {
            LessonAnswerResult.INCORRECT
        }

        val answerUpdate = updateProgressForAnswer(
            progress = current.playerProgress,
            vocabulary = vocabulary,
            correct = isCorrect,
            sessionMode = current.learningSessionMode
        )

        _uiState.update {
            it.copy(
                selectedDetection = selectedDetection,
                selectedLessonAnswer = selectedAnswer,
                lessonAnswerResult = result,
                lessonXpEarned = it.lessonXpEarned + answerUpdate.xpAward,
                lessonNewWords = it.lessonNewWords + if (answerUpdate.isNewWord) 1 else 0,
                playerProgress = answerUpdate.progress,
                recentXpAward = answerUpdate.xpAward,
                xpAnimationToken = if (answerUpdate.xpAward > 0) {
                    it.xpAnimationToken + 1L
                } else {
                    it.xpAnimationToken
                },
                errorMessage = null
            )
        }
        savePlayerProgress(answerUpdate.progress)
    }

    private fun continueLesson() {
        val current = _uiState.value
        if (current.learningSessionMode != LearningSessionMode.SCAN) return
        val answerResult = current.lessonAnswerResult ?: return
        val targetDetection = current.lessonTargetDetection ?: current.selectedDetection ?: return

        val nextAnsweredCount = (current.lessonAnsweredCount + 1)
            .coerceAtMost(current.lessonTargetCount)
        val nextCorrectCount = current.lessonCorrectCount +
                if (answerResult == LessonAnswerResult.CORRECT) 1 else 0
        val completedDetections = (current.completedLessonDetections + targetDetection)
            .distinctBy(::detectionKey)
        val lessonComplete = current.lessonTargetCount > 0 &&
                nextAnsweredCount >= current.lessonTargetCount
        val completionBonus = if (lessonComplete) XP_LESSON_COMPLETE else 0

        var updatedProgress = current.playerProgress
        if (lessonComplete) {
            val perfect = nextCorrectCount == current.lessonTargetCount
            val quests = updatedProgress.dailyQuests.normalized().copy(
                perfectLessons = updatedProgress.dailyQuests.normalized().perfectLessons +
                        if (perfect) 1 else 0
            )
            updatedProgress = awardXp(
                updatedProgress.copy(
                    lessonsCompleted = updatedProgress.lessonsCompleted + 1,
                    dailyQuests = quests
                ),
                completionBonus
            )
        }

        _uiState.update {
            it.copy(
                selectedDetection = null,
                lessonTargetDetection = null,
                overlappingDetections = emptyList(),
                vocabulary = null,
                lessonPrompt = "",
                lessonCorrectAnswer = "",
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
        } else {
            prepareAutomaticScanQuestion()
        }
    }

    private fun restartLesson() {
        generationJob?.cancel()
        generationJob = null

        _uiState.update {
            it.copy(
                learningSessionMode = LearningSessionMode.SCAN,
                selectedDetection = null,
                lessonTargetDetection = null,
                overlappingDetections = emptyList(),
                vocabulary = null,
                lessonQuestionType = LessonQuestionType.ENGLISH_TO_SPANISH,
                lessonPrompt = "",
                lessonCorrectAnswer = "",
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
                reviewQueue = emptyList(),
                reviewCurrentIndex = 0,
                recentXpAward = 0,
                isGenerating = false,
                errorMessage = null
            )
        }
    }

    private fun startReview(wordKey: String? = null) {
        generationJob?.cancel()
        val progress = _uiState.value.playerProgress.normalizedForToday()
        val queue = when {
            !wordKey.isNullOrBlank() -> progress.words.filter { it.key == wordKey }
            else -> {
                val due = progress.words
                    .filter { it.isDue() }
                    .sortedWith(compareBy<WordProgress> { it.nextReviewDay }.thenBy { it.masteryPoints })
                (if (due.isNotEmpty()) due else progress.words.sortedBy { it.masteryPoints })
                    .take(REVIEW_QUESTION_LIMIT)
            }
        }

        if (queue.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Discover a word before starting a review.")
            }
            return
        }

        _uiState.update {
            it.copy(
                learningSessionMode = LearningSessionMode.REVIEW,
                selectedDetection = null,
                lessonTargetDetection = null,
                overlappingDetections = emptyList(),
                vocabulary = null,
                lessonQuestionType = LessonQuestionType.ENGLISH_TO_SPANISH,
                lessonPrompt = "",
                lessonCorrectAnswer = "",
                lessonTargetCount = queue.size,
                lessonAnsweredCount = 0,
                lessonCorrectCount = 0,
                lessonOptions = emptyList(),
                selectedLessonAnswer = null,
                lessonAnswerResult = null,
                completedLessonDetections = emptyList(),
                isLessonComplete = false,
                lessonXpEarned = 0,
                lessonNewWords = 0,
                reviewQueue = queue,
                reviewCurrentIndex = 0,
                recentXpAward = 0,
                errorMessage = null
            )
        }
        prepareReviewQuestion(0)
    }

    private fun prepareReviewQuestion(index: Int) {
        val state = _uiState.value
        val word = state.reviewQueue.getOrNull(index) ?: return
        val type = if (index % 2 == 0) {
            LessonQuestionType.ENGLISH_TO_SPANISH
        } else {
            LessonQuestionType.SPANISH_TO_ENGLISH
        }
        val vocabulary = Vocabulary(
            englishWord = word.english,
            spanishWord = word.spanish,
            englishSentence = "",
            spanishSentence = ""
        )
        val question = createQuestion(type, vocabulary, state.playerProgress)

        _uiState.update {
            it.copy(
                vocabulary = vocabulary,
                lessonQuestionType = type,
                lessonPrompt = question.prompt,
                lessonCorrectAnswer = question.correctAnswer,
                lessonOptions = question.options,
                selectedLessonAnswer = null,
                lessonAnswerResult = null,
                isGenerating = false,
                errorMessage = null
            )
        }
    }

    private fun continueReview() {
        val current = _uiState.value
        if (current.learningSessionMode != LearningSessionMode.REVIEW) return
        val answerResult = current.lessonAnswerResult ?: return

        val nextAnswered = current.lessonAnsweredCount + 1
        val nextCorrect = current.lessonCorrectCount +
                if (answerResult == LessonAnswerResult.CORRECT) 1 else 0
        val complete = nextAnswered >= current.lessonTargetCount

        if (complete) {
            val quests = current.playerProgress.dailyQuests.normalized().let {
                it.copy(reviewsCompleted = it.reviewsCompleted + 1)
            }
            val updatedProgress = awardXp(
                current.playerProgress.copy(
                    reviewSessionsCompleted = current.playerProgress.reviewSessionsCompleted + 1,
                    dailyQuests = quests
                ),
                XP_REVIEW_COMPLETE
            )

            _uiState.update {
                it.copy(
                    vocabulary = null,
                    lessonPrompt = "",
                    lessonCorrectAnswer = "",
                    lessonOptions = emptyList(),
                    selectedLessonAnswer = null,
                    lessonAnswerResult = null,
                    lessonAnsweredCount = nextAnswered,
                    lessonCorrectCount = nextCorrect,
                    isLessonComplete = true,
                    lessonXpEarned = it.lessonXpEarned + XP_REVIEW_COMPLETE,
                    playerProgress = updatedProgress,
                    recentXpAward = XP_REVIEW_COMPLETE,
                    xpAnimationToken = it.xpAnimationToken + 1L
                )
            }
            savePlayerProgress(updatedProgress)
        } else {
            _uiState.update {
                it.copy(
                    vocabulary = null,
                    lessonPrompt = "",
                    lessonCorrectAnswer = "",
                    lessonOptions = emptyList(),
                    selectedLessonAnswer = null,
                    lessonAnswerResult = null,
                    lessonAnsweredCount = nextAnswered,
                    lessonCorrectCount = nextCorrect,
                    reviewCurrentIndex = current.reviewCurrentIndex + 1
                )
            }
            prepareReviewQuestion(current.reviewCurrentIndex + 1)
        }
    }

    private fun restartReview() {
        val current = _uiState.value
        val key = current.reviewQueue.singleOrNull()?.key
        startReview(key)
    }

    private fun endReview() {
        _uiState.update {
            it.copy(
                learningSessionMode = LearningSessionMode.SCAN,
                vocabulary = null,
                lessonQuestionType = LessonQuestionType.ENGLISH_TO_SPANISH,
                lessonPrompt = "",
                lessonCorrectAnswer = "",
                lessonTargetCount = minOf(LESSON_QUESTION_LIMIT, it.detections.size),
                lessonAnsweredCount = 0,
                lessonCorrectCount = 0,
                lessonOptions = emptyList(),
                selectedLessonAnswer = null,
                lessonAnswerResult = null,
                isLessonComplete = false,
                lessonXpEarned = 0,
                lessonNewWords = 0,
                reviewQueue = emptyList(),
                reviewCurrentIndex = 0,
                recentXpAward = 0,
                errorMessage = null
            )
        }
    }

    private fun updateSavedWord(wordKey: String, english: String, spanish: String) {
        val cleanEnglish = english.trim()
        val cleanSpanish = spanish.trim()
        if (cleanEnglish.isBlank() || cleanSpanish.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Both word fields are required.") }
            return
        }

        val progress = _uiState.value.playerProgress
        val existing = progress.words.firstOrNull { it.key == wordKey } ?: return
        val newKey = normalizeAnswer(cleanEnglish)
        val updatedWord = existing.copy(
            key = newKey,
            english = cleanEnglish,
            spanish = cleanSpanish
        )
        val updated = progress.copy(
            words = progress.words
                .filterNot { it.key == wordKey || (it.key == newKey && it.key != wordKey) }
                .plus(updatedWord)
        ).withUnlockedAchievements()

        _uiState.update { it.copy(playerProgress = updated, errorMessage = null) }
        savePlayerProgress(updated)
    }

    private fun deleteSavedWord(wordKey: String) {
        val progress = _uiState.value.playerProgress
        val updated = progress.copy(
            words = progress.words.filterNot { it.key == wordKey }
        ).withUnlockedAchievements()
        _uiState.update { it.copy(playerProgress = updated, errorMessage = null) }
        savePlayerProgress(updated)
    }

    private fun completeOnboarding() {
        val updated = _uiState.value.playerProgress.copy(
            onboardingCompleted = true
        )
        _uiState.update { it.copy(playerProgress = updated) }
        savePlayerProgress(updated)
    }

    private fun changeMode(mode: AppMode) {
        generationJob?.cancel()
        _uiState.update {
            it.copy(
                appMode = mode,
                learningSessionMode = LearningSessionMode.SCAN,
                selectedDetection = null,
                lessonTargetDetection = null,
                vocabulary = null,
                lessonQuestionType = LessonQuestionType.ENGLISH_TO_SPANISH,
                lessonPrompt = "",
                lessonCorrectAnswer = "",
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
                lessonXpEarned = 0,
                lessonNewWords = 0,
                reviewQueue = emptyList(),
                reviewCurrentIndex = 0,
                isGenerating = false,
                selectedTrainingAnnotationId = null,
                trainingMessage = null,
                targetWord = if (mode == AppMode.TEACH) pickRandomTargetWord() else null
            )
        }
    }

    private fun pickRandomTargetWord(): String {
        val words = listOf(
            "stapler",
            "calculator",
            "bottle",
            "keyboard",
            "mouse",
            "lamp",
            "chair"
        )
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

    private fun boxesNearlyEqual(
        first: RectF,
        second: RectF,
        tolerance: Float = 1f
    ): Boolean =
        kotlin.math.abs(first.left - second.left) <= tolerance &&
                kotlin.math.abs(first.top - second.top) <= tolerance &&
                kotlin.math.abs(first.right - second.right) <= tolerance &&
                kotlin.math.abs(first.bottom - second.bottom) <= tolerance

    private fun addManualAnnotation(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
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

    private fun updateAnnotationBox(
        id: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
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
                    } else {
                        it
                    }
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
                    } else {
                        annotation
                    }
                }
            )
        }
    }

    private fun confirmTrainingAnnotation(annotationId: String) {
        val annotation = _uiState.value.editableTrainingAnnotations
            .firstOrNull { it.id == annotationId }
            ?: return
        if (annotation.label.trim().isBlank()) {
            _uiState.update { it.copy(trainingMessage = "The object label cannot be blank.") }
            return
        }

        _uiState.update { state ->
            state.copy(
                editableTrainingAnnotations = state.editableTrainingAnnotations.map {
                    if (it.id == annotationId) {
                        it.copy(
                            label = it.label.trim().lowercase(Locale.ROOT),
                            isConfirmed = true
                        )
                    } else {
                        it
                    }
                },
                selectedTrainingAnnotationId = null,
                trainingMessage = "Object confirmed."
            )
        }
    }

    private fun deleteTrainingAnnotation(annotationId: String) {
        _uiState.update { state ->
            state.copy(
                editableTrainingAnnotations = state.editableTrainingAnnotations
                    .filterNot { it.id == annotationId },
                selectedTrainingAnnotationId = null,
                trainingMessage = "Detection removed."
            )
        }
    }

    private fun saveTrainingExample() {
        val state = _uiState.value
        val bitmap = state.bitmap ?: return
        val confirmed = state.editableTrainingAnnotations
            .filter { it.isConfirmed && it.label.isNotBlank() }

        if (confirmed.isEmpty()) {
            _uiState.update {
                it.copy(trainingMessage = "Confirm at least one object first.")
            }
            return
        }

        trainingJob?.cancel()
        trainingJob = viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isSavingTrainingExample = true, trainingMessage = null)
                }

                val annotations = confirmed.map { annotation ->
                    val box = annotation.boundingBox
                    TrainingAnnotation(
                        label = annotation.label.trim().lowercase(Locale.ROOT),
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
                        editableTrainingAnnotations = emptyList(),
                        selectedImageUri = null,
                        bitmap = null,
                        targetWord = if (state.appMode == AppMode.TEACH) {
                            pickRandomTargetWord()
                        } else {
                            null
                        }
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingTrainingExample = false,
                        trainingMessage = exception.message
                            ?: "Could not save the training example."
                    )
                }
            }
        }
    }

    private fun exportTrainingDataset(destination: Uri) {
        trainingJob?.cancel()
        trainingJob = viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isExportingTrainingDataset = true, trainingMessage = null)
                }
                trainingExportManager.export(destination)
                _uiState.update {
                    it.copy(
                        isExportingTrainingDataset = false,
                        trainingMessage = "Training dataset exported."
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isExportingTrainingDataset = false,
                        trainingMessage = exception.message
                            ?: "Could not export the training dataset."
                    )
                }
            }
        }
    }

    private fun uploadTrainingDataset() {
        if (_uiState.value.trainingExampleCount <= 0) {
            _uiState.update {
                it.copy(datasetUploadMessage = "Save at least one training example first.")
            }
            return
        }

        trainingJob?.cancel()
        trainingJob = viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isUploadingTrainingDataset = true,
                        datasetUploadMessage = "Preparing and uploading dataset…"
                    )
                }

                val result = datasetUploadManager.uploadDataset()
                _uiState.update {
                    it.copy(
                        isUploadingTrainingDataset = false,
                        datasetUploadMessage = buildString {
                            append(result.message)
                            result.uploadId?.takeIf(String::isNotBlank)?.let { id ->
                                append(" Upload ID: ")
                                append(id)
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
                        datasetUploadMessage = exception.message
                            ?: "Could not upload the training dataset."
                    )
                }
            }
        }
    }

    private data class QuestionDefinition(
        val prompt: String,
        val correctAnswer: String,
        val options: List<String>
    )

    private data class AnswerProgressUpdate(
        val progress: PlayerProgress,
        val xpAward: Int,
        val isNewWord: Boolean
    )

    private fun createQuestion(
        type: LessonQuestionType,
        vocabulary: Vocabulary,
        progress: PlayerProgress
    ): QuestionDefinition = when (type) {
        LessonQuestionType.ENGLISH_TO_SPANISH -> {
            val answer = vocabulary.spanishWord.trim()
            QuestionDefinition(
                prompt = "What is “${vocabulary.englishWord.displayWord()}” in Spanish?",
                correctAnswer = answer,
                options = createOptions(
                    correctAnswer = answer,
                    candidates = progress.words.map { it.spanish } + SPANISH_DISTRACTORS,
                    spanish = true
                )
            )
        }

        LessonQuestionType.SPANISH_TO_ENGLISH -> {
            val answer = vocabulary.englishWord.trim()
            QuestionDefinition(
                prompt = "What does “${vocabulary.spanishWord.displayWord()}” mean?",
                correctAnswer = answer,
                options = createOptions(
                    correctAnswer = answer,
                    candidates = progress.words.map { it.english } + ENGLISH_DISTRACTORS,
                    spanish = false
                )
            )
        }

        LessonQuestionType.TAP_OBJECT -> QuestionDefinition(
            prompt = "Tap the object that means “${vocabulary.spanishWord.displayWord()}”",
            correctAnswer = vocabulary.englishWord.trim(),
            options = emptyList()
        )
    }

    private fun createOptions(
        correctAnswer: String,
        candidates: List<String>,
        spanish: Boolean
    ): List<String> {
        val cleanedAnswer = correctAnswer.trim()
        val key: (String) -> String = if (spanish) ::answerConceptKey else ::normalizeAnswer
        val correctKey = key(cleanedAnswer)
        val distractors = candidates
            .map(String::trim)
            .filter(String::isNotBlank)
            .shuffled()
            .filter { key(it) != correctKey }
            .distinctBy(key)
            .take(LESSON_OPTION_COUNT - 1)
        return (distractors + cleanedAnswer).shuffled()
    }

    private fun questionTypeForScanIndex(
        index: Int,
        detectionCount: Int
    ): LessonQuestionType {
        val requested = when (index % 3) {
            0 -> LessonQuestionType.ENGLISH_TO_SPANISH
            1 -> LessonQuestionType.SPANISH_TO_ENGLISH
            else -> LessonQuestionType.TAP_OBJECT
        }

        return if (requested == LessonQuestionType.TAP_OBJECT && detectionCount < 2) {
            LessonQuestionType.ENGLISH_TO_SPANISH
        } else {
            requested
        }
    }

    private fun updateProgressForAnswer(
        progress: PlayerProgress,
        vocabulary: Vocabulary,
        correct: Boolean,
        sessionMode: LearningSessionMode
    ): AnswerProgressUpdate {
        val today = DayKeys.today()
        val normalizedProgress = progress.normalizedForToday(today)
        val wordKey = normalizeAnswer(vocabulary.englishWord)
        val existingWord = normalizedProgress.words.firstOrNull { it.key == wordKey }
        val isNewWord = existingWord == null
        val previousMastery = existingWord?.masteryPoints ?: 0
        val nextMastery = if (correct) {
            (previousMastery + MASTERY_GAIN_CORRECT).coerceAtMost(MAX_MASTERY_POINTS)
        } else {
            (previousMastery - MASTERY_LOSS_INCORRECT).coerceAtLeast(0)
        }
        val delay = if (correct) reviewDelayDays(nextMastery) else 0

        val updatedWord = WordProgress(
            key = wordKey,
            english = vocabulary.englishWord.trim(),
            spanish = vocabulary.spanishWord.trim(),
            timesSeen = (existingWord?.timesSeen ?: 0) + 1,
            correctAnswers = (existingWord?.correctAnswers ?: 0) + if (correct) 1 else 0,
            incorrectAnswers = (existingWord?.incorrectAnswers ?: 0) + if (correct) 0 else 1,
            masteryPoints = nextMastery,
            lastPractisedDay = today,
            nextReviewDay = DayKeys.daysFromToday(delay),
            discoveredAtMillis = existingWord?.discoveredAtMillis
                ?.takeIf { it > 0L }
                ?: System.currentTimeMillis()
        )

        val nextCorrectStreak = if (correct) {
            normalizedProgress.currentCorrectStreak + 1
        } else {
            0
        }
        val currentQuests = normalizedProgress.dailyQuests.normalized(today)
        val updatedQuests = currentQuests.copy(
            discoveredWords = currentQuests.discoveredWords + if (isNewWord) 1 else 0,
            correctAnswers = currentQuests.correctAnswers + if (correct) 1 else 0
        )

        val base = normalizedProgress.copy(
            totalCorrectAnswers = normalizedProgress.totalCorrectAnswers + if (correct) 1 else 0,
            totalAnswers = normalizedProgress.totalAnswers + 1,
            currentCorrectStreak = nextCorrectStreak,
            bestCorrectStreak = maxOf(normalizedProgress.bestCorrectStreak, nextCorrectStreak),
            words = normalizedProgress.words
                .filterNot { it.key == wordKey }
                .plus(updatedWord)
                .sortedByDescending { it.lastPractisedDay },
            dailyQuests = updatedQuests
        )

        val answerXp = when {
            !correct -> 0
            sessionMode == LearningSessionMode.REVIEW -> XP_REVIEW_CORRECT
            else -> XP_CORRECT_ANSWER
        }
        val discoveryXp = if (isNewWord) XP_NEW_WORD else 0
        val totalAward = answerXp + discoveryXp
        val updatedProgress = awardXp(base, totalAward)

        return AnswerProgressUpdate(
            progress = updatedProgress,
            xpAward = totalAward,
            isNewWord = isNewWord
        )
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

    private fun answersMatch(first: String, second: String): Boolean =
        normalizeAnswer(first) == normalizeAnswer(second) ||
                answerConceptKey(first) == answerConceptKey(second)

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
            val count = runCatching { trainingRepository.getExampleCount() }
                .getOrDefault(0)
            _uiState.update { it.copy(trainingExampleCount = count) }
        }
    }

    private fun savePlayerProgress(progress: PlayerProgress) {
        runCatching { playerProgressStore.save(progress.withUnlockedAchievements()) }
    }

    private fun awardXp(progress: PlayerProgress, amount: Int): PlayerProgress {
        val today = DayKeys.today()
        val normalized = progress.normalizedForToday(today)
        if (amount <= 0) return normalized.withUnlockedAchievements()

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
        ).withUnlockedAchievements()
    }

    private fun reviewDelayDays(masteryPoints: Int): Int = when {
        masteryPoints >= 8 -> 7
        masteryPoints >= 5 -> 4
        masteryPoints >= 4 -> 2
        else -> 1
    }

    private fun String.displayWord(): String = trim().replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase() else character.toString()
    }

    private companion object {
        const val LESSON_QUESTION_LIMIT = 5
        const val REVIEW_QUESTION_LIMIT = 5
        const val LESSON_OPTION_COUNT = 4
        const val XP_CORRECT_ANSWER = 10
        const val XP_REVIEW_CORRECT = 8
        const val XP_NEW_WORD = 10
        const val XP_LESSON_COMPLETE = 20
        const val XP_REVIEW_COMPLETE = 15
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
            "la mesa", "la silla", "la botella", "el libro", "la taza",
            "la puerta", "la ventana", "el teléfono", "el teclado", "el ratón",
            "la lámpara", "la mochila", "el zapato", "el reloj", "la cuchara",
            "el plato", "el ordenador", "el sofá", "la cama", "la bolsa",
            "el vaso", "la lata", "la caja", "el bolígrafo", "la pantalla",
            "la chaqueta", "el sombrero", "la bicicleta", "el coche", "la planta"
        )

        val ENGLISH_DISTRACTORS = listOf(
            "table", "chair", "bottle", "book", "cup", "door", "window",
            "phone", "keyboard", "mouse", "lamp", "backpack", "shoe", "clock",
            "spoon", "plate", "computer", "sofa", "bed", "bag", "glass",
            "can", "box", "pen", "screen", "jacket", "hat", "bicycle", "car",
            "plant"
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
