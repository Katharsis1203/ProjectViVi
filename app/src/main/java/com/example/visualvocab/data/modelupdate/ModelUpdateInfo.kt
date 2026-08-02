package com.example.visualvocab.data.modelupdate

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
