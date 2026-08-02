package com.example.visualvocab.data.datasetupload

import android.content.Context
import java.util.UUID

class InstallationIdProvider(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "visual_vocab_installation",
        Context.MODE_PRIVATE
    )

    fun getInstallationId(): String {
        preferences.getString("installation_id", null)?.takeIf { it.isNotBlank() }?.let {
            return it
        }

        val generated = UUID.randomUUID().toString()
        preferences.edit().putString("installation_id", generated).apply()
        return generated
    }
}
