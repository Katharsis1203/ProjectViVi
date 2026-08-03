package com.example.visualvocab.feature.vocab.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visualvocab.domain.model.DetectionResult
import com.example.visualvocab.feature.vocab.presentation.AnnotationTool
import com.example.visualvocab.feature.vocab.presentation.EditableTrainingAnnotation
import java.util.Locale
import kotlin.math.abs

@Composable
fun AnnotationOverlay(
    detections: List<DetectionResult>,
    annotations: List<EditableTrainingAnnotation>,
    selectedAnnotationId: String?,
    tool: AnnotationTool,
    imageWidth: Int,
    imageHeight: Int,
    useCropScale: Boolean = true,
    onDetectionSelected: (DetectionResult) -> Unit,
    onAddAnnotation: (Float, Float, Float, Float) -> Unit,
    onUpdateAnnotation: (String, Float, Float, Float, Float) -> Unit,
    onSelectAnnotation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var drawingRect by remember { mutableStateOf<RectF?>(null) }
    var dragMode by remember { mutableStateOf<DragMode>(DragMode.None) }

    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val detectionColor = MaterialTheme.colorScheme.secondary
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(
                detections,
                annotations,
                selectedAnnotationId,
                tool,
                imageWidth,
                imageHeight,
                useCropScale
            ) {
                detectTapGestures { offset ->
                    if (tool != AnnotationTool.SELECT || imageWidth <= 0 || imageHeight <= 0) {
                        return@detectTapGestures
                    }

                    val transform = imageTransform(
                        containerWidth = size.width.toFloat(),
                        containerHeight = size.height.toFloat(),
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        useCropScale = useCropScale
                    )
                    val bitmapX = (offset.x - transform.offsetX) / transform.scale
                    val bitmapY = (offset.y - transform.offsetY) / transform.scale

                    val annotation = annotations.lastOrNull {
                        it.boundingBox.contains(bitmapX, bitmapY)
                    }
                    if (annotation != null) {
                        onSelectAnnotation(annotation.id)
                        return@detectTapGestures
                    }

                    detections
                        .filter { it.boundingBox.contains(bitmapX, bitmapY) }
                        .sortedWith(
                            compareBy<DetectionResult> {
                                it.boundingBox.width() * it.boundingBox.height()
                            }.thenByDescending { it.score }
                        )
                        .firstOrNull()
                        ?.let(onDetectionSelected)
                }
            }
            .pointerInput(
                annotations,
                selectedAnnotationId,
                tool,
                imageWidth,
                imageHeight,
                useCropScale
            ) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (imageWidth <= 0 || imageHeight <= 0) {
                            return@detectDragGestures
                        }

                        val transform = imageTransform(
                            containerWidth = size.width.toFloat(),
                            containerHeight = size.height.toFloat(),
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                            useCropScale = useCropScale
                        )
                        val bitmapX = ((offset.x - transform.offsetX) / transform.scale)
                            .coerceIn(0f, imageWidth.toFloat())
                        val bitmapY = ((offset.y - transform.offsetY) / transform.scale)
                            .coerceIn(0f, imageHeight.toFloat())

                        if (tool == AnnotationTool.DRAW) {
                            dragMode = DragMode.Create
                            drawingRect = RectF(bitmapX, bitmapY, bitmapX, bitmapY)
                            return@detectDragGestures
                        }

                        val selected = annotations.firstOrNull {
                            it.id == selectedAnnotationId
                        }
                        if (selected != null) {
                            val handle = getHandleAt(
                                rect = selected.boundingBox,
                                x = bitmapX,
                                y = bitmapY,
                                threshold = 30f / transform.scale
                            )
                            if (handle is DragMode.Resize) {
                                dragMode = handle
                                return@detectDragGestures
                            }
                        }

                        val tapped = annotations.lastOrNull {
                            it.boundingBox.contains(bitmapX, bitmapY)
                        }
                        if (tapped != null) {
                            onSelectAnnotation(tapped.id)
                            dragMode = DragMode.Move(tapped.id)
                        } else {
                            dragMode = DragMode.None
                        }
                    },
                    onDrag = { _, dragAmount ->
                        if (imageWidth <= 0 || imageHeight <= 0) {
                            return@detectDragGestures
                        }

                        val transform = imageTransform(
                            containerWidth = size.width.toFloat(),
                            containerHeight = size.height.toFloat(),
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                            useCropScale = useCropScale
                        )
                        val dx = dragAmount.x / transform.scale
                        val dy = dragAmount.y / transform.scale

                        when (val mode = dragMode) {
                            DragMode.Create -> {
                                drawingRect?.let { current ->
                                    drawingRect = RectF(
                                        current.left,
                                        current.top,
                                        (current.right + dx).coerceIn(0f, imageWidth.toFloat()),
                                        (current.bottom + dy).coerceIn(0f, imageHeight.toFloat())
                                    )
                                }
                            }

                            is DragMode.Move -> {
                                annotations
                                    .firstOrNull { it.id == mode.annotationId }
                                    ?.let { annotation ->
                                        val moved = RectF(annotation.boundingBox)
                                        moved.offset(dx, dy)
                                        keepInsideImage(
                                            rect = moved,
                                            width = imageWidth.toFloat(),
                                            height = imageHeight.toFloat()
                                        )
                                        onUpdateAnnotation(
                                            annotation.id,
                                            moved.left / imageWidth,
                                            moved.top / imageHeight,
                                            moved.right / imageWidth,
                                            moved.bottom / imageHeight
                                        )
                                    }
                            }

                            is DragMode.Resize -> {
                                annotations
                                    .firstOrNull { it.id == selectedAnnotationId }
                                    ?.let { annotation ->
                                        val resized = RectF(annotation.boundingBox)
                                        if (mode.left) {
                                            resized.left = (resized.left + dx)
                                                .coerceIn(0f, resized.right - 5f)
                                        }
                                        if (mode.top) {
                                            resized.top = (resized.top + dy)
                                                .coerceIn(0f, resized.bottom - 5f)
                                        }
                                        if (mode.right) {
                                            resized.right = (resized.right + dx)
                                                .coerceIn(
                                                    resized.left + 5f,
                                                    imageWidth.toFloat()
                                                )
                                        }
                                        if (mode.bottom) {
                                            resized.bottom = (resized.bottom + dy)
                                                .coerceIn(
                                                    resized.top + 5f,
                                                    imageHeight.toFloat()
                                                )
                                        }
                                        onUpdateAnnotation(
                                            annotation.id,
                                            resized.left / imageWidth,
                                            resized.top / imageHeight,
                                            resized.right / imageWidth,
                                            resized.bottom / imageHeight
                                        )
                                    }
                            }

                            DragMode.None -> Unit
                        }
                    },
                    onDragEnd = {
                        if (dragMode == DragMode.Create) {
                            drawingRect?.let { rect ->
                                rect.sort()
                                if (rect.width() > 5f && rect.height() > 5f) {
                                    onAddAnnotation(
                                        rect.left / imageWidth,
                                        rect.top / imageHeight,
                                        rect.right / imageWidth,
                                        rect.bottom / imageHeight
                                    )
                                }
                            }
                        }
                        drawingRect = null
                        dragMode = DragMode.None
                    },
                    onDragCancel = {
                        drawingRect = null
                        dragMode = DragMode.None
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (imageWidth <= 0 || imageHeight <= 0) {
                return@Canvas
            }

            val transform = imageTransform(
                containerWidth = size.width,
                containerHeight = size.height,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                useCropScale = useCropScale
            )

            fun drawLabel(
                text: String,
                anchorLeft: Float,
                anchorTop: Float,
                backgroundColor: Color
            ) {
                val safeText = text.take(28)
                val textLayout = textMeasurer.measure(
                    text = safeText,
                    style = labelStyle
                )
                val horizontalPadding = 8.dp.toPx()
                val verticalPadding = 4.dp.toPx()
                val labelWidth = textLayout.size.width + horizontalPadding * 2
                val labelHeight = textLayout.size.height + verticalPadding * 2
                val edgePadding = 4.dp.toPx()

                val maxLeft = (size.width - labelWidth - edgePadding)
                    .coerceAtLeast(edgePadding)
                val labelLeft = anchorLeft.coerceIn(edgePadding, maxLeft)
                val preferredTop = anchorTop - labelHeight - 5.dp.toPx()
                val maxTop = (size.height - labelHeight - edgePadding)
                    .coerceAtLeast(edgePadding)
                val labelTop = if (preferredTop >= edgePadding) {
                    preferredTop.coerceAtMost(maxTop)
                } else {
                    (anchorTop + 5.dp.toPx()).coerceIn(edgePadding, maxTop)
                }

                drawRoundRect(
                    color = backgroundColor.copy(alpha = 0.9f),
                    topLeft = Offset(labelLeft, labelTop),
                    size = Size(labelWidth, labelHeight),
                    cornerRadius = CornerRadius(50.dp.toPx())
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = safeText,
                    topLeft = Offset(
                        labelLeft + horizontalPadding,
                        labelTop + verticalPadding
                    ),
                    style = labelStyle
                )
            }

            if (tool == AnnotationTool.SELECT) {
                detections.forEach { detection ->
                    val box = detection.boundingBox
                    val left = box.left * transform.scale + transform.offsetX
                    val top = box.top * transform.scale + transform.offsetY
                    val right = box.right * transform.scale + transform.offsetX
                    val bottom = box.bottom * transform.scale + transform.offsetY

                    if (right < 0f || left > size.width || bottom < 0f || top > size.height) {
                        return@forEach
                    }

                    drawRoundRect(
                        color = detectionColor.copy(alpha = 0.06f),
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                    drawRoundRect(
                        color = detectionColor.copy(alpha = 0.82f),
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        style = Stroke(width = 1.5.dp.toPx()),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )

                    val label = String.format(
                        Locale.getDefault(),
                        "%s  %.0f%%",
                        detection.label,
                        detection.score * 100f
                    )
                    drawLabel(
                        text = label,
                        anchorLeft = left,
                        anchorTop = top,
                        backgroundColor = Color.Black
                    )
                }
            }

            annotations.forEach { annotation ->
                val box = annotation.boundingBox
                val left = box.left * transform.scale + transform.offsetX
                val top = box.top * transform.scale + transform.offsetY
                val right = box.right * transform.scale + transform.offsetX
                val bottom = box.bottom * transform.scale + transform.offsetY
                val isSelected = annotation.id == selectedAnnotationId
                val color = if (isSelected) selectedColor else primaryColor

                if (right < 0f || left > size.width || bottom < 0f || top > size.height) {
                    return@forEach
                }

                drawRoundRect(
                    color = color.copy(alpha = if (isSelected) 0.14f else 0.08f),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(
                        width = if (isSelected) 3.dp.toPx() else 2.2.dp.toPx()
                    ),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )

                val annotationLabel = annotation.label
                    .takeIf { it.isNotBlank() }
                    ?: "New object"
                drawLabel(
                    text = annotationLabel,
                    anchorLeft = left,
                    anchorTop = top,
                    backgroundColor = color
                )

                if (isSelected) {
                    val handleRadius = 6.dp.toPx()
                    listOf(
                        Offset(left, top),
                        Offset(right, top),
                        Offset(left, bottom),
                        Offset(right, bottom),
                        Offset(left, (top + bottom) / 2),
                        Offset(right, (top + bottom) / 2),
                        Offset((left + right) / 2, top),
                        Offset((left + right) / 2, bottom)
                    ).forEach { center ->
                        drawCircle(
                            color = color,
                            radius = handleRadius,
                            center = center
                        )
                    }
                }
            }

            drawingRect?.let { rect ->
                val sorted = RectF(rect).apply { sort() }
                val left = sorted.left * transform.scale + transform.offsetX
                val top = sorted.top * transform.scale + transform.offsetY
                val right = sorted.right * transform.scale + transform.offsetX
                val bottom = sorted.bottom * transform.scale + transform.offsetY

                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.14f),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = 2.5.dp.toPx()),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
            }
        }
    }
}

private data class ImageTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)

private fun imageTransform(
    containerWidth: Float,
    containerHeight: Float,
    imageWidth: Int,
    imageHeight: Int,
    useCropScale: Boolean
): ImageTransform {
    val widthScale = containerWidth / imageWidth.toFloat()
    val heightScale = containerHeight / imageHeight.toFloat()
    val scale = if (useCropScale) {
        maxOf(widthScale, heightScale)
    } else {
        minOf(widthScale, heightScale)
    }

    return ImageTransform(
        scale = scale,
        offsetX = (containerWidth - imageWidth * scale) / 2f,
        offsetY = (containerHeight - imageHeight * scale) / 2f
    )
}

private fun keepInsideImage(
    rect: RectF,
    width: Float,
    height: Float
) {
    if (rect.left < 0f) rect.offset(-rect.left, 0f)
    if (rect.top < 0f) rect.offset(0f, -rect.top)
    if (rect.right > width) rect.offset(width - rect.right, 0f)
    if (rect.bottom > height) rect.offset(0f, height - rect.bottom)
}

private sealed class DragMode {
    data object None : DragMode()
    data object Create : DragMode()
    data class Move(val annotationId: String) : DragMode()
    data class Resize(
        val left: Boolean = false,
        val top: Boolean = false,
        val right: Boolean = false,
        val bottom: Boolean = false
    ) : DragMode()
}

private fun getHandleAt(
    rect: RectF,
    x: Float,
    y: Float,
    threshold: Float
): DragMode {
    val nearLeft = abs(x - rect.left) <= threshold
    val nearRight = abs(x - rect.right) <= threshold
    val nearTop = abs(y - rect.top) <= threshold
    val nearBottom = abs(y - rect.bottom) <= threshold
    val insideHorizontal = x in (rect.left - threshold)..(rect.right + threshold)
    val insideVertical = y in (rect.top - threshold)..(rect.bottom + threshold)

    return when {
        nearLeft && nearTop -> DragMode.Resize(left = true, top = true)
        nearRight && nearTop -> DragMode.Resize(right = true, top = true)
        nearLeft && nearBottom -> DragMode.Resize(left = true, bottom = true)
        nearRight && nearBottom -> DragMode.Resize(right = true, bottom = true)
        nearLeft && insideVertical -> DragMode.Resize(left = true)
        nearRight && insideVertical -> DragMode.Resize(right = true)
        nearTop && insideHorizontal -> DragMode.Resize(top = true)
        nearBottom && insideHorizontal -> DragMode.Resize(bottom = true)
        else -> DragMode.None
    }
}