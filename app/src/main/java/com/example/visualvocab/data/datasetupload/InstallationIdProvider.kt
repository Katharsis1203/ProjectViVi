package com.example.visualvocab.data.datasetupload

import android.content.Context
import java.util.UUID

// this helps us keep track of which phone is which without knowing who the user is.
class InstallationIdProvider(context: Context) {
    // we save a random ID in the phone's shared preferences.
    private val preferences = context.applicationContext.getSharedPreferences(
        "visual_vocab_installation",
        Context.MODE_PRIVATE
    )

    // gets the ID or makes a new one if it's the first time.
    fun getInstallationId(): String {
        preferences.getString("installation_id", null)?.takeIf { it.isNotBlank() }?.let {
            return it
        }

        // generate a new random ID using UUID.
        val generated = UUID.randomUUID().toString()
        preferences.edit().putString("installation_id", generated).apply()
        return generated
    }
}
