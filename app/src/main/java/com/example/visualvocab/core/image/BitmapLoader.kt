package com.example.visualvocab.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.visualvocab.core.common.AppDispatchers
import kotlinx.coroutines.withContext

// this helper just grabs an image from a URI and turns it into a bitmap so we can use it.
class BitmapLoader(
    private val context: Context,
    private val dispatchers: AppDispatchers
) {
    suspend fun loadFromUri(uri: Uri): Bitmap? = withContext(dispatchers.io) {
        // we open a stream to the file and decode it.
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    }
}
