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
import java.util.Locale

@Composable
fun DetectionOverlay(
    detections: List<DetectionResult>,
    selectedDetection: DetectionResult?,
    imageWidth: Int,
    imageHeight: Int,
    useCropScale: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()

    val normalColor =
        MaterialTheme.colorScheme.primary

    val selectedColor =
        MaterialTheme.colorScheme.tertiary

    val labelStyle =
        MaterialTheme.typography.labelSmall.copy(
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        if (
            imageWidth <= 0 ||
            imageHeight <= 0
        ) {
            return@Canvas
        }

        val widthScale =
            size.width / imageWidth.toFloat()

        val heightScale =
            size.height / imageHeight.toFloat()

        val scale =
            if (useCropScale) {
                maxOf(
                    widthScale,
                    heightScale
                )
            } else {
                minOf(
                    widthScale,
                    heightScale
                )
            }

        val offsetX =
            (size.width -
                    imageWidth * scale) / 2f

        val offsetY =
            (size.height -
                    imageHeight * scale) / 2f

        detections.forEach { detection ->
            val box = detection.boundingBox

            val left =
                box.left * scale + offsetX

            val top =
                box.top * scale + offsetY

            val right =
                box.right * scale + offsetX

            val bottom =
                box.bottom * scale + offsetY

            val isSelected =
                detection == selectedDetection

            val outlineColor =
                if (isSelected) {
                    selectedColor
                } else {
                    normalColor.copy(alpha = 0.58f)
                }

            if (isSelected) {
                drawRoundRect(
                    color =
                        selectedColor.copy(alpha = 0.12f),
                    topLeft = Offset(left, top),
                    size = Size(
                        width = right - left,
                        height = bottom - top
                    ),
                    cornerRadius = CornerRadius(
                        16.dp.toPx(),
                        16.dp.toPx()
                    )
                )
            }

            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(left, top),
                size = Size(
                    width = right - left,
                    height = bottom - top
                ),
                cornerRadius = CornerRadius(
                    16.dp.toPx(),
                    16.dp.toPx()
                ),
                style = Stroke(
                    width =
                        if (isSelected) {
                            3.dp.toPx()
                        } else {
                            1.4.dp.toPx()
                        }
                )
            )

            val label =
                String.format(
                    Locale.getDefault(),
                    "%s  %.0f%%",
                    detection.label,
                    detection.score * 100f
                )

            val textLayout =
                textMeasurer.measure(
                    text = label,
                    style = labelStyle
                )

            val horizontalPadding =
                8.dp.toPx()

            val verticalPadding =
                5.dp.toPx()

            val labelWidth =
                textLayout.size.width +
                        horizontalPadding * 2

            val labelHeight =
                textLayout.size.height +
                        verticalPadding * 2

            val preferredTop =
                top -
                        labelHeight -
                        7.dp.toPx()

            val labelTop =
                if (preferredTop >= 0f) {
                    preferredTop
                } else {
                    top + 7.dp.toPx()
                }

            drawRoundRect(
                color =
                    Color.Black.copy(
                        alpha =
                            if (isSelected) {
                                0.78f
                            } else {
                                0.56f
                            }
                    ),
                topLeft = Offset(
                    x = left,
                    y = labelTop
                ),
                size = Size(
                    width = labelWidth,
                    height = labelHeight
                ),
                cornerRadius = CornerRadius(
                    50.dp.toPx(),
                    50.dp.toPx()
                )
            )

            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(
                    x = left + horizontalPadding,
                    y = labelTop + verticalPadding
                ),
                style = labelStyle
            )
        }
    }
}