package com.example.visualvocab.app

import android.app.Application
import com.example.visualvocab.app.di.AppModule

// this is the main part of our app. i'm using it to start up the DI stuff right at the beginning.
class VisualVocabApp : Application() {
    lateinit var appModule: AppModule

    override fun onCreate() {
        super.onCreate()
        // setting up the app module here so the rest of the app can use it later.
        appModule = AppModule(this)
    }
}
