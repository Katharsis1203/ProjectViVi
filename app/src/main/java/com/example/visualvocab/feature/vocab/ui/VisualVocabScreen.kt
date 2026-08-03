package com.example.visualvocab.feature.vocab.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush as GradientBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import com.example.visualvocab.domain.model.AppMode
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.feature.vocab.presentation.AnnotationTool
import com.example.visualvocab.feature.vocab.presentation.EditableTrainingAnnotation
import com.example.visualvocab.feature.vocab.presentation.LessonAnswerResult
import com.example.visualvocab.feature.vocab.presentation.VocabUiEvent
import com.example.visualvocab.feature.vocab.presentation.VocabUiState
import com.example.visualvocab.feature.vocab.presentation.VocabViewModel
import com.example.visualvocab.feature.vocab.ui.components.AnnotationOverlay
import com.example.visualvocab.feature.vocab.ui.components.DetectionOverlay
import com.example.visualvocab.feature.vocab.ui.components.ImagePickerContent
import com.example.visualvocab.feature.vocab.ui.components.LessonCompleteCard
import com.example.visualvocab.feature.vocab.ui.components.VocabularyCard
import com.example.visualvocab.ui.theme.CreatorPurple
import com.example.visualvocab.ui.theme.NightInk
import com.example.visualvocab.ui.theme.SuccessGreen

@Composable
fun VisualVocabScreen(viewModel: VocabViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var destination by rememberSaveable { mutableStateOf(VisualVocabDestination.HOME) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onEvent(VocabUiEvent.ImageSelected(it)) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.onEvent(VocabUiEvent.ExportTrainingDataset(it)) }
    }

    val chooseImage: () -> Unit = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val isCreatorStudio = uiState.appMode == AppMode.TEACH

    fun openLearningScan() {
        if (uiState.appMode != AppMode.LEARN) {
            viewModel.onEvent(VocabUiEvent.ChangeMode(AppMode.LEARN))
        }
        destination = VisualVocabDestination.SCAN
    }

    fun exitCreatorStudio() {
        viewModel.onEvent(VocabUiEvent.ChangeMode(AppMode.LEARN))
        destination = VisualVocabDestination.PROFILE
    }

    BackHandler(enabled = isCreatorStudio) {
        exitCreatorStudio()
    }

    BackHandler(enabled = !isCreatorStudio && destination != VisualVocabDestination.HOME) {
        if (destination == VisualVocabDestination.SCAN) {
            viewModel.onEvent(VocabUiEvent.ClearSelectedObject)
        }
        destination = VisualVocabDestination.HOME
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GameTopBar(
                destination = destination,
                isCreatorStudio = isCreatorStudio,
                imageSelected = uiState.bitmap != null,
                imageActionsEnabled = !uiState.isProcessing,
                playerProgress = uiState.playerProgress,
                onChangeImage = chooseImage,
                onExitCreatorStudio = ::exitCreatorStudio
            )
        },
        bottomBar = {
            when {
                isCreatorStudio &&
                        destination == VisualVocabDestination.SCAN &&
                        uiState.bitmap != null -> {
                    TeachControls(
                        confirmedCount = uiState.editableTrainingAnnotations.count { it.isConfirmed },
                        exampleCount = uiState.trainingExampleCount,
                        annotationTool = uiState.annotationTool,
                        onToolChanged = { viewModel.onEvent(VocabUiEvent.ChangeAnnotationTool(it)) },
                        isSaving = uiState.isSavingTrainingExample,
                        isExporting = uiState.isExportingTrainingDataset,
                        isUploading = uiState.isUploadingTrainingDataset,
                        onSave = { viewModel.onEvent(VocabUiEvent.SaveTrainingExample) },
                        onExport = { exportLauncher.launch("visual_vocab_dataset.zip") },
                        onUpload = { viewModel.onEvent(VocabUiEvent.UploadTrainingDataset) }
                    )
                }

                !isCreatorStudio -> {
                    GameBottomNavigation(
                        selected = destination,
                        onSelected = { selected ->
                            if (destination == VisualVocabDestination.SCAN &&
                                selected != VisualVocabDestination.SCAN
                            ) {
                                viewModel.onEvent(VocabUiEvent.ClearSelectedObject)
                            }
                            if (uiState.appMode != AppMode.LEARN) {
                                viewModel.onEvent(VocabUiEvent.ChangeMode(AppMode.LEARN))
                            }
                            destination = selected
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isCreatorStudio -> {
                    ScanWorkspace(
                        uiState = uiState,
                        onEvent = { event -> viewModel.onEvent(event) },
                        onChooseImage = chooseImage
                    )
                }

                destination == VisualVocabDestination.HOME -> {
                    HomeScreen(
                        playerProgress = uiState.playerProgress,
                        onStartScan = ::openLearningScan,
                        onOpenWords = { destination = VisualVocabDestination.WORDS }
                    )
                }

                destination == VisualVocabDestination.SCAN -> {
                    ScanWorkspace(
                        uiState = uiState,
                        onEvent = { event -> viewModel.onEvent(event) },
                        onChooseImage = chooseImage
                    )
                }

                destination == VisualVocabDestination.WORDS -> {
                    WordsScreen(
                        playerProgress = uiState.playerProgress,
                        onStartScan = ::openLearningScan
                    )
                }

                destination == VisualVocabDestination.PROFILE -> {
                    ProfileScreen(
                        playerProgress = uiState.playerProgress,
                        onOpenCreatorStudio = {
                            viewModel.onEvent(VocabUiEvent.ClearSelectedObject)
                            viewModel.onEvent(VocabUiEvent.ChangeMode(AppMode.TEACH))
                            destination = VisualVocabDestination.SCAN
                        }
                    )
                }
            }

            val visibleMessage: Pair<String, Boolean>? = when {
                !uiState.datasetUploadMessage.isNullOrBlank() -> uiState.datasetUploadMessage!! to false
                !uiState.trainingMessage.isNullOrBlank() -> uiState.trainingMessage!! to false
                !uiState.errorMessage.isNullOrBlank() && !uiState.hasSelectedObject -> {
                    uiState.errorMessage!! to true
                }
                else -> null
            }

            if (destination == VisualVocabDestination.SCAN || isCreatorStudio) {
                visibleMessage?.let { (message, isError) ->
                    MessagePill(
                        message = message,
                        isError = isError,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }

            if (!isCreatorStudio) {
                XpAwardBurst(
                    amount = uiState.recentXpAward,
                    animationToken = uiState.xpAnimationToken,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                        .zIndex(20f)
                )
            }
        }

        if (uiState.overlappingDetections.isNotEmpty()) {
            OverlappingDetectionsDialog(
                detections = uiState.overlappingDetections,
                onDetectionSelected = { viewModel.onEvent(VocabUiEvent.ObjectTapped(it)) },
                onDismiss = {
                    viewModel.onEvent(VocabUiEvent.OverlappingDetectionsChanged(emptyList()))
                }
            )
        }

        uiState.selectedTrainingAnnotation?.let { annotation ->
            TrainingAnnotationDialog(
                annotation = annotation,
                onConfirm = { label ->
                    viewModel.onEvent(VocabUiEvent.UpdateTrainingLabel(annotation.id, label))
                    viewModel.onEvent(VocabUiEvent.ConfirmTrainingAnnotation(annotation.id))
                },
                onDelete = {
                    viewModel.onEvent(VocabUiEvent.DeleteTrainingAnnotation(annotation.id))
                },
                onDismiss = {
                    if (annotation.isConfirmed) {
                        viewModel.onEvent(VocabUiEvent.DismissTrainingAnnotation)
                    } else {
                        viewModel.onEvent(VocabUiEvent.DeleteTrainingAnnotation(annotation.id))
                    }
                }
            )
        }
    }
}


@Composable
private fun XpAwardBurst(
    amount: Int,
    animationToken: Long,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.84f,
        animationSpec = tween(durationMillis = 220),
        label = "XP burst scale"
    )

    LaunchedEffect(animationToken) {
        if (animationToken <= 0L || amount <= 0) return@LaunchedEffect
        visible = false
        delay(30)
        visible = true
        delay(1050)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.78f) + slideInVertically { -it / 2 },
        exit = fadeOut() + scaleOut(targetScale = 0.88f) + slideOutVertically { -it / 2 },
        modifier = modifier.graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
    ) {
        Surface(
            shape = CircleShape,
            color = com.example.visualvocab.ui.theme.QuestGold,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = com.example.visualvocab.ui.theme.ExplorerBlueDark,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "+$amount XP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = com.example.visualvocab.ui.theme.ExplorerBlueDark
                )
            }
        }
    }
}

@Composable
private fun ScanWorkspace(
    uiState: VocabUiState,
    onEvent: (VocabUiEvent) -> Unit,
    onChooseImage: () -> Unit
) {
    val bitmap = uiState.bitmap

    if (bitmap == null) {
        ImagePickerContent(
            onSelectImage = onChooseImage,
            targetWord = uiState.targetWord,
            isTeachMode = uiState.appMode == AppMode.TEACH
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WorkspaceStatus(uiState = uiState)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(NightInk)
                .pointerInput(
                    uiState.detections,
                    uiState.completedLessonDetections,
                    uiState.selectedDetection,
                    uiState.isLessonComplete,
                    uiState.appMode,
                    bitmap
                ) {
                    detectTapGestures { tapOffset ->
                        if (
                            uiState.appMode != AppMode.LEARN ||
                            uiState.selectedDetection != null ||
                            uiState.isLessonComplete
                        ) {
                            return@detectTapGestures
                        }

                        val scale = maxOf(
                            size.width / bitmap.width.toFloat(),
                            size.height / bitmap.height.toFloat()
                        )
                        val offsetX = (size.width - bitmap.width * scale) / 2f
                        val offsetY = (size.height - bitmap.height * scale) / 2f
                        val bitmapX = (tapOffset.x - offsetX) / scale
                        val bitmapY = (tapOffset.y - offsetY) / scale

                        val matches = uiState.detections
                            .filter { detection ->
                                detection.boundingBox.contains(bitmapX, bitmapY) &&
                                        uiState.completedLessonDetections.none { completed ->
                                            detectionsMatch(detection, completed)
                                        }
                            }
                            .sortedByDescending { it.score }

                        when {
                            matches.size == 1 -> onEvent(VocabUiEvent.ObjectTapped(matches.first()))
                            matches.size > 1 -> onEvent(VocabUiEvent.OverlappingDetectionsChanged(matches))
                        }
                    }
                }
        ) {
            ComposeImage(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Selected image",
                modifier = Modifier.fillMaxSize(),
                contentScale = if (uiState.appMode == AppMode.TEACH) {
                    ContentScale.Fit
                } else {
                    ContentScale.Crop
                }
            )

            if (uiState.appMode == AppMode.LEARN) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            GradientBrush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Black.copy(alpha = 0.12f),
                                    0.18f to Color.Transparent,
                                    0.78f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.24f)
                                )
                            )
                        )
                )
            }

            if (uiState.appMode == AppMode.TEACH) {
                AnnotationOverlay(
                    detections = uiState.detections,
                    annotations = uiState.editableTrainingAnnotations,
                    selectedAnnotationId = uiState.selectedTrainingAnnotationId,
                    tool = uiState.annotationTool,
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    useCropScale = false,
                    onDetectionSelected = { detection ->
                        onEvent(VocabUiEvent.AddDetectionAnnotation(detection))
                    },
                    onAddAnnotation = { left, top, right, bottom ->
                        onEvent(VocabUiEvent.AddManualAnnotation(left, top, right, bottom))
                    },
                    onUpdateAnnotation = { id, left, top, right, bottom ->
                        onEvent(VocabUiEvent.UpdateAnnotationBox(id, left, top, right, bottom))
                    },
                    onSelectAnnotation = { id ->
                        onEvent(VocabUiEvent.SelectTrainingAnnotation(id))
                    }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = (uiState.hasSelectedObject || uiState.isLessonComplete) && uiState.appMode == AppMode.LEARN,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                )
            }

            if (uiState.appMode == AppMode.LEARN) {
                DetectionOverlay(
                    detections = uiState.detections,
                    selectedDetection = uiState.selectedDetection,
                    completedDetections = uiState.completedLessonDetections,
                    answerResult = uiState.lessonAnswerResult,
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    useCropScale = true
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.hasSelectedObject && uiState.appMode == AppMode.LEARN,
                enter = fadeIn() + scaleIn(initialScale = 0.95f) + slideInVertically { it / 10 },
                exit = fadeOut() + scaleOut(targetScale = 0.96f) + slideOutVertically { it / 12 },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .zIndex(5f)
            ) {
                VocabularyCard(
                    vocabulary = uiState.vocabulary,
                    options = uiState.lessonOptions,
                    selectedAnswer = uiState.selectedLessonAnswer,
                    answerResult = uiState.lessonAnswerResult,
                    questionNumber = uiState.currentLessonQuestionNumber,
                    questionTotal = uiState.lessonTargetCount,
                    isGenerating = uiState.isGenerating,
                    generationError = uiState.errorMessage,
                    onAnswerSelected = { answer ->
                        onEvent(VocabUiEvent.LessonAnswerSelected(answer))
                    },
                    onContinue = { onEvent(VocabUiEvent.ContinueLesson) },
                    onRetry = {
                        uiState.selectedDetection?.let { detection ->
                            onEvent(VocabUiEvent.ClearSelectedObject)
                            onEvent(VocabUiEvent.ObjectTapped(detection))
                        }
                    },
                    onClose = {
                        if (uiState.lessonAnswerResult != null) {
                            onEvent(VocabUiEvent.ContinueLesson)
                        } else {
                            onEvent(VocabUiEvent.ClearSelectedObject)
                        }
                    }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.isLessonComplete && uiState.appMode == AppMode.LEARN,
                enter = fadeIn() + scaleIn(initialScale = 0.92f) + slideInVertically { it / 8 },
                exit = fadeOut() + scaleOut(targetScale = 0.94f) + slideOutVertically { it / 10 },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .zIndex(6f)
            ) {
                LessonCompleteCard(
                    correctAnswers = uiState.lessonCorrectCount,
                    totalQuestions = uiState.lessonTargetCount,
                    xpEarned = uiState.lessonXpEarned,
                    newWords = uiState.lessonNewWords,
                    currentStreak = uiState.playerProgress.currentStreak,
                    dailyGoalReached = uiState.playerProgress.dailyXp >= uiState.playerProgress.dailyGoal,
                    onRestart = { onEvent(VocabUiEvent.RestartLesson) },
                    onChooseNewPhoto = onChooseImage
                )
            }
        }
    }
}

@Composable
private fun WorkspaceStatus(uiState: VocabUiState) {
    val isTeachMode = uiState.appMode == AppMode.TEACH

    val icon = when {
        uiState.isDetecting -> null
        isTeachMode && uiState.annotationTool == AnnotationTool.SELECT -> Icons.Rounded.AdsClick
        isTeachMode -> Icons.Rounded.Brush
        uiState.isLessonComplete -> Icons.Rounded.EmojiEvents
        uiState.lessonAnswerResult == LessonAnswerResult.CORRECT -> Icons.Rounded.CheckCircle
        uiState.lessonAnswerResult == LessonAnswerResult.INCORRECT -> Icons.Rounded.TouchApp
        else -> Icons.Rounded.TouchApp
    }

    val text = when {
        uiState.isDetecting -> "Vivi is searching for objects…"
        isTeachMode && uiState.annotationTool == AnnotationTool.SELECT && uiState.detections.isEmpty() -> {
            "No detections found — choose Draw to add one yourself"
        }
        isTeachMode && uiState.annotationTool == AnnotationTool.SELECT -> {
            "Tap a detected box to confirm and name it"
        }
        isTeachMode -> "Drag around an object, then enter its class name"
        uiState.detections.isEmpty() -> "No clear objects found — try a brighter photo"
        uiState.isLessonComplete -> "Quest complete — ${uiState.lessonCorrectCount} of ${uiState.lessonTargetCount} correct"
        uiState.isGenerating -> "Vivi is building question ${uiState.currentLessonQuestionNumber}…"
        uiState.hasSelectedObject && !uiState.errorMessage.isNullOrBlank() -> "That question did not load — try again or choose another object"
        uiState.lessonAnswerResult == LessonAnswerResult.CORRECT -> "Correct! Read the example, then continue"
        uiState.lessonAnswerResult == LessonAnswerResult.INCORRECT -> "Good try — the correct answer is highlighted"
        uiState.hasSelectedObject -> "Choose the Spanish translation"
        else -> {
            val nextQuestion = uiState.currentLessonQuestionNumber.coerceAtLeast(1)
            "Question $nextQuestion of ${uiState.lessonTargetCount} — tap a blue object"
        }
    }

    val accentColor = when {
        isTeachMode -> CreatorPurple
        uiState.lessonAnswerResult == LessonAnswerResult.CORRECT -> SuccessGreen
        uiState.lessonAnswerResult == LessonAnswerResult.INCORRECT -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        color = if (isTeachMode) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (uiState.isDetecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = accentColor
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = accentColor
                )
            }

            Spacer(Modifier.size(8.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = if (selected) CreatorPurple else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onTertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onTertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun TeachControls(
    confirmedCount: Int,
    exampleCount: Int,
    annotationTool: AnnotationTool,
    onToolChanged: (AnnotationTool) -> Unit,
    isSaving: Boolean,
    isExporting: Boolean,
    isUploading: Boolean,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onUpload: () -> Unit
) {
    var datasetMenuExpanded by remember { mutableStateOf(false) }
    val isBusy = isSaving || isExporting || isUploading

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(0.72f)) {
                    Text(
                        text = "$confirmedCount ${if (confirmedCount == 1) "box" else "boxes"} ready",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$exampleCount ${if (exampleCount == 1) "image" else "images"} saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    modifier = Modifier.weight(1.6f),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        ToolButton(
                            icon = Icons.Rounded.AdsClick,
                            text = "Tap",
                            selected = annotationTool == AnnotationTool.SELECT,
                            onClick = { onToolChanged(AnnotationTool.SELECT) },
                            modifier = Modifier.weight(1f)
                        )
                        ToolButton(
                            icon = Icons.Rounded.Brush,
                            text = "Draw",
                            selected = annotationTool == AnnotationTool.DRAW,
                            onClick = { onToolChanged(AnnotationTool.DRAW) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSave,
                    enabled = confirmedCount > 0 && !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = CreatorPurple
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    } else {
                        Icon(imageVector = Icons.Rounded.Save, contentDescription = null)
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(if (isSaving) "Saving…" else "Save image")
                }

                Box {
                    OutlinedButton(
                        onClick = { datasetMenuExpanded = true },
                        enabled = exampleCount > 0 && !isBusy,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isUploading || isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = null)
                        }
                        Spacer(Modifier.size(6.dp))
                        Text("Dataset")
                    }

                    DropdownMenu(
                        expanded = datasetMenuExpanded,
                        onDismissRequest = { datasetMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export ZIP") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Rounded.Download, contentDescription = null)
                            },
                            onClick = {
                                datasetMenuExpanded = false
                                onExport()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Upload dataset") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Rounded.CloudUpload, contentDescription = null)
                            },
                            onClick = {
                                datasetMenuExpanded = false
                                onUpload()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingAnnotationDialog(
    annotation: EditableTrainingAnnotation,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember(annotation.id, annotation.label) {
        mutableStateOf(annotation.label)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    annotation.isConfirmed -> "Edit object label"
                    annotation.originalLabel == null -> "Name this object"
                    else -> "Confirm detected object"
                }
            )
        },
        text = {
            Column {
                if (annotation.originalLabel != null) {
                    Text(
                        text = "Detected as ${annotation.originalLabel}" +
                                annotation.originalConfidence
                                    ?.let { " (${(it * 100).toInt()}%)" }
                                    .orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Class name") },
                    supportingText = { Text("Example: red_bull_can") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label.trim()) },
                enabled = label.isNotBlank()
            ) {
                Text(if (annotation.isConfirmed) "Save label" else "Add object")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(if (annotation.isConfirmed) "Delete box" else "Discard")
                }
                if (annotation.isConfirmed) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    )
}

@Composable
private fun MessagePill(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        },
        shadowElevation = 6.dp
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OverlappingDetectionsDialog(
    detections: List<DetectionResult>,
    onDetectionSelected: (DetectionResult) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Which object did you mean?") },
        text = {
            Column {
                detections.forEach { detection ->
                    TextButton(
                        onClick = {
                            onDetectionSelected(detection)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${detection.label} (${(detection.score * 100).toInt()}%)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun detectionsMatch(
    first: DetectionResult,
    second: DetectionResult,
    tolerance: Float = 1f
): Boolean =
    first.label == second.label &&
            kotlin.math.abs(first.boundingBox.left - second.boundingBox.left) <= tolerance &&
            kotlin.math.abs(first.boundingBox.top - second.boundingBox.top) <= tolerance &&
            kotlin.math.abs(first.boundingBox.right - second.boundingBox.right) <= tolerance &&
            kotlin.math.abs(first.boundingBox.bottom - second.boundingBox.bottom) <= tolerance
