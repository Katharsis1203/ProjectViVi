package com.example.visualvocab.feature.vocab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.feature.vocab.presentation.LessonAnswerResult
import com.example.visualvocab.ui.theme.DiscoveryMint
import com.example.visualvocab.ui.theme.FriendlyCoral
import com.example.visualvocab.ui.theme.QuestGold
import com.example.visualvocab.ui.theme.SuccessGreen
import kotlin.math.abs

// this overlay highlights objects in learning mode so the user knows what they can tap.
@Composable
fun DetectionOverlay(
    detections: List<DetectionResult>,
    selectedDetection: DetectionResult?,
    correctDetection: DetectionResult? = null,
    completedDetections: List<DetectionResult> = emptyList(),
    answerResult: LessonAnswerResult? = null,
    imageWidth: Int,
    imageHeight: Int,
    useCropScale: Boolean = false,
    showLabels: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()
    val normalColor = MaterialTheme.colorScheme.primary
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (imageWidth <= 0 || imageHeight <= 0) return@Canvas

        // calculate scaling to match the photo.
        val widthScale = size.width / imageWidth.toFloat()
        val heightScale = size.height / imageHeight.toFloat()
        val scale = if (useCropScale) {
            maxOf(widthScale, heightScale)
        } else {
            minOf(widthScale, heightScale)
        }

        val offsetX = (size.width - imageWidth * scale) / 2f
        val offsetY = (size.height - imageHeight * scale) / 2f

        detections.forEach { detection ->
            // figure out where the box goes on the screen.
            val box = detection.boundingBox
            val left = box.left * scale + offsetX
            val top = box.top * scale + offsetY
            val right = box.right * scale + offsetX
            val bottom = box.bottom * scale + offsetY

            val isSelected = detectionsMatch(detection, selectedDetection)
            val isCorrectTarget = answerResult != null && detectionsMatch(detection, correctDetection)
            val isCompleted = completedDetections.any { detectionsMatch(detection, it) }

            // use different colors based on if it's selected, correct, or already done.
            val outlineColor = when {
                isSelected && answerResult == LessonAnswerResult.CORRECT -> SuccessGreen
                isSelected && answerResult == LessonAnswerResult.INCORRECT -> FriendlyCoral
                isCorrectTarget -> SuccessGreen
                isSelected -> QuestGold
                isCompleted -> DiscoveryMint
                else -> normalColor.copy(alpha = 0.74f)
            }

            val strokeWidth = when {
                isSelected || isCorrectTarget -> 3.5.dp.toPx()
                isCompleted -> 2.3.dp.toPx()
                else -> 1.6.dp.toPx()
            }

            if (isSelected || isCorrectTarget || isCompleted) {
                drawRoundRect(
                    color = outlineColor.copy(alpha = if (isSelected || isCorrectTarget) 0.18f else 0.10f),
                    topLeft = Offset(left, top),
                    size = Size(
                        width = (right - left).coerceAtLeast(0f),
                        height = (bottom - top).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
            }

            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(left, top),
                size = Size(
                    width = (right - left).coerceAtLeast(0f),
                    height = (bottom - top).coerceAtLeast(0f)
                ),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )

            // don't show the name if we are playing "tap the object".
            if (!showLabels) return@forEach

            val objectName = detection.label
                .trim()
                .replaceFirstChar { character ->
                    if (character.isLowerCase()) character.titlecase() else character.toString()
                }

            val label = when {
                isSelected && answerResult == LessonAnswerResult.CORRECT -> "$objectName  ✓"
                isSelected && answerResult == LessonAnswerResult.INCORRECT -> "$objectName  !"
                isCorrectTarget -> "$objectName  ✓"
                isSelected -> "$objectName  ?"
                isCompleted -> "$objectName  ✓"
                else -> objectName
            }

            val textLayout = textMeasurer.measure(text = label, style = labelStyle)
            val horizontalPadding = 9.dp.toPx()
            val verticalPadding = 5.dp.toPx()
            val labelWidth = textLayout.size.width + horizontalPadding * 2
            val labelHeight = textLayout.size.height + verticalPadding * 2
            val preferredTop = top - labelHeight - 7.dp.toPx()
            val labelTop = if (preferredTop >= 4.dp.toPx()) {
                preferredTop
            } else {
                (top + 7.dp.toPx()).coerceAtMost(size.height - labelHeight - 4.dp.toPx())
            }
            val labelLeft = left.coerceIn(
                4.dp.toPx(),
                (size.width - labelWidth - 4.dp.toPx()).coerceAtLeast(4.dp.toPx())
            )

            drawRoundRect(
                color = outlineColor.copy(alpha = if (isSelected || isCorrectTarget || isCompleted) 0.94f else 0.82f),
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(50.dp.toPx(), 50.dp.toPx())
            )

            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(
                    x = labelLeft + horizontalPadding,
                    y = labelTop + verticalPadding
                ),
                style = labelStyle
            )
        }
    }
}

private fun detectionsMatch(
    first: DetectionResult,
    second: DetectionResult?,
    tolerance: Float = 1f
): Boolean {
    if (second == null || first.label != second.label) return false

    return abs(first.boundingBox.left - second.boundingBox.left) <= tolerance &&
            abs(first.boundingBox.top - second.boundingBox.top) <= tolerance &&
            abs(first.boundingBox.right - second.boundingBox.right) <= tolerance &&
            abs(first.boundingBox.bottom - second.boundingBox.bottom) <= tolerance
}
