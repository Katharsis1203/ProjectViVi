package com.example.visualvocab.feature.vocab.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.visualvocab.domain.model.AppMode
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.Vocabulary
import com.example.visualvocab.feature.vocab.presentation.EditableTrainingAnnotation
import com.example.visualvocab.feature.vocab.presentation.VocabUiEvent
import com.example.visualvocab.feature.vocab.presentation.VocabViewModel
import com.example.visualvocab.feature.vocab.ui.components.DetectionOverlay
import com.example.visualvocab.feature.vocab.ui.components.ImagePickerContent

@Composable
fun VisualVocabScreen(
    viewModel: VocabViewModel
) {
    val uiState by
    viewModel.uiState.collectAsState()

    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .PickVisualMedia()
        ) { uri ->
            uri?.let {
                viewModel.onEvent(
                    VocabUiEvent
                        .ImageSelected(it)
                )
            }
        }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .CreateDocument(
                        "application/zip"
                    )
        ) { uri ->
            uri?.let {
                viewModel.onEvent(
                    VocabUiEvent
                        .ExportTrainingDataset(
                            it
                        )
                )
            }
        }

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        val bitmap =
            uiState.bitmap

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            if (bitmap == null) {
                ImagePickerContent(
                    onSelectImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts
                                    .PickVisualMedia
                                    .ImageOnly
                            )
                        )
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            uiState.detections,
                            uiState.hasSelectedObject,
                            uiState.appMode,
                            bitmap
                        ) {
                            detectTapGestures {
                                    tapOffset ->

                                if (
                                    uiState.hasSelectedObject &&
                                    uiState.appMode ==
                                    AppMode.LEARN
                                ) {
                                    return@detectTapGestures
                                }

                                val scale =
                                    maxOf(
                                        size.width /
                                                bitmap.width
                                                    .toFloat(),
                                        size.height /
                                                bitmap.height
                                                    .toFloat()
                                    )

                                val offsetX =
                                    (
                                            size.width -
                                                    bitmap.width *
                                                    scale
                                            ) / 2f

                                val offsetY =
                                    (
                                            size.height -
                                                    bitmap.height *
                                                    scale
                                            ) / 2f

                                val bitmapX =
                                    (
                                            tapOffset.x -
                                                    offsetX
                                            ) / scale

                                val bitmapY =
                                    (
                                            tapOffset.y -
                                                    offsetY
                                            ) / scale

                                val matches =
                                    uiState.detections
                                        .filter {
                                            it.boundingBox
                                                .contains(
                                                    bitmapX,
                                                    bitmapY
                                                )
                                        }
                                        .sortedByDescending {
                                            it.score
                                        }

                                when {
                                    matches.size == 1 -> {
                                        viewModel.onEvent(
                                            VocabUiEvent
                                                .ObjectTapped(
                                                    matches
                                                        .first()
                                                )
                                        )
                                    }

                                    matches.size > 1 -> {
                                        viewModel.onEvent(
                                            VocabUiEvent
                                                .OverlappingDetectionsChanged(
                                                    matches
                                                )
                                        )
                                    }
                                }
                            }
                        }
                ) {
                    Image(
                        bitmap =
                            bitmap.asImageBitmap(),
                        contentDescription =
                            "Selected image",
                        modifier =
                            Modifier.fillMaxSize(),
                        contentScale =
                            ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops =
                                        arrayOf(
                                            0f to
                                                    Color.Black
                                                        .copy(
                                                            alpha =
                                                                0.46f
                                                        ),
                                            0.16f to
                                                    Color.Transparent,
                                            0.74f to
                                                    Color.Transparent,
                                            1f to
                                                    Color.Black
                                                        .copy(
                                                            alpha =
                                                                0.78f
                                                        )
                                        )
                                )
                            )
                    )

                    DetectionOverlay(
                        detections =
                            uiState.detections,
                        selectedDetection =
                            uiState.selectedDetection,
                        imageWidth =
                            bitmap.width,
                        imageHeight =
                            bitmap.height,
                        useCropScale = true
                    )

                    HeaderControls(
                        mode = uiState.appMode,
                        onModeChanged = {
                            viewModel.onEvent(
                                VocabUiEvent
                                    .ChangeMode(it)
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible =
                            uiState.isDetecting,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(
                                Alignment.TopCenter
                            )
                            .windowInsetsPadding(
                                WindowInsets.statusBars
                            )
                            .padding(top = 82.dp)
                    ) {
                        StatusPill(
                            text =
                                "Detecting objects",
                            showProgress = true
                        )
                    }

                    AnimatedVisibility(
                        visible =
                            uiState.detections
                                .isNotEmpty() &&
                                    !uiState.isDetecting,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(
                                Alignment.TopCenter
                            )
                            .windowInsetsPadding(
                                WindowInsets.statusBars
                            )
                            .padding(top = 82.dp)
                    ) {
                        StatusPill(
                            text =
                                if (
                                    uiState.appMode ==
                                    AppMode.TEACH
                                ) {
                                    "Tap a box to correct it"
                                } else {
                                    "Tap a highlighted object"
                                }
                        )
                    }

                    if (
                        uiState.appMode ==
                        AppMode.TEACH
                    ) {
                        TeachControls(
                            confirmedCount =
                                uiState
                                    .editableTrainingAnnotations
                                    .count {
                                        it.isConfirmed
                                    },
                            exampleCount =
                                uiState.trainingExampleCount,
                            isSaving =
                                uiState
                                    .isSavingTrainingExample,
                            isExporting =
                                uiState
                                    .isExportingTrainingDataset,
                            onSave = {
                                viewModel.onEvent(
                                    VocabUiEvent
                                        .SaveTrainingExample
                                )
                            },
                            onExport = {
                                exportLauncher.launch(
                                    "visual_vocab_dataset.zip"
                                )
                            },
                            modifier = Modifier
                                .align(
                                    Alignment.BottomCenter
                                )
                                .windowInsetsPadding(
                                    WindowInsets
                                        .navigationBars
                                )
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 18.dp
                                )
                        )
                    } else {
                        ChangeImageButton(
                            enabled =
                                !uiState.isProcessing,
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts
                                            .PickVisualMedia
                                            .ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier
                                .align(
                                    Alignment.BottomCenter
                                )
                                .windowInsetsPadding(
                                    WindowInsets
                                        .navigationBars
                                )
                                .padding(
                                    bottom = 22.dp
                                )
                        )
                    }

                    AnimatedVisibility(
                        visible =
                            uiState.hasSelectedObject &&
                                    uiState.appMode ==
                                    AppMode.LEARN,
                        enter =
                            fadeIn() +
                                    slideInVertically {
                                        it / 3
                                    },
                        exit =
                            fadeOut() +
                                    slideOutVertically {
                                        it / 3
                                    },
                        modifier = Modifier
                            .align(
                                Alignment.BottomCenter
                            )
                            .zIndex(5f)
                    ) {
                        VocabularySheet(
                            vocabulary =
                                uiState.vocabulary,
                            difficulty =
                                uiState
                                    .sentenceDifficulty,
                            isGenerating =
                                uiState.isGenerating,
                            onRegenerate = {
                                viewModel.onEvent(
                                    VocabUiEvent
                                        .RegenerateSentence
                                )
                            },
                            onMakeEasier = {
                                viewModel.onEvent(
                                    VocabUiEvent
                                        .MakeSentenceEasier
                                )
                            },
                            onMakeHarder = {
                                viewModel.onEvent(
                                    VocabUiEvent
                                        .MakeSentenceHarder
                                )
                            },
                            onClose = {
                                viewModel.onEvent(
                                    VocabUiEvent
                                        .ClearSelectedObject
                                )
                            }
                        )
                    }

                    (
                            uiState.trainingMessage
                                ?: uiState.errorMessage
                            )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let { message ->
                            ErrorPill(
                                message = message,
                                modifier = Modifier
                                    .align(
                                        Alignment.BottomCenter
                                    )
                                    .windowInsetsPadding(
                                        WindowInsets
                                            .navigationBars
                                    )
                                    .padding(
                                        start = 24.dp,
                                        end = 24.dp,
                                        bottom =
                                            if (
                                                uiState.appMode ==
                                                AppMode.TEACH
                                            ) {
                                                116.dp
                                            } else {
                                                92.dp
                                            }
                                    )
                            )
                        }
                }
            }
        }

        if (
            uiState.overlappingDetections
                .isNotEmpty()
        ) {
            OverlappingDetectionsDialog(
                detections =
                    uiState
                        .overlappingDetections,
                onDetectionSelected = {
                    viewModel.onEvent(
                        VocabUiEvent
                            .ObjectTapped(it)
                    )
                },
                onDismiss = {
                    viewModel.onEvent(
                        VocabUiEvent
                            .OverlappingDetectionsChanged(
                                emptyList()
                            )
                    )
                }
            )
        }

        uiState.selectedTrainingAnnotation
            ?.let { annotation ->
                TrainingAnnotationDialog(
                    annotation = annotation,
                    onConfirm = { label ->
                        viewModel.onEvent(
                            VocabUiEvent
                                .UpdateTrainingLabel(
                                    annotation.id,
                                    label
                                )
                        )

                        viewModel.onEvent(
                            VocabUiEvent
                                .ConfirmTrainingAnnotation(
                                    annotation.id
                                )
                        )
                    },
                    onDelete = {
                        viewModel.onEvent(
                            VocabUiEvent
                                .DeleteTrainingAnnotation(
                                    annotation.id
                                )
                        )
                    },
                    onDismiss = {
                        viewModel.onEvent(
                            VocabUiEvent
                                .DismissTrainingAnnotation
                        )
                    }
                )
            }
    }
}

@Composable
private fun HeaderControls(
    mode: AppMode,
    onModeChanged: (AppMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.statusBars
            )
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color =
                Color.Black.copy(
                    alpha = 0.38f
                )
        ) {
            Text(
                text = "Visual Vocab",
                modifier = Modifier.padding(
                    horizontal = 13.dp,
                    vertical = 8.dp
                ),
                style =
                    MaterialTheme.typography
                        .labelLarge,
                fontWeight =
                    FontWeight.Bold,
                color = Color.White
            )
        }

        Surface(
            shape = CircleShape,
            color =
                Color.Black.copy(
                    alpha = 0.58f
                )
        ) {
            Row(
                modifier =
                    Modifier.padding(4.dp)
            ) {
                ModeButton(
                    text = "Learn",
                    selected =
                        mode == AppMode.LEARN,
                    onClick = {
                        onModeChanged(
                            AppMode.LEARN
                        )
                    }
                )

                ModeButton(
                    text = "Teach",
                    selected =
                        mode == AppMode.TEACH,
                    onClick = {
                        onModeChanged(
                            AppMode.TEACH
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color =
            if (selected) {
                MaterialTheme
                    .colorScheme
                    .primary
            } else {
                Color.Transparent
            }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 13.dp,
                vertical = 7.dp
            ),
            style =
                MaterialTheme.typography
                    .labelMedium,
            fontWeight =
                FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    showProgress: Boolean = false
) {
    Surface(
        shape = CircleShape,
        color =
            Color.Black.copy(
                alpha = 0.58f
            )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 8.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            if (showProgress) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            }

            Text(
                text = text,
                style =
                    MaterialTheme.typography
                        .labelMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun TeachControls(
    confirmedCount: Int,
    exampleCount: Int,
    isSaving: Boolean,
    isExporting: Boolean,
    onSave: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape =
            RoundedCornerShape(24.dp),
        color =
            Color.Black.copy(
                alpha = 0.72f
            ),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 12.dp
            )
        ) {
            Text(
                text =
                    "$confirmedCount confirmed • $exampleCount saved examples",
                style =
                    MaterialTheme.typography
                        .labelSmall,
                color =
                    Color.White.copy(
                        alpha = 0.78f
                    )
            )

            Spacer(
                Modifier.height(9.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSave,
                    enabled =
                        confirmedCount > 0 &&
                                !isSaving &&
                                !isExporting
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(17.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector =
                                Icons.Default.Save,
                            contentDescription =
                                null
                        )
                    }

                    Spacer(
                        Modifier.size(7.dp)
                    )

                    Text("Save")
                }

                OutlinedButton(
                    onClick = onExport,
                    enabled =
                        exampleCount > 0 &&
                                !isSaving &&
                                !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(17.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector =
                                Icons.Default.Download,
                            contentDescription =
                                null
                        )
                    }

                    Spacer(
                        Modifier.size(7.dp)
                    )

                    Text("Export ZIP")
                }
            }
        }
    }
}

@Composable
private fun TrainingAnnotationDialog(
    annotation:
    EditableTrainingAnnotation,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember(
        annotation.id,
        annotation.label
    ) {
        mutableStateOf(
            annotation.label
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Confirm object")
        },
        text = {
            Column {
                Text(
                    text =
                        "Detected as ${annotation.originalLabel ?: "unknown"}" +
                                annotation.originalConfidence
                                    ?.let {
                                        " (${(it * 100).toInt()}%)"
                                    }
                                    .orEmpty(),
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Correct label")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        label.trim()
                    )
                },
                enabled =
                    label.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete
                ) {
                    Text("Delete")
                }

                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun ChangeImageButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color =
            Color.Black.copy(
                alpha = 0.62f
            ),
        shadowElevation = 8.dp
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier =
                Modifier.size(56.dp)
        ) {
            Icon(
                imageVector =
                    Icons.Default.Image,
                contentDescription =
                    "Choose another image",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ErrorPill(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape =
            RoundedCornerShape(18.dp),
        color =
            MaterialTheme.colorScheme
                .errorContainer
                .copy(alpha = 0.94f)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 11.dp
            ),
            style =
                MaterialTheme.typography
                    .bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onErrorContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VocabularySheet(
    vocabulary: Vocabulary?,
    difficulty:
    SentenceDifficulty,
    isGenerating: Boolean,
    onRegenerate: () -> Unit,
    onMakeEasier: () -> Unit,
    onMakeHarder: () -> Unit,
    onClose: () -> Unit
) {
    var horizontalDrag by remember {
        mutableFloatStateOf(0f)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(
                vocabulary
                    ?.englishSentence,
                difficulty,
                isGenerating
            ) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        horizontalDrag = 0f
                    },
                    onHorizontalDrag = {
                            change,
                            dragAmount ->

                        change.consume()
                        horizontalDrag +=
                            dragAmount
                    },
                    onDragEnd = {
                        if (!isGenerating) {
                            when {
                                horizontalDrag >=
                                        SWIPE_THRESHOLD ->
                                    onMakeEasier()

                                horizontalDrag <=
                                        -SWIPE_THRESHOLD ->
                                    onMakeHarder()
                            }
                        }

                        horizontalDrag = 0f
                    },
                    onDragCancel = {
                        horizontalDrag = 0f
                    }
                )
            },
        shape = RoundedCornerShape(
            topStart = 30.dp,
            topEnd = 30.dp
        ),
        colors =
            CardDefaults
                .elevatedCardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                            .copy(
                                alpha = 0.97f
                            )
                ),
        elevation =
            CardDefaults
                .elevatedCardElevation(
                    defaultElevation =
                        18.dp
                )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                )
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(
                        Alignment.TopEnd
                    )
                    .padding(
                        top = 10.dp,
                        end = 12.dp
                    )
            ) {
                Icon(
                    imageVector =
                        Icons.Default.Clear,
                    contentDescription =
                        "Close"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 26.dp,
                        end = 26.dp,
                        top = 16.dp,
                        bottom = 20.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(
                        width = 42.dp,
                        height = 4.dp
                    ),
                    shape = CircleShape,
                    color =
                        MaterialTheme
                            .colorScheme
                            .outline
                            .copy(
                                alpha = 0.42f
                            )
                ) {}

                Spacer(
                    Modifier.height(16.dp)
                )

                Surface(
                    shape = CircleShape,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                ) {
                    Text(
                        text =
                            when (difficulty) {
                                SentenceDifficulty
                                    .EASY ->
                                    "EASY"

                                SentenceDifficulty
                                    .MEDIUM ->
                                    "MEDIUM"

                                SentenceDifficulty
                                    .HARD ->
                                    "HARD"
                            },
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    12.dp,
                                vertical =
                                    6.dp
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    Modifier.height(14.dp)
                )

                if (vocabulary != null) {
                    Text(
                        text =
                            vocabulary
                                .englishWord
                                .replaceFirstChar {
                                    it.uppercase()
                                },
                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            vocabulary
                                .spanishWord,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }

                Spacer(
                    Modifier.height(18.dp)
                )

                Crossfade(
                    targetState =
                        if (isGenerating) {
                            null
                        } else {
                            vocabulary
                        },
                    label = "sentence"
                ) { displayedVocabulary ->
                    if (
                        displayedVocabulary ==
                        null
                    ) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(
                                    28.dp
                                ),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Column(
                            horizontalAlignment =
                                Alignment
                                    .CenterHorizontally
                        ) {
                            Text(
                                text =
                                    displayedVocabulary
                                        .englishSentence,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyLarge,
                                fontWeight =
                                    FontWeight
                                        .Medium,
                                textAlign =
                                    TextAlign.Center
                            )

                            Spacer(
                                Modifier.height(
                                    9.dp
                                )
                            )

                            Text(
                                text =
                                    displayedVocabulary
                                        .spanishSentence,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,
                                textAlign =
                                    TextAlign.Center,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )
                        }
                    }
                }

                Spacer(
                    Modifier.height(18.dp)
                )

                Text(
                    text =
                        "← harder   swipe   easier →",
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall
                )

                Spacer(
                    Modifier.height(6.dp)
                )

                TextButton(
                    onClick =
                        onRegenerate,
                    enabled =
                        !isGenerating
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.Refresh,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                17.dp
                            )
                    )

                    Spacer(
                        Modifier.size(7.dp)
                    )

                    Text(
                        "Another sentence"
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlappingDetectionsDialog(
    detections:
    List<DetectionResult>,
    onDetectionSelected:
        (DetectionResult) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text("Select object")
        },
        text = {
            Column {
                detections.forEach {
                        detection ->

                    TextButton(
                        onClick = {
                            onDetectionSelected(
                                detection
                            )
                            onDismiss()
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    ) {
                        Text(
                            "${detection.label} " +
                                    "(${(detection.score * 100).toInt()}%)"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

private const val SWIPE_THRESHOLD =
    90f