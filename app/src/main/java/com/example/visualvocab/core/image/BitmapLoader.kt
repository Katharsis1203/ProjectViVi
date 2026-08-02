package com.example.visualvocab.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.visualvocab.core.common.AppDispatchers
import kotlinx.coroutines.withContext

class BitmapLoader(
    private val context: Context,
    private val dispatchers: AppDispatchers
) {
    suspend fun loadFromUri(uri: Uri): Bitmap? = withContext(dispatchers.io) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    }
}
