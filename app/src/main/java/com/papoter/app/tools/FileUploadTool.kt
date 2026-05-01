package com.papoter.app.tools

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object FileUploadTool {
    suspend fun readTextFile(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    val content = reader.readText()
                    if (content.length > 50000) {
                        content.take(50000) + "\n\n[File truncated for brevity]"
                    } else {
                        content
                    }
                }
            } ?: "Could not read file."
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    suspend fun summarizeFile(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val content = readTextFile(context, uri)
        val fileName = getFileName(context, uri)
        "The user uploaded a file named '$fileName'. Here is its content:\n\n$content"
    }

    fun getFileName(context: Context, uri: Uri): String {
        var result = "unknown"
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) result = cursor.getString(idx) ?: result
                }
            }
        }
        return result
    }
}
