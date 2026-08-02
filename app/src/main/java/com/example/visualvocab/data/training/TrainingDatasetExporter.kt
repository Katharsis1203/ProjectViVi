package com.example.visualvocab.data.training

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

internal class TrainingDatasetExporter(
    private val rootDirectory: File,
    private val imageDirectory: File,
    private val metadataFile: File,
    private val json: Json
) {

    suspend fun export(
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        validateDatasetFiles()

        val storedDataset =
            json.decodeFromString<StoredDataset>(
                metadataFile.readText()
            )

        if (storedDataset.examples.isEmpty()) {
            throw IllegalStateException(
                "No training examples have been saved."
            )
        }

        val categories =
            storedDataset.examples
                .flatMap { example ->
                    example.annotations
                }
                .map { annotation ->
                    annotation.label
                        .trim()
                        .lowercase()
                }
                .filter { label ->
                    label.isNotBlank()
                }
                .distinct()
                .sorted()
                .mapIndexed { index, label ->
                    CocoCategory(
                        id = index + 1,
                        name = label
                    )
                }

        if (categories.isEmpty()) {
            throw IllegalStateException(
                "The training dataset contains no valid labels."
            )
        }

        val categoryIds =
            categories.associate { category ->
                category.name to category.id
            }

        val cocoImages =
            storedDataset.examples.mapIndexed {
                    index,
                    example ->

                CocoImage(
                    id = index + 1,
                    fileName =
                        example.imageFileName,
                    width =
                        example.imageWidth,
                    height =
                        example.imageHeight
                )
            }

        var nextAnnotationId = 1

        val cocoAnnotations =
            storedDataset.examples.flatMapIndexed {
                    imageIndex,
                    example ->

                example.annotations.mapNotNull {
                        annotation ->

                    val normalizedLabel =
                        annotation.label
                            .trim()
                            .lowercase()

                    val categoryId =
                        categoryIds[normalizedLabel]
                            ?: return@mapNotNull null

                    val left =
                        annotation.left
                            .coerceIn(0f, 1f) *
                                example.imageWidth

                    val top =
                        annotation.top
                            .coerceIn(0f, 1f) *
                                example.imageHeight

                    val right =
                        annotation.right
                            .coerceIn(0f, 1f) *
                                example.imageWidth

                    val bottom =
                        annotation.bottom
                            .coerceIn(0f, 1f) *
                                example.imageHeight

                    val width =
                        (right - left)
                            .coerceAtLeast(0f)

                    val height =
                        (bottom - top)
                            .coerceAtLeast(0f)

                    if (
                        width <= 0f ||
                        height <= 0f
                    ) {
                        return@mapNotNull null
                    }

                    CocoAnnotation(
                        id = nextAnnotationId++,
                        imageId =
                            imageIndex + 1,
                        categoryId =
                            categoryId,
                        boundingBox =
                            listOf(
                                left.roundToInt(),
                                top.roundToInt(),
                                width.roundToInt(),
                                height.roundToInt()
                            ),
                        area =
                            width * height,
                        isCrowd = 0
                    )
                }
            }

        if (cocoAnnotations.isEmpty()) {
            throw IllegalStateException(
                "The training dataset contains no valid annotations."
            )
        }

        val cocoDataset =
            CocoDataset(
                images = cocoImages,
                annotations =
                    cocoAnnotations,
                categories =
                    categories
            )

        ZipOutputStream(
            outputStream.buffered()
        ).use { zip ->
            storedDataset.examples.forEach {
                    example ->

                val imageFile =
                    File(
                        imageDirectory,
                        example.imageFileName
                    )

                if (!imageFile.exists()) {
                    return@forEach
                }

                zip.putNextEntry(
                    ZipEntry(
                        "images/" +
                                example.imageFileName
                    )
                )

                imageFile
                    .inputStream()
                    .use { input ->
                        input.copyTo(zip)
                    }

                zip.closeEntry()
            }

            zip.putNextEntry(
                ZipEntry(
                    "annotations.json"
                )
            )

            zip.write(
                json.encodeToString(
                    cocoDataset
                ).toByteArray(
                    Charsets.UTF_8
                )
            )

            zip.closeEntry()
        }
    }

    private fun validateDatasetFiles() {
        if (!rootDirectory.exists()) {
            throw IllegalStateException(
                "The training dataset directory does not exist."
            )
        }

        if (!metadataFile.exists()) {
            throw IllegalStateException(
                "No training examples have been saved."
            )
        }

        if (metadataFile.readText().isBlank()) {
            throw IllegalStateException(
                "The training metadata file is empty."
            )
        }

        if (!imageDirectory.exists()) {
            throw IllegalStateException(
                "The training image directory does not exist."
            )
        }
    }

    /*
     * Local representations of the repository's stored JSON.
     *
     * Keeping these classes here avoids depending on private nested
     * classes declared inside TrainingRepositoryImpl.
     */
    @Serializable
    private data class StoredDataset(
        val examples:
        List<StoredTrainingExample> =
            emptyList()
    )

    @Serializable
    private data class StoredTrainingExample(
        val id: String,
        val imageFileName: String,
        val imageWidth: Int,
        val imageHeight: Int,
        val createdAt: Long,
        val annotations:
        List<StoredTrainingAnnotation> =
            emptyList()
    )

    @Serializable
    private data class StoredTrainingAnnotation(
        val label: String,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val originalLabel: String? = null,
        val originalConfidence: Float? = null
    )

    @Serializable
    private data class CocoDataset(
        val images:
        List<CocoImage>,

        val annotations:
        List<CocoAnnotation>,

        val categories:
        List<CocoCategory>
    )

    @Serializable
    private data class CocoImage(
        val id: Int,

        @SerialName("file_name")
        val fileName: String,

        val width: Int,
        val height: Int
    )

    @Serializable
    private data class CocoAnnotation(
        val id: Int,

        @SerialName("image_id")
        val imageId: Int,

        @SerialName("category_id")
        val categoryId: Int,

        @SerialName("bbox")
        val boundingBox:
        List<Int>,

        val area: Float,

        @SerialName("iscrowd")
        val isCrowd: Int
    )

    @Serializable
    private data class CocoCategory(
        val id: Int,
        val name: String
    )
}