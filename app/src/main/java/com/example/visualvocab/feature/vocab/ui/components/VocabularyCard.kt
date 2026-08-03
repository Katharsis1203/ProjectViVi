package com.example.visualvocab.feature.vocab.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.visualvocab.domain.model.Vocabulary
import com.example.visualvocab.feature.vocab.presentation.LessonAnswerResult
import com.example.visualvocab.feature.vocab.presentation.LessonQuestionType
import com.example.visualvocab.ui.theme.DiscoveryMintSoft
import com.example.visualvocab.ui.theme.ExplorerBlueDark
import com.example.visualvocab.ui.theme.FriendlyCoral
import com.example.visualvocab.ui.theme.FriendlyCoralSoft
import com.example.visualvocab.ui.theme.QuestGold
import com.example.visualvocab.ui.theme.QuestGoldSoft
import com.example.visualvocab.ui.theme.SuccessGreen

@Composable
fun VocabularyCard(
    vocabulary: Vocabulary?,
    questionType: LessonQuestionType,
    prompt: String,
    correctAnswer: String,
    options: List<String>,
    selectedAnswer: String?,
    answerResult: LessonAnswerResult?,
    questionNumber: Int,
    questionTotal: Int,
    isGenerating: Boolean,
    generationError: String?,
    showCloseButton: Boolean = true,
    onAnswerSelected: (String) -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onSpeakSpanish: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LessonHeader(
                questionNumber = questionNumber,
                questionTotal = questionTotal,
                showCloseButton = showCloseButton,
                onClose = onClose
            )

            when {
                isGenerating -> LoadingContent()
                !generationError.isNullOrBlank() && vocabulary == null -> ErrorContent(
                    message = generationError,
                    onRetry = onRetry
                )
                vocabulary != null -> QuestionContent(
                    vocabulary = vocabulary,
                    questionType = questionType,
                    prompt = prompt,
                    correctAnswer = correctAnswer,
                    options = options,
                    selectedAnswer = selectedAnswer,
                    answerResult = answerResult,
                    onAnswerSelected = onAnswerSelected,
                    onContinue = onContinue,
                    onSpeakEnglish = onSpeakEnglish,
                    onSpeakSpanish = onSpeakSpanish
                )
            }
        }
    }
}

@Composable
private fun LessonHeader(
    questionNumber: Int,
    questionTotal: Int,
    showCloseButton: Boolean,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Question ${questionNumber.coerceAtLeast(1)} of ${questionTotal.coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = {
                    if (questionTotal <= 0) 0f else {
                        questionNumber.toFloat().div(questionTotal).coerceIn(0f, 1f)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        if (showCloseButton) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close question")
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        ViviMascot(pose = ViviPose.SCANNING, modifier = Modifier.size(88.dp))
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        Text(
            text = "Vivi is building your challenge…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ViviMascot(pose = ViviPose.ENCOURAGING, modifier = Modifier.size(82.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRetry, shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Try again")
        }
    }
}

@Composable
private fun QuestionContent(
    vocabulary: Vocabulary,
    questionType: LessonQuestionType,
    prompt: String,
    correctAnswer: String,
    options: List<String>,
    selectedAnswer: String?,
    answerResult: LessonAnswerResult?,
    onAnswerSelected: (String) -> Unit,
    onContinue: () -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onSpeakSpanish: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ViviMascot(
                pose = when (answerResult) {
                    LessonAnswerResult.CORRECT -> ViviPose.CORRECT
                    LessonAnswerResult.INCORRECT -> ViviPose.ENCOURAGING
                    null -> if (questionType == LessonQuestionType.TAP_OBJECT) {
                        ViviPose.SCANNING
                    } else {
                        ViviPose.WELCOME
                    }
                },
                modifier = Modifier.size(72.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                if (questionType == LessonQuestionType.TAP_OBJECT && answerResult == null) {
                    Text(
                        text = "Look past this card and tap a box in the photo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (questionType != LessonQuestionType.TAP_OBJECT && answerResult == null) {
            options.forEach { option ->
                AnswerButton(
                    text = option,
                    onClick = { onAnswerSelected(option) }
                )
            }
        }

        if (questionType == LessonQuestionType.TAP_OBJECT && answerResult == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = QuestGoldSoft,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = ExplorerBlueDark
                    )
                    Text(
                        text = "Tap the matching object to submit your answer.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ExplorerBlueDark
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = answerResult != null,
            enter = fadeIn() + scaleIn(initialScale = 0.96f)
        ) {
            AnswerFeedback(
                vocabulary = vocabulary,
                correctAnswer = correctAnswer,
                selectedAnswer = selectedAnswer,
                result = answerResult ?: LessonAnswerResult.INCORRECT,
                onSpeakEnglish = onSpeakEnglish,
                onSpeakSpanish = onSpeakSpanish
            )
        }

        if (answerResult != null) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(17.dp)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                Icon(Icons.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun AnswerButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(17.dp)
    ) {
        Text(
            text = text.displayWord(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AnswerFeedback(
    vocabulary: Vocabulary,
    correctAnswer: String,
    selectedAnswer: String?,
    result: LessonAnswerResult,
    onSpeakEnglish: (String) -> Unit,
    onSpeakSpanish: (String) -> Unit
) {
    val correct = result == LessonAnswerResult.CORRECT
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (correct) DiscoveryMintSoft else FriendlyCoralSoft,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = if (correct) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    contentDescription = null,
                    tint = if (correct) SuccessGreen else FriendlyCoral
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (correct) "Correct!" else "Good try!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (correct) SuccessGreen else FriendlyCoral
                    )
                    if (!correct && !selectedAnswer.isNullOrBlank()) {
                        Text(
                            text = "You chose ${selectedAnswer.displayWord()}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = correctAnswer.displayWord(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { onSpeakEnglish(vocabulary.englishWord) }) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = null)
                    Spacer(Modifier.size(5.dp))
                    Text(vocabulary.englishWord.displayWord())
                }
                TextButton(onClick = { onSpeakSpanish(vocabulary.spanishWord) }) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = null)
                    Spacer(Modifier.size(5.dp))
                    Text(vocabulary.spanishWord.displayWord())
                }
            }

            if (vocabulary.englishSentence.isNotBlank()) {
                Text(
                    text = vocabulary.englishSentence,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (vocabulary.spanishSentence.isNotBlank()) {
                Text(
                    text = vocabulary.spanishSentence,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
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
    onChooseNewPhoto: () -> Unit
) {
    CompletionCard(
        title = "Quest complete!",
        subtitle = if (correctAnswers == totalQuestions && totalQuestions > 0) {
            "Perfect score — Vivi is impressed."
        } else {
            "$correctAnswers of $totalQuestions correct"
        },
        correctAnswers = correctAnswers,
        totalQuestions = totalQuestions,
        xpEarned = xpEarned,
        extraLabel = "$newWords new words",
        footer = when {
            dailyGoalReached -> "Daily goal complete"
            currentStreak > 0 -> "$currentStreak day streak"
            else -> "Keep exploring"
        },
        primaryText = "Scan another photo",
        primaryIcon = Icons.Rounded.PhotoCamera,
        onPrimary = onChooseNewPhoto,
        secondaryText = "Replay quest",
        onSecondary = onRestart
    )
}

@Composable
fun ReviewCompleteCard(
    correctAnswers: Int,
    totalQuestions: Int,
    xpEarned: Int,
    onRestart: () -> Unit,
    onDone: () -> Unit
) {
    CompletionCard(
        title = "Review complete!",
        subtitle = "$correctAnswers of $totalQuestions remembered",
        correctAnswers = correctAnswers,
        totalQuestions = totalQuestions,
        xpEarned = xpEarned,
        extraLabel = "Mastery updated",
        footer = "Come back when more words are due",
        primaryText = "Done",
        primaryIcon = Icons.Rounded.CheckCircle,
        onPrimary = onDone,
        secondaryText = "Review again",
        onSecondary = onRestart
    )
}

@Composable
private fun CompletionCard(
    title: String,
    subtitle: String,
    correctAnswers: Int,
    totalQuestions: Int,
    xpEarned: Int,
    extraLabel: String,
    footer: String,
    primaryText: String,
    primaryIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ViviMascot(pose = ViviPose.COMPLETE, modifier = Modifier.size(116.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                CompletionStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.CheckCircle,
                    value = "$correctAnswers/$totalQuestions",
                    label = "Correct",
                    color = SuccessGreen
                )
                CompletionStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Bolt,
                    value = "+$xpEarned",
                    label = "XP",
                    color = QuestGold
                )
                CompletionStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.EmojiEvents,
                    value = extraLabel.substringBefore(' '),
                    label = extraLabel.substringAfter(' ', extraLabel),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = footer,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ExplorerBlueDark
            )

            Button(
                onClick = onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(17.dp)
            ) {
                Icon(primaryIcon, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(primaryText, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onSecondary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(17.dp)
            ) {
                Icon(Icons.Rounded.Replay, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(secondaryText)
            }
        }
    }
}

@Composable
private fun CompletionStat(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.13f),
        shape = RoundedCornerShape(17.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun String.displayWord(): String = trim().replaceFirstChar { character ->
    if (character.isLowerCase()) character.titlecase() else character.toString()
}
