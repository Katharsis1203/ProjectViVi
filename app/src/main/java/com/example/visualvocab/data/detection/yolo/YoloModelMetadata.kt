package com.example.visualvocab.data.detection.yolo

import android.content.Context
import com.example.visualvocab.data.modelupdate.YoloModelSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// this class holds info about the YOLO model, like what kind of objects it can find.
@Serializable
data class YoloModelMetadata(
    val names: Map<String, String>,
    val input_size: Int = 640,
    val base_model: String? = null
) {
    // a simple list of all the labels the model knows, in the right order.
    val classNames: List<String>
        get() =
            names.entries
                .sortedBy {
                    it.key.toIntOrNull()
                        ?: Int.MAX_VALUE
                }
                .map {
                    it.value
                }

    companion object {

        private val json =
            Json {
                ignoreUnknownKeys = true
            }

        // load the metadata from either the assets or a downloaded file.
        fun load(
            context: Context,
            source: YoloModelSource
        ): YoloModelMetadata {
            val jsonText =
                when (source) {
                    is YoloModelSource.Assets -> {
                        context.assets
                            .open(
                                source.metadataAssetName
                            )
                            .bufferedReader()
                            .use {
                                it.readText()
                            }
                    }

                    is YoloModelSource.Files -> {
                        require(
                            source.metadataFile.isFile
                        ) {
                            "YOLO metadata file does not exist: ${source.metadataFile.absolutePath}"
                        }

                        source.metadataFile
                            .readText()
                    }
                }

            return json.decodeFromString(
                jsonText
            )
        }
    }
}
