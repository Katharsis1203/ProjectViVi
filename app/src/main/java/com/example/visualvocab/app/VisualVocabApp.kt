package com.example.visualvocab.app

import android.app.Application
import com.example.visualvocab.app.di.AppModule

class VisualVocabApp : Application() {
    lateinit var appModule: AppModule

    override fun onCreate() {
        super.onCreate()
        appModule = AppModule(this)
    }
}
