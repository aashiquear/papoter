package com.papoter.app.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ImageUploadTool {
    suspend fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = 1024): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)

                var scale = 1
                while (options.outWidth / scale > maxDimension || options.outHeight / scale > maxDimension) {
                    scale *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
                context.contentResolver.openInputStream(uri)?.use { decodeStream ->
                    val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
                    bitmap?.let {
                        val output = ByteArrayOutputStream()
                        it.compress(Bitmap.CompressFormat.JPEG, 85, output)
                        Base64.encodeToString(output.toByteArray(), Base64.DEFAULT)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }
}
