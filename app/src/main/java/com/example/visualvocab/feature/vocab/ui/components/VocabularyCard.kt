package com.example.visualvocab.feature.vocab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visualvocab.domain.model.Vocabulary
import com.example.visualvocab.feature.vocab.presentation.LessonAnswerResult
import com.example.visualvocab.ui.theme.DiscoveryMintSoft
import com.example.visualvocab.ui.theme.ExplorerBlueSoft
import com.example.visualvocab.ui.theme.FriendlyCoral
import com.example.visualvocab.ui.theme.FriendlyCoralSoft
import com.example.visualvocab.ui.theme.QuestGold
import com.example.visualvocab.ui.theme.QuestGoldSoft
import com.example.visualvocab.ui.theme.SuccessGreen

@Composable
fun VocabularyCard(
    vocabulary: Vocabulary?,
    options: List<String>,
    selectedAnswer: String?,
    answerResult: LessonAnswerResult?,
    questionNumber: Int,
    questionTotal: Int,
    isGenerating: Boolean,
    generationError: String?,
    onAnswerSelected: (String) -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .widthIn(max = 390.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(30.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close question",
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LessonProgressHeader(
                    questionNumber = questionNumber,
                    questionTotal = questionTotal
                )

                Spacer(Modifier.height(18.dp))

                when {
                    isGenerating -> LoadingQuestionContent()
                    vocabulary == null -> QuestionErrorContent(
                        message = generationError,
                        onRetry = onRetry
                    )
                    else -> QuestionContent(
                        vocabulary = vocabulary,
                        options = options,
                        selectedAnswer = selectedAnswer,
                        answerResult = answerResult,
                        onAnswerSelected = onAnswerSelected,
                        onContinue = onContinue
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonProgressHeader(
    questionNumber: Int,
    questionTotal: Int
) {
    val safeTotal = questionTotal.coerceAtLeast(1)
    val progress = questionNumber.coerceIn(0, safeTotal).toFloat() / safeTotal.toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OBJECT QUEST",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$questionNumber of $questionTotal",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun LoadingQuestionContent() {
    ViviMascot(
        pose = ViviPose.SCANNING,
        modifier = Modifier.size(96.dp)
    )
    Spacer(Modifier.height(12.dp))
    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Vivi is building your question…",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(5.dp))
    Text(
        text = "Creating a Spanish translation and answer choices.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun QuestionErrorContent(
    message: String?,
    onRetry: () -> Unit
) {
    ViviMascot(
        pose = ViviPose.ENCOURAGING,
        modifier = Modifier.size(96.dp)
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "That question did not load",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = message?.takeIf(String::isNotBlank)
            ?: "Check your connection and try this object again.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(18.dp))
    OutlinedButton(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Try again", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuestionContent(
    vocabulary: Vocabulary,
    options: List<String>,
    selectedAnswer: String?,
    answerResult: LessonAnswerResult?,
    onAnswerSelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    Text(
        text = "What is “${vocabulary.englishWord.displayWord()}” in Spanish?",
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = 25.sp,
            lineHeight = 31.sp
        ),
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = "Choose the best translation",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(20.dp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { option ->
            AnswerOption(
                text = option,
                isSelected = selectedAnswer == option,
                isCorrectOption = option.trim().equals(
                    vocabulary.spanishWord.trim(),
                    ignoreCase = true
                ),
                answerResult = answerResult,
                onClick = { onAnswerSelected(option) }
            )
        }
    }

    if (answerResult != null) {
        Spacer(Modifier.height(18.dp))
        AnswerFeedback(
            vocabulary = vocabulary,
            answerResult = answerResult
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(17.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(7.dp))
            Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun AnswerOption(
    text: String,
    isSelected: Boolean,
    isCorrectOption: Boolean,
    answerResult: LessonAnswerResult?,
    onClick: () -> Unit
) {
    val hasAnswered = answerResult != null
    val selectedWrong = hasAnswered && isSelected && !isCorrectOption
    val revealCorrect = hasAnswered && isCorrectOption

    val containerColor = when {
        revealCorrect -> DiscoveryMintSoft
        selectedWrong -> FriendlyCoralSoft
        isSelected -> ExplorerBlueSoft
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        revealCorrect -> SuccessGreen
        selectedWrong -> FriendlyCoral
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val contentColor = when {
        revealCorrect -> SuccessGreen
        selectedWrong -> FriendlyCoral
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        enabled = !hasAnswered,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = containerColor,
        border = BorderStroke(if (revealCorrect || selectedWrong || isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )

            when {
                revealCorrect -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Correct answer",
                    tint = SuccessGreen
                )
                selectedWrong -> Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Incorrect answer",
                    tint = FriendlyCoral
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun AnswerFeedback(
    vocabulary: Vocabulary,
    answerResult: LessonAnswerResult
) {
    val isCorrect = answerResult == LessonAnswerResult.CORRECT
    val background = if (isCorrect) DiscoveryMintSoft else FriendlyCoralSoft
    val accent = if (isCorrect) SuccessGreen else FriendlyCoral

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = background
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ViviMascot(
                    pose = if (isCorrect) ViviPose.CORRECT else ViviPose.ENCOURAGING,
                    modifier = Modifier.size(62.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCorrect) "Great choice!" else "Nearly there!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                    Text(
                        text = if (isCorrect) {
                            "${vocabulary.spanishWord} means ${vocabulary.englishWord}."
                        } else {
                            "The answer is ${vocabulary.spanishWord}."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.72f)
            ) {
                Column(
                    modifier = Modifier.padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = vocabulary.englishSentence,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = vocabulary.spanishSentence,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun LessonCompleteCard(
    correctAnswers: Int,
    totalQuestions: Int,
    xpEarned: Int,
    newWords: Int,
    currentStreak: Int,
    dailyGoalReached: Boolean,
    onRestart: () -> Unit,
    onChooseNewPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeTotal = totalQuestions.coerceAtLeast(1)
    val accuracy = correctAnswers.toFloat() / safeTotal.toFloat()
    val earnedStars = when {
        accuracy >= 0.8f -> 3
        accuracy >= 0.5f -> 2
        else -> 1
    }
    val celebrationProgress = remember { Animatable(0f) }

    LaunchedEffect(correctAnswers, totalQuestions, xpEarned) {
        celebrationProgress.snapTo(0f)
        celebrationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1700)
        )
    }

    Box(
        modifier = modifier
            .widthIn(max = 390.dp)
            .fillMaxWidth()
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ViviMascot(
                    pose = ViviPose.COMPLETE,
                    modifier = Modifier.size(118.dp)
                )

                Text(
                    text = "Quest complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "You explored $totalQuestions ${if (totalQuestions == 1) "object" else "objects"} in this photo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) { index ->
                        Icon(
                            imageVector = if (index < earnedStars) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = null,
                            tint = QuestGold,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompletionStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Bolt,
                        value = "+$xpEarned",
                        label = "XP earned",
                        containerColor = QuestGoldSoft
                    )
                    CompletionStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.CollectionsBookmark,
                        value = newWords.toString(),
                        label = if (newWords == 1) "new word" else "new words",
                        containerColor = ExplorerBlueSoft
                    )
                }

                Spacer(Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = DiscoveryMintSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (dailyGoalReached) {
                                Icons.Rounded.Check
                            } else {
                                Icons.Rounded.LocalFireDepartment
                            },
                            contentDescription = null,
                            tint = SuccessGreen
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (dailyGoalReached) {
                                    "Daily goal complete"
                                } else {
                                    "$correctAnswers / $totalQuestions correct"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentStreak > 0) {
                                    "$currentStreak-day streak · Keep exploring tomorrow"
                                } else {
                                    "Every answer strengthens your collection"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Play this photo again", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onChooseNewPhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Choose another photo", fontWeight = FontWeight.Bold)
                }
            }
        }

        CompletionConfetti(
            progress = celebrationProgress.value,
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
private fun CompletionStat(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    containerColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompletionConfetti(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colours = listOf(QuestGold, SuccessGreen, FriendlyCoral, MaterialTheme.colorScheme.primary)
    Canvas(modifier = modifier) {
        val fade = (1f - progress).coerceIn(0f, 1f)
        val points = listOf(
            0.08f to 0.05f,
            0.22f to 0.12f,
            0.40f to 0.03f,
            0.62f to 0.10f,
            0.80f to 0.04f,
            0.92f to 0.16f,
            0.13f to 0.30f,
            0.87f to 0.34f
        )
        points.forEachIndexed { index, (x, startY) ->
            val y = startY + progress * (0.28f + (index % 3) * 0.08f)
            val colour = colours[index % colours.size].copy(alpha = fade)
            if (index % 2 == 0) {
                drawCircle(
                    color = colour,
                    radius = size.minDimension * 0.012f,
                    center = Offset(size.width * x, size.height * y)
                )
            } else {
                drawRect(
                    color = colour,
                    topLeft = Offset(size.width * x, size.height * y),
                    size = Size(size.minDimension * 0.022f, size.minDimension * 0.010f)
                )
            }
        }
    }
}

private fun String.displayWord(): String = trim().replaceFirstChar { character ->
    if (character.isLowerCase()) character.titlecase() else character.toString()
}
