package app.ninesevennine.twofactorauthenticator.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object Document {
    fun getName(context: Context, uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return cursor.getString(nameIndex)
                }
            }
        }

        return null
    }
}