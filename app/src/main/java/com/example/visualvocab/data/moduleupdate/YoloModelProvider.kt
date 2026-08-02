package com.example.visualvocab.data.modelupdate

/**
 * Supplies the YOLO model package that should currently be used.
 *
 * The detector does not need to know whether the package came from the APK
 * or was downloaded after installation.
 */
interface YoloModelProvider {

    fun getActiveSource(): YoloModelSource

    fun getActiveVersion(): String
}
