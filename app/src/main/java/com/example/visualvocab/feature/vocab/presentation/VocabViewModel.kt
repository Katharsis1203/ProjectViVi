package com.example.visualvocab.feature.vocab.presentation

import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visualvocab.core.image.BitmapLoader
import com.example.visualvocab.data.training.TrainingExportManager
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VocabViewModel(
    private val bitmapLoader: BitmapLoader,
    private val detectObjectsUseCase:
    DetectObjectsUseCase,
    private val generateVocabularyUseCase:
    GenerateVocabularyUseCase,
    private val regenerateSentenceUseCase:
    RegenerateSentenceUseCase,
    private val trainingRepository:
    TrainingRepository,
    private val trainingExportManager:
    TrainingExportManager
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(VocabUiState())

    val uiState: StateFlow<VocabUiState> =
        _uiState.asStateFlow()

    private val _uiEffect =
        MutableSharedFlow<VocabUiEffect>()

    val uiEffect: SharedFlow<VocabUiEffect> =
        _uiEffect.asSharedFlow()

    private var imageLoadingJob: Job? = null
    private var detectionJob: Job? = null
    private var generationJob: Job? = null
    private var trainingJob: Job? = null

    init {
        refreshTrainingExampleCount()
    }

    fun onEvent(
        event: VocabUiEvent
    ) {
        when (event) {
            is VocabUiEvent.ImageSelected ->
                handleImageSelected(event.uri)

            VocabUiEvent.AnalyzeImage ->
                analyzeImage()

            is VocabUiEvent.ObjectTapped ->
                handleObjectTapped(
                    event.detection
                )

            is VocabUiEvent
            .OverlappingDetectionsChanged ->
                _uiState.update {
                    it.copy(
                        overlappingDetections =
                            event.detections
                    )
                }

            VocabUiEvent.RegenerateSentence ->
                regenerateSentence()

            VocabUiEvent.MakeSentenceEasier ->
                changeDifficulty(false)

            VocabUiEvent.MakeSentenceHarder ->
                changeDifficulty(true)

            VocabUiEvent.ClearSelectedObject ->
                clearSelectedObject()

            is VocabUiEvent.ChangeMode ->
                changeMode(event.mode)

            is VocabUiEvent
            .SelectTrainingAnnotation ->
                selectTrainingAnnotation(
                    event.annotationId
                )

            VocabUiEvent
                .DismissTrainingAnnotation ->
                _uiState.update {
                    it.copy(
                        selectedTrainingAnnotationId =
                            null
                    )
                }

            is VocabUiEvent.UpdateTrainingLabel ->
                updateTrainingLabel(
                    event.annotationId,
                    event.label
                )

            is VocabUiEvent
            .ConfirmTrainingAnnotation ->
                confirmTrainingAnnotation(
                    event.annotationId
                )

            is VocabUiEvent
            .DeleteTrainingAnnotation ->
                deleteTrainingAnnotation(
                    event.annotationId
                )

            VocabUiEvent.SaveTrainingExample ->
                saveTrainingExample()

            is VocabUiEvent
            .ExportTrainingDataset ->
                exportTrainingDataset(
                    event.destination
                )

            VocabUiEvent.ClearTrainingMessage ->
                _uiState.update {
                    it.copy(
                        trainingMessage = null
                    )
                }
        }
    }

    private fun handleImageSelected(
        uri: Uri
    ) {
        imageLoadingJob?.cancel()
        detectionJob?.cancel()
        generationJob?.cancel()

        val currentMode =
            _uiState.value.appMode

        val currentCount =
            _uiState.value
                .trainingExampleCount

        imageLoadingJob =
            viewModelScope.launch {
                try {
                    _uiState.value =
                        VocabUiState(
                            selectedImageUri = uri,
                            isDetecting = true,
                            appMode = currentMode,
                            trainingExampleCount =
                                currentCount
                        )

                    val bitmap =
                        bitmapLoader
                            .loadFromUri(uri)

                    if (bitmap == null) {
                        _uiState.value =
                            VocabUiState(
                                selectedImageUri =
                                    uri,
                                appMode =
                                    currentMode,
                                trainingExampleCount =
                                    currentCount,
                                errorMessage =
                                    "Failed to load image."
                            )
                        return@launch
                    }

                    _uiState.value =
                        VocabUiState(
                            selectedImageUri = uri,
                            bitmap = bitmap,
                            isDetecting = true,
                            appMode = currentMode,
                            trainingExampleCount =
                                currentCount
                        )

                    analyzeImage()
                } catch (
                    exception:
                    CancellationException
                ) {
                    throw exception
                } catch (exception: Exception) {
                    _uiState.update {
                        it.copy(
                            isDetecting = false,
                            errorMessage =
                                exception.message
                                    ?.takeIf(
                                        String::isNotBlank
                                    )
                                    ?: "Failed to load image."
                        )
                    }
                }
            }
    }

    private fun analyzeImage() {
        val bitmap =
            _uiState.value.bitmap
                ?: return

        detectionJob?.cancel()
        generationJob?.cancel()

        _uiState.update {
            it.copy(
                detections = emptyList(),
                generalLabels = emptyList(),
                selectedDetection = null,
                overlappingDetections =
                    emptyList(),
                vocabulary = null,
                editableTrainingAnnotations =
                    emptyList(),
                selectedTrainingAnnotationId =
                    null,
                isDetecting = true,
                isGenerating = false,
                errorMessage = null,
                trainingMessage = null
            )
        }

        detectionJob =
            viewModelScope.launch {
                try {
                    detectObjectsUseCase(
                        bitmap
                    ).collect { output ->

                        val editable =
                            output.detections.map {
                                    detection ->

                                EditableTrainingAnnotation(
                                    label =
                                        detection.label,
                                    boundingBox =
                                        RectF(
                                            detection
                                                .boundingBox
                                        ),
                                    originalLabel =
                                        detection.label,
                                    originalConfidence =
                                        detection.score
                                )
                            }

                        _uiState.update {
                            it.copy(
                                detections =
                                    output.detections,
                                generalLabels =
                                    output.labels,
                                editableTrainingAnnotations =
                                    editable,
                                isDetecting = false,
                                errorMessage =
                                    if (
                                        output.detections
                                            .isEmpty()
                                    ) {
                                        "No objects were detected."
                                    } else {
                                        null
                                    }
                            )
                        }
                    }
                } catch (
                    exception:
                    CancellationException
                ) {
                    throw exception
                } catch (exception: Exception) {
                    _uiState.update {
                        it.copy(
                            isDetecting = false,
                            errorMessage =
                                exception.message
                                    ?.takeIf(
                                        String::isNotBlank
                                    )
                                    ?: "Object detection failed."
                        )
                    }
                }
            }
    }

    private fun handleObjectTapped(
        detection: DetectionResult
    ) {
        if (
            _uiState.value.appMode ==
            AppMode.TEACH
        ) {
            val index =
                _uiState.value.detections
                    .indexOf(detection)

            val annotation =
                _uiState.value
                    .editableTrainingAnnotations
                    .getOrNull(index)

            if (annotation != null) {
                selectTrainingAnnotation(
                    annotation.id
                )
            }

            return
        }

        generationJob?.cancel()

        val word =
            detection.label
                .trim()
                .lowercase()

        if (word.isBlank()) return

        _uiState.update {
            it.copy(
                selectedDetection =
                    detection,
                overlappingDetections =
                    emptyList(),
                vocabulary = null,
                sentenceDifficulty =
                    SentenceDifficulty.MEDIUM,
                isGenerating = true,
                errorMessage = null
            )
        }

        generationJob =
            viewModelScope.launch {
                try {
                    generateVocabularyUseCase(
                        word = word,
                        difficulty =
                            SentenceDifficulty.MEDIUM
                    ).collect { vocabulary ->
                        if (
                            _uiState.value
                                .selectedDetection !=
                            detection
                        ) {
                            return@collect
                        }

                        _uiState.update {
                            it.copy(
                                vocabulary =
                                    vocabulary,
                                isGenerating =
                                    false,
                                errorMessage = null
                            )
                        }
                    }
                } catch (
                    exception:
                    CancellationException
                ) {
                    throw exception
                } catch (exception: Exception) {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage =
                                exception.message
                                    ?.takeIf(
                                        String::isNotBlank
                                    )
                                    ?: "Vocabulary generation failed."
                        )
                    }
                }
            }
    }

    private fun changeDifficulty(
        makeHarder: Boolean
    ) {
        val current =
            _uiState.value

        val vocabulary =
            current.vocabulary
                ?: return

        val difficulty =
            if (makeHarder) {
                current.sentenceDifficulty
                    .harder()
            } else {
                current.sentenceDifficulty
                    .easier()
            }

        _uiState.update {
            it.copy(
                sentenceDifficulty =
                    difficulty
            )
        }

        regenerateSentence(
            word =
                vocabulary.englishWord,
            previousSentence =
                vocabulary.englishSentence,
            difficulty = difficulty
        )
    }

    private fun regenerateSentence() {
        val state =
            _uiState.value

        val vocabulary =
            state.vocabulary
                ?: return

        regenerateSentence(
            word =
                vocabulary.englishWord,
            previousSentence =
                vocabulary.englishSentence,
            difficulty =
                state.sentenceDifficulty
        )
    }

    private fun regenerateSentence(
        word: String,
        previousSentence: String,
        difficulty:
        SentenceDifficulty
    ) {
        generationJob?.cancel()

        generationJob =
            viewModelScope.launch {
                try {
                    _uiState.update {
                        it.copy(
                            isGenerating = true,
                            errorMessage = null
                        )
                    }

                    regenerateSentenceUseCase(
                        word = word,
                        previousSentence =
                            previousSentence,
                        difficulty = difficulty
                    ).collect { vocabulary ->
                        if (
                            !_uiState.value
                                .hasSelectedObject
                        ) {
                            return@collect
                        }

                        _uiState.update {
                            it.copy(
                                vocabulary =
                                    vocabulary,
                                isGenerating =
                                    false,
                                errorMessage = null
                            )
                        }
                    }
                } catch (
                    exception:
                    CancellationException
                ) {
                    throw exception
                } catch (exception: Exception) {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage =
                                exception.message
                                    ?.takeIf(
                                        String::isNotBlank
                                    )
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
                overlappingDetections =
                    emptyList(),
                vocabulary = null,
                isGenerating = false,
                errorMessage = null
            )
        }
    }

    private fun changeMode(
        mode: AppMode
    ) {
        generationJob?.cancel()

        _uiState.update {
            it.copy(
                appMode = mode,
                selectedDetection = null,
                vocabulary = null,
                isGenerating = false,
                selectedTrainingAnnotationId =
                    null,
                trainingMessage = null
            )
        }
    }

    private fun selectTrainingAnnotation(
        annotationId: String
    ) {
        _uiState.update {
            it.copy(
                selectedTrainingAnnotationId =
                    annotationId
            )
        }
    }

    private fun updateTrainingLabel(
        annotationId: String,
        label: String
    ) {
        _uiState.update { state ->
            state.copy(
                editableTrainingAnnotations =
                    state.editableTrainingAnnotations
                        .map { annotation ->
                            if (
                                annotation.id ==
                                annotationId
                            ) {
                                annotation.copy(
                                    label = label,
                                    isConfirmed =
                                        false
                                )
                            } else {
                                annotation
                            }
                        }
            )
        }
    }

    private fun confirmTrainingAnnotation(
        annotationId: String
    ) {
        val annotation =
            _uiState.value
                .editableTrainingAnnotations
                .firstOrNull {
                    it.id ==
                            annotationId
                }
                ?: return

        if (
            annotation.label
                .trim()
                .isBlank()
        ) {
            _uiState.update {
                it.copy(
                    trainingMessage =
                        "The object label cannot be blank."
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                editableTrainingAnnotations =
                    state.editableTrainingAnnotations
                        .map {
                            if (
                                it.id ==
                                annotationId
                            ) {
                                it.copy(
                                    label =
                                        it.label
                                            .trim()
                                            .lowercase(),
                                    isConfirmed =
                                        true
                                )
                            } else {
                                it
                            }
                        },
                selectedTrainingAnnotationId =
                    null,
                trainingMessage =
                    "Object confirmed."
            )
        }
    }

    private fun deleteTrainingAnnotation(
        annotationId: String
    ) {
        _uiState.update { state ->
            state.copy(
                editableTrainingAnnotations =
                    state.editableTrainingAnnotations
                        .filterNot {
                            it.id ==
                                    annotationId
                        },
                selectedTrainingAnnotationId =
                    null,
                trainingMessage =
                    "Detection removed."
            )
        }
    }

    private fun saveTrainingExample() {
        val state =
            _uiState.value

        val bitmap =
            state.bitmap
                ?: return

        val confirmed =
            state.editableTrainingAnnotations
                .filter {
                    it.isConfirmed &&
                            it.label.isNotBlank()
                }

        if (confirmed.isEmpty()) {
            _uiState.update {
                it.copy(
                    trainingMessage =
                        "Confirm at least one object first."
                )
            }
            return
        }

        trainingJob?.cancel()

        trainingJob =
            viewModelScope.launch {
                try {
                    _uiState.update {
                        it.copy(
                            isSavingTrainingExample =
                                true,
                            trainingMessage = null
                        )
                    }

                    val annotations =
                        confirmed.map {
                                annotation ->

                            val box =
                                annotation.boundingBox

                            TrainingAnnotation(
                                label =
                                    annotation.label
                                        .trim()
                                        .lowercase(),
                                left =
                                    (
                                            box.left /
                                                    bitmap.width
                                            ).coerceIn(
                                            0f,
                                            1f
                                        ),
                                top =
                                    (
                                            box.top /
                                                    bitmap.height
                                            ).coerceIn(
                                            0f,
                                            1f
                                        ),
                                right =
                                    (
                                            box.right /
                                                    bitmap.width
                                            ).coerceIn(
                                            0f,
                                            1f
                                        ),
                                bottom =
                                    (
                                            box.bottom /
                                                    bitmap.height
                                            ).coerceIn(
                                            0f,
                                            1f
                                        ),
                                originalLabel =
                                    annotation
                                        .originalLabel,
                                originalConfidence =
                                    annotation
                                        .originalConfidence
                            )
                        }

                    trainingRepository
                        .saveExample(
                            bitmap,
                            annotations
                        )

                    val count =
                        trainingRepository
                            .getExampleCount()

                    _uiState.update {
                        it.copy(
                            isSavingTrainingExample =
                                false,
                            trainingExampleCount =
                                count,
                            trainingMessage =
                                "Training example saved."
                        )
                    }
                } catch (
                    exception:
                    CancellationException
                ) {
                    throw exception
                } catch (exception: Exception) {
                    _uiState.update {
                        it.copy(
                            isSavingTrainingExample =
                                false,
                            trainingMessage =
                                exception.message
                                    ?: "Could not save the training example."
                        )
                    }
                }
            }
    }

    private fun exportTrainingDataset(
        destination: Uri
    ) {
        trainingJob?.cancel()

        trainingJob =
            viewModelScope.launch {
                try {
                    _uiState.update {
                        it.copy(
                            isExportingTrainingDataset =
                                true,
                            trainingMessage = null
                        )
                    }

                    trainingExportManager
                        .export(destination)

                    _uiState.update {
                        it.copy(
                            isExportingTrainingDataset =
                                false,
                            trainingMessage =
                                "Training dataset exported."
                        )
                    }
                } catch (
                    exception:
                    CancellationException
                ) {
                    throw exception
                } catch (exception: Exception) {
                    _uiState.update {
                        it.copy(
                            isExportingTrainingDataset =
                                false,
                            trainingMessage =
                                exception.message
                                    ?: "Could not export the training dataset."
                        )
                    }
                }
            }
    }

    private fun refreshTrainingExampleCount() {
        viewModelScope.launch {
            val count =
                runCatching {
                    trainingRepository
                        .getExampleCount()
                }.getOrDefault(0)

            _uiState.update {
                it.copy(
                    trainingExampleCount =
                        count
                )
            }
        }
    }

    override fun onCleared() {
        imageLoadingJob?.cancel()
        detectionJob?.cancel()
        generationJob?.cancel()
        trainingJob?.cancel()

        super.onCleared()
    }
}