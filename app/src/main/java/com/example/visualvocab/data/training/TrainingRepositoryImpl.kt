package com.example.visualvocab.data.training

import android.content.Context
import android.graphics.Bitmap
import com.example.visualvocab.domain.model.training.TrainingAnnotation
import com.example.visualvocab.domain.repository.TrainingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

class TrainingRepositoryImpl(
    context: Context
) : TrainingRepository {

    private val appContext =
        context.applicationContext

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val rootDirectory: File
        get() = File(
            appContext.filesDir,
            TRAINING_DIRECTORY
        )

    private val imageDirectory: File
        get() = File(
            rootDirectory,
            IMAGE_DIRECTORY
        )

    private val metadataFile: File
        get() = File(
            rootDirectory,
            METADATA_FILE
        )

    override suspend fun saveExample(
        bitmap: Bitmap,
        annotations: List<TrainingAnnotation>
    ) = withContext(Dispatchers.IO) {
        require(annotations.isNotEmpty()) {
            "Confirm at least one object first."
        }

        ensureDirectories()

        val exampleId =
            UUID.randomUUID().toString()

        val imageFileName =
            "$exampleId.jpg"

        val imageFile =
            File(
                imageDirectory,
                imageFileName
            )

        imageFile.outputStream().use { output ->
            val saved =
                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    JPEG_QUALITY,
                    output
                )

            check(saved) {
                "The training image could not be saved."
            }
        }

        val dataset =
            readDataset()

        val storedExample =
            StoredTrainingExample(
                id = exampleId,
                imageFileName = imageFileName,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height,
                createdAt =
                    System.currentTimeMillis(),
                annotations =
                    annotations.map {
                        StoredTrainingAnnotation(
                            label = it.label,
                            left = it.left,
                            top = it.top,
                            right = it.right,
                            bottom = it.bottom,
                            originalLabel =
                                it.originalLabel,
                            originalConfidence =
                                it.originalConfidence
                        )
                    }
            )

        writeDataset(
            dataset.copy(
                examples =
                    dataset.examples +
                            storedExample
            )
        )
    }

    override suspend fun exportDataset(
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        val dataset =
            readDataset()

        require(dataset.examples.isNotEmpty()) {
            "No training examples have been saved."
        }

        val categories =
            dataset.examples
                .flatMap {
                    it.annotations
                }
                .map {
                    it.label.trim().lowercase()
                }
                .distinct()
                .sorted()
                .mapIndexed { index, label ->
                    CocoCategory(
                        id = index + 1,
                        name = label
                    )
                }

        val categoryIds =
            categories.associate {
                it.name to it.id
            }

        val cocoImages =
            dataset.examples.mapIndexed {
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
            dataset.examples.flatMapIndexed {
                    imageIndex,
                    example ->

                example.annotations.map {
                        annotation ->

                    val x =
                        annotation.left *
                                example.imageWidth

                    val y =
                        annotation.top *
                                example.imageHeight

                    val width =
                        (
                                annotation.right -
                                        annotation.left
                                ) *
                                example.imageWidth

                    val height =
                        (
                                annotation.bottom -
                                        annotation.top
                                ) *
                                example.imageHeight

                    CocoAnnotation(
                        id = nextAnnotationId++,
                        imageId =
                            imageIndex + 1,
                        categoryId =
                            categoryIds.getValue(
                                annotation.label
                                    .trim()
                                    .lowercase()
                            ),
                        boundingBox =
                            listOf(
                                x.roundToInt(),
                                y.roundToInt(),
                                width.roundToInt(),
                                height.roundToInt()
                            ),
                        area =
                            width * height,
                        isCrowd = 0
                    )
                }
            }

        val cocoDataset =
            CocoDataset(
                images = cocoImages,
                annotations =
                    cocoAnnotations,
                categories = categories
            )

        ZipOutputStream(
            outputStream.buffered()
        ).use { zip ->
            dataset.examples.forEach {
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

                imageFile.inputStream().use {
                        input ->

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
                ).toByteArray()
            )

            zip.closeEntry()
        }
    }

    override suspend fun getExampleCount(): Int =
        withContext(Dispatchers.IO) {
            readDataset().examples.size
        }

    private fun ensureDirectories() {
        rootDirectory.mkdirs()
        imageDirectory.mkdirs()
    }

    private fun readDataset(): StoredDataset {
        ensureDirectories()

        if (
            !metadataFile.exists() ||
            metadataFile.readText().isBlank()
        ) {
            return StoredDataset()
        }

        return runCatching {
            json.decodeFromString<StoredDataset>(
                metadataFile.readText()
            )
        }.getOrElse {
            StoredDataset()
        }
    }

    private fun writeDataset(
        dataset: StoredDataset
    ) {
        ensureDirectories()

        metadataFile.writeText(
            json.encodeToString(dataset)
        )
    }

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
        List<StoredTrainingAnnotation>
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
        val images: List<CocoImage>,
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
        val boundingBox: List<Int>,

        val area: Float,

        @SerialName("iscrowd")
        val isCrowd: Int
    )

    @Serializable
    private data class CocoCategory(
        val id: Int,
        val name: String
    )

    companion object {
        private const val TRAINING_DIRECTORY =
            "training_dataset"

        private const val IMAGE_DIRECTORY =
            "images"

        private const val METADATA_FILE =
            "dataset.json"

        private const val JPEG_QUALITY =
            92
    }
}