package com.example.visualvocab.data.modelupdate

// this class holds info about a potential update and shows if it's newer than the current one.
data class ModelUpdateInfo(
    val manifest: ModelManifest,
    val currentVersion: String
) {
    val isNewer: Boolean
        get() =
            compareVersions(
                manifest.version,
                currentVersion
            ) > 0

    // a simple way to compare two version strings like "v1.2" and "v1.1".
    private fun compareVersions(
        first: String,
        second: String
    ): Int {
        val firstParts =
            first
                .removePrefix("v")
                .split(".")
                .map {
                    it.toIntOrNull() ?: 0
                }

        val secondParts =
            second
                .removePrefix("v")
                .removePrefix("bundled-")
                .split(".")
                .map {
                    it.toIntOrNull() ?: 0
                }

        val length =
            maxOf(
                firstParts.size,
                secondParts.size
            )

        for (index in 0 until length) {
            val firstValue =
                firstParts.getOrElse(index) {
                    0
                }

            val secondValue =
                secondParts.getOrElse(index) {
                    0
                }

            if (firstValue != secondValue) {
                return firstValue
                    .compareTo(secondValue)
            }
        }

        return 0
    }
}
