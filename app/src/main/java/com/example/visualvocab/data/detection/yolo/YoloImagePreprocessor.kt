package com.example.visualvocab.data.detection.yolo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.roundToInt

internal class YoloImagePreprocessor(
    private val inputSize: Int,
    private val dataLayout: DataLayout
) {

    enum class DataLayout {
        NHWC,
        NCHW
    }

    data class Result(
        val buffer: ByteBuffer,
        val scale: Float,
        val paddingX: Float,
        val paddingY: Float,
        val originalWidth: Int,
        val originalHeight: Int
    )

    fun process(
        bitmap: Bitmap
    ): Result {
        require(
            bitmap.width > 0 &&
                bitmap.height > 0
        ) {
            "Bitmap dimensions must be positive."
        }

        val source =
            if (
                bitmap.config ==
                Bitmap.Config.ARGB_8888
            ) {
                bitmap
            } else {
                bitmap.copy(
                    Bitmap.Config.ARGB_8888,
                    false
                )
            }

        val scale =
            min(
                inputSize /
                    source.width.toFloat(),
                inputSize /
                    source.height.toFloat()
            )

        val resizedWidth =
            (source.width * scale)
                .roundToInt()
                .coerceAtLeast(1)

        val resizedHeight =
            (source.height * scale)
                .roundToInt()
                .coerceAtLeast(1)

        val paddingX =
            (inputSize - resizedWidth) /
                2f

        val paddingY =
            (inputSize - resizedHeight) /
                2f

        val letterboxed =
            Bitmap.createBitmap(
                inputSize,
                inputSize,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(letterboxed)

        canvas.drawColor(
            Color.rgb(
                LETTERBOX_VALUE,
                LETTERBOX_VALUE,
                LETTERBOX_VALUE
            )
        )

        val resized =
            Bitmap.createScaledBitmap(
                source,
                resizedWidth,
                resizedHeight,
                true
            )

        canvas.drawBitmap(
            resized,
            paddingX,
            paddingY,
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
            )
        )

        val pixels =
            IntArray(
                inputSize * inputSize
            )

        letterboxed.getPixels(
            pixels,
            0,
            inputSize,
            0,
            0,
            inputSize,
            inputSize
        )

        val buffer =
            ByteBuffer.allocateDirect(
                1 *
                    inputSize *
                    inputSize *
                    RGB_CHANNELS *
                    FLOAT_BYTES
            ).order(
                ByteOrder.nativeOrder()
            )

        if (dataLayout == DataLayout.NHWC) {
            pixels.forEach { pixel ->
                buffer.putFloat(
                    Color.red(pixel) /
                        255f
                )

                buffer.putFloat(
                    Color.green(pixel) /
                        255f
                )

                buffer.putFloat(
                    Color.blue(pixel) /
                        255f
                )
            }
        } else {
            for (pixel in pixels) {
                buffer.putFloat(
                    Color.red(pixel) /
                        255f
                )
            }
            for (pixel in pixels) {
                buffer.putFloat(
                    Color.green(pixel) /
                        255f
                )
            }
            for (pixel in pixels) {
                buffer.putFloat(
                    Color.blue(pixel) /
                        255f
                )
            }
        }

        buffer.rewind()

        if (resized !== source) {
            resized.recycle()
        }

        letterboxed.recycle()

        return Result(
            buffer = buffer,
            scale = scale,
            paddingX = paddingX,
            paddingY = paddingY,
            originalWidth =
                source.width,
            originalHeight =
                source.height
        )
    }

    companion object {
        private const val RGB_CHANNELS =
            3

        private const val FLOAT_BYTES =
            4

        private const val LETTERBOX_VALUE =
            114
    }
}
