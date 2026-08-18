package com.example.visualvocab.data.training

import android.content.Context
import android.graphics.Bitmap
import com.example.visualvocab.domain.model.training.AnnotatedExample
import com.example.visualvocab.domain.model.training.TrainingAnnotation
import com.example.visualvocab.domain.repository.TrainingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// this repository handles saving and exporting images and labels for training our AI.
class TrainingRepositoryImpl(
    context: Context
) : TrainingRepository {

    private val appContext =
        context.applicationContext

    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    // main folder for training data.
    private val rootDirectory: File
        get() =
            File(
                appContext.filesDir,
                TRAINING_DIRECTORY
            )

    // folder where the photos are stored.
    private val imageDirectory: File
        get() =
            File(
                rootDirectory,
                IMAGE_DIRECTORY
            )

    // file that keeps track of all the metadata.
    private val metadataFile: File
        get() =
            File(
                rootDirectory,
                METADATA_FILE
            )

    // save a new photo and its labels to the phone.
    override suspend fun saveExample(
        bitmap: Bitmap,
        annotations:
        List<TrainingAnnotation>
    ) = withContext(Dispatchers.IO) {
        // at least one label is needed to save anything.
        require(
            annotations.isNotEmpty()
        ) {
            "Confirm at least one object first."
        }

        ensureDirectories()

        val exampleId =
            UUID.randomUUID()
                .toString()

        val imageFileName =
            "$exampleId.jpg"

        val imageFile =
            File(
                imageDirectory,
                imageFileName
            )

        // save the bitmap as a JPEG file.
        imageFile
            .outputStream()
            .use { output ->
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

        // create the metadata for this example.
        val storedExample =
            StoredTrainingExample(
                id = exampleId,
                imageFileName =
                    imageFileName,
                imageWidth =
                    bitmap.width,
                imageHeight =
                    bitmap.height,
                createdAt =
                    System.currentTimeMillis(),
                annotations =
                    annotations.map {
                        StoredTrainingAnnotation(
                            label =
                                it.label,
                            left =
                                it.left,
                            top =
                                it.top,
                            right =
                                it.right,
                            bottom =
                                it.bottom,
                            originalLabel =
                                it.originalLabel,
                            originalConfidence =
                                it.originalConfidence
                        )
                    }
            )

        // update the main dataset file.
        writeDataset(
            dataset.copy(
                examples =
                    dataset.examples +
                            storedExample
            )
        )
    }

    // get all the examples saved so far.
    override suspend fun getExamples(): List<AnnotatedExample> = withContext(Dispatchers.IO) {
        readDataset().examples.map { stored ->
            AnnotatedExample(
                id = stored.id,
                imageFileName = stored.imageFileName,
                imageWidth = stored.imageWidth,
                imageHeight = stored.imageHeight,
                createdAt = stored.createdAt,
                annotations = stored.annotations.map { ann ->
                    TrainingAnnotation(
                        label = ann.label,
                        left = ann.left,
                        top = ann.top,
                        right = ann.right,
                        bottom = ann.bottom,
                        originalLabel = ann.originalLabel,
                        originalConfidence = ann.originalConfidence
                    )
                }
            )
        }
    }

    // delete an example and its photo.
    override suspend fun deleteExample(id: String) = withContext(Dispatchers.IO) {
        val dataset = readDataset()
        val example = dataset.examples.find { it.id == id } ?: return@withContext

        val imageFile = File(imageDirectory, example.imageFileName)
        if (imageFile.exists()) {
            imageFile.delete()
        }

        writeDataset(
            dataset.copy(
                examples = dataset.examples.filter { it.id != id }
            )
        )
    }

    // bundle all the examples into a ZIP file to upload.
    override suspend fun exportDataset(
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        val dataset =
            readDataset()

        require(
            dataset.examples.isNotEmpty()
        ) {
            "No training examples have been saved."
        }

        // find all the unique labels used.
        val classNames =
            dataset.examples
                .flatMap {
                    it.annotations
                }
                .map {
                    normalizeLabel(
                        it.label
                    )
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .sorted()

        require(
            classNames.isNotEmpty()
        ) {
            "The training dataset contains no valid labels."
        }

        val classIds =
            classNames
                .mapIndexed {
                        index,
                        label ->

                    label to index
                }
                .toMap()

        val validExamples =
            dataset.examples
                .filter { example ->
                    File(
                        imageDirectory,
                        example.imageFileName
                    ).isFile
                }

        require(
            validExamples.isNotEmpty()
        ) {
            "The training dataset contains no image files."
        }

        val classesMetadata =
            YoloClassesMetadata(
                names =
                    classNames
                        .mapIndexed {
                                index,
                                name ->

                            index.toString() to
                                    name
                        }
                        .toMap()
            )

        val datasetYaml =
            createDatasetYaml(
                classNames
            )

        // start building the ZIP file.
        ZipOutputStream(
            outputStream.buffered()
        ).use { zip ->
            validExamples.forEach {
                    example ->

                val imageFile =
                    File(
                        imageDirectory,
                        example.imageFileName
                    )

                // add the photo to the ZIP.
                writeFileEntry(
                    zip = zip,
                    entryName =
                        "images/${example.imageFileName}",
                    file = imageFile
                )

                val labelFileName =
                    example.imageFileName
                        .substringBeforeLast(
                            '.'
                        ) +
                            ".txt"

                // create and add the YOLO format label file.
                val labelText =
                    createYoloLabelText(
                        example =
                            example,
                        classIds =
                            classIds
                    )

                writeTextEntry(
                    zip = zip,
                    entryName =
                        "labels/$labelFileName",
                    text =
                        labelText
                )
            }

            // add the configuration files for training.
            writeTextEntry(
                zip = zip,
                entryName =
                    "dataset.yaml",
                text =
                    datasetYaml
            )

            writeTextEntry(
                zip = zip,
                entryName =
                    "classes.json",
                text =
                    json.encodeToString(
                        classesMetadata
                    )
            )
        }
    }

    override suspend fun getExampleCount():
            Int =
        withContext(Dispatchers.IO) {
            readDataset()
                .examples
                .size
        }

    // helper to format labels into the specific text format YOLO expects.
    private fun createYoloLabelText(
        example:
        StoredTrainingExample,
        classIds:
        Map<String, Int>
    ): String {
        val lines =
            example.annotations
                .mapNotNull {
                        annotation ->

                    val normalizedLabel =
                        normalizeLabel(
                            annotation.label
                        )

                    val classId =
                        classIds[
                            normalizedLabel
                        ]
                            ?: return@mapNotNull null

                    val left =
                        annotation.left.coerceIn(0f, 1f)
                    val top =
                        annotation.top.coerceIn(0f, 1f)
                    val right =
                        annotation.right.coerceIn(0f, 1f)
                    val bottom =
                        annotation.bottom.coerceIn(0f, 1f)

                    val width = right - left
                    val height = bottom - top

                    if (width <= 0f || height <= 0f) {
                        return@mapNotNull null
                    }

                    val centerX = left + width / 2f
                    val centerY = top + height / 2f

                    // format: class_id center_x center_y width height (all normalized 0-1)
                    String.format(
                        Locale.US,
                        "%d %.6f %.6f %.6f %.6f",
                        classId,
                        centerX,
                        centerY,
                        width,
                        height
                    )
                }

        return lines.joinToString(
            separator = "\n",
            postfix = if (lines.isNotEmpty()) "\n" else ""
        )
    }

    // create the YAML configuration file for training.
    private fun createDatasetYaml(
        classNames:
        List<String>
    ): String {
        return buildString {
            appendLine("# Note: This dataset uses the same folder for train and val.")
            appendLine("# A proper split should be created externally before serious training.")
            appendLine("path: .")
            appendLine("train: images")
            appendLine("val: images")
            appendLine("names:")

            classNames.forEachIndexed {
                    index,
                    name ->

                append("  ")
                append(index)
                append(": ")
                appendLine(
                    quoteYamlString(
                        name
                    )
                )
            }
        }
    }

    // helper to make sure strings are safe for YAML.
    private fun quoteYamlString(
        value: String
    ): String {
        return "\"" +
                value
                    .replace(
                        "\\",
                        "\\\\"
                    )
                    .replace(
                        "\"",
                        "\\\""
                    ) +
                "\""
    }

    private fun writeFileEntry(
        zip: ZipOutputStream,
        entryName: String,
        file: File
    ) {
        zip.putNextEntry(
            ZipEntry(entryName)
        )

        file.inputStream()
            .use { input ->
                input.copyTo(zip)
            }

        zip.closeEntry()
    }

    private fun writeTextEntry(
        zip: ZipOutputStream,
        entryName: String,
        text: String
    ) {
        zip.putNextEntry(
            ZipEntry(entryName)
        )

        zip.write(
            text.toByteArray(
                Charsets.UTF_8
            )
        )

        zip.closeEntry()
    }

    // make labels consistent (lowercase, no extra spaces).
    private fun normalizeLabel(
        value: String
    ): String {
        return value
            .trim()
            .lowercase(
                Locale.ROOT
            )
    }

    private fun ensureDirectories() {
        rootDirectory.mkdirs()
        imageDirectory.mkdirs()
    }

    // read all the saved example metadata from the JSON file.
    private fun readDataset():
            StoredDataset {
        ensureDirectories()

        if (
            !metadataFile.exists() ||
            metadataFile
                .readText()
                .isBlank()
        ) {
            return StoredDataset()
        }

        return runCatching {
            json.decodeFromString<
                    StoredDataset
                    >(
                metadataFile.readText()
            )
        }.getOrElse {
            StoredDataset()
        }
    }

    // write all the example metadata back to the JSON file.
    private fun writeDataset(
        dataset: StoredDataset
    ) {
        ensureDirectories()

        metadataFile.writeText(
            json.encodeToString(
                dataset
            )
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
        val originalLabel:
        String? = null,
        val originalConfidence:
        Float? = null
    )

    @Serializable
    private data class YoloClassesMetadata(
        val names:
        Map<String, String>
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
