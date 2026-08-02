package com.example.visualvocab.feature.vocab.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visualvocab.domain.model.SentenceDifficulty
import com.example.visualvocab.domain.model.Vocabulary

@Composable
fun VocabularyCard(
    vocabulary: Vocabulary?,
    difficulty: SentenceDifficulty,
    isGenerating: Boolean,
    onRegenerate: () -> Unit,
    onMakeEasier: () -> Unit,
    onMakeHarder: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var horizontalDrag by remember {
        mutableFloatStateOf(0f)
    }

    ElevatedCard(
        modifier = modifier
            .widthIn(max = 350.dp)
            .fillMaxWidth()
            .pointerInput(
                vocabulary?.englishSentence,
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
                        horizontalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (!isGenerating) {
                            when {
                                horizontalDrag >=
                                        SWIPE_THRESHOLD -> {
                                    onMakeEasier()
                                }

                                horizontalDrag <=
                                        -SWIPE_THRESHOLD -> {
                                    onMakeHarder()
                                }
                            }
                        }

                        horizontalDrag = 0f
                    },
                    onDragCancel = {
                        horizontalDrag = 0f
                    }
                )
            },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
                    .copy(alpha = 0.91f)
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 24.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = 12.dp,
                        end = 12.dp
                    )
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme
                            .onSurface
                            .copy(alpha = 0.08f)
                    )
            ) {
                Icon(
                    imageVector =
                        Icons.Default.Clear,
                    contentDescription =
                        "Close vocabulary card",
                    modifier =
                        Modifier.size(18.dp),
                    tint =
                        MaterialTheme.colorScheme
                            .onSurface
                            .copy(alpha = 0.78f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 28.dp,
                        end = 28.dp,
                        top = 26.dp,
                        bottom = 24.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                DifficultyBadge(
                    difficulty = difficulty
                )

                Spacer(
                    Modifier.height(18.dp)
                )

                if (vocabulary != null) {
                    Text(
                        text =
                            vocabulary.englishWord
                                .replaceFirstChar {
                                    it.uppercase()
                                },
                        style =
                            MaterialTheme.typography
                                .headlineLarge
                                .copy(
                                    fontSize = 36.sp,
                                    lineHeight = 40.sp
                                ),
                        fontWeight =
                            FontWeight.Bold,
                        textAlign =
                            TextAlign.Center,
                        color =
                            MaterialTheme.colorScheme
                                .onSurface
                    )

                    Spacer(
                        Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            vocabulary.spanishWord,
                        style =
                            MaterialTheme.typography
                                .titleLarge
                                .copy(
                                    fontSize = 22.sp,
                                    lineHeight = 28.sp
                                ),
                        fontWeight =
                            FontWeight.SemiBold,
                        textAlign =
                            TextAlign.Center,
                        color =
                            MaterialTheme.colorScheme
                                .primary
                    )
                }

                Spacer(
                    Modifier.height(22.dp)
                )

                Crossfade(
                    targetState =
                        if (isGenerating) {
                            null
                        } else {
                            vocabulary
                        },
                    label =
                        "vocabulary-sentence"
                ) { displayedVocabulary ->
                    if (
                        displayedVocabulary ==
                        null
                    ) {
                        Column(
                            horizontalAlignment =
                                Alignment
                                    .CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )

                            Spacer(
                                Modifier.height(10.dp)
                            )

                            Text(
                                text =
                                    "Creating a sentence…",
                                style =
                                    MaterialTheme.typography
                                        .bodySmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier =
                                Modifier.widthIn(
                                    max = 292.dp
                                ),
                            horizontalAlignment =
                                Alignment
                                    .CenterHorizontally
                        ) {
                            Text(
                                text =
                                    displayedVocabulary
                                        .englishSentence,
                                style =
                                    MaterialTheme.typography
                                        .bodyLarge
                                        .copy(
                                            fontSize = 18.sp,
                                            lineHeight = 26.sp
                                        ),
                                fontWeight =
                                    FontWeight.Medium,
                                textAlign =
                                    TextAlign.Center,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface
                            )

                            if (
                                displayedVocabulary
                                    .spanishSentence
                                    .isNotBlank()
                            ) {
                                Spacer(
                                    Modifier.height(12.dp)
                                )

                                Text(
                                    text =
                                        displayedVocabulary
                                            .spanishSentence,
                                    style =
                                        MaterialTheme.typography
                                            .bodyLarge
                                            .copy(
                                                fontSize = 17.sp,
                                                lineHeight = 25.sp
                                            ),
                                    fontWeight =
                                        FontWeight.Medium,
                                    textAlign =
                                        TextAlign.Center,
                                    color =
                                        MaterialTheme.colorScheme
                                            .primary
                                )
                            }
                        }
                    }
                }

                Spacer(
                    Modifier.height(22.dp)
                )

                Surface(
                    shape = CircleShape,
                    color =
                        MaterialTheme.colorScheme
                            .onSurface
                            .copy(alpha = 0.055f)
                ) {
                    TextButton(
                        onClick = onRegenerate,
                        enabled = !isGenerating,
                        modifier = Modifier.padding(
                            horizontal = 4.dp
                        )
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Refresh,
                            contentDescription = null,
                            modifier =
                                Modifier.size(17.dp)
                        )

                        Spacer(
                            Modifier.size(8.dp)
                        )

                        Text(
                            text =
                                "Another sentence",
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyBadge(
    difficulty: SentenceDifficulty
) {
    val label =
        when (difficulty) {
            SentenceDifficulty.EASY ->
                "Easy"

            SentenceDifficulty.MEDIUM ->
                "Medium"

            SentenceDifficulty.HARD ->
                "Hard"
        }

    val dotColor =
        when (difficulty) {
            SentenceDifficulty.EASY ->
                Color(0xFF58C97A)

            SentenceDifficulty.MEDIUM ->
                Color(0xFFF0B64B)

            SentenceDifficulty.HARD ->
                Color(0xFFEF6A6A)
        }

    Surface(
        shape = CircleShape,
        color =
            MaterialTheme.colorScheme
                .onSurface
                .copy(alpha = 0.07f)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Spacer(
                Modifier.size(7.dp)
            )

            Text(
                text = label,
                style =
                    MaterialTheme.typography
                        .labelMedium,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    MaterialTheme.colorScheme
                        .onSurface
                        .copy(alpha = 0.82f)
            )
        }
    }
}

private const val SWIPE_THRESHOLD =
    90f