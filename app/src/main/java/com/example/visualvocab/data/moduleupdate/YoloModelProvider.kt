package com.example.visualvocab.data.modelupdate

// this interface defines how the app gets its current YOLO model package.
interface YoloModelProvider {

    // get the source where the model files are located.
    fun getActiveSource(): YoloModelSource

    // get the version name of the current model.
    fun getActiveVersion(): String
}
