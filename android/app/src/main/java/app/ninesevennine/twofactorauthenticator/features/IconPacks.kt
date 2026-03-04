package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import app.ninesevennine.twofactorauthenticator.utils.Logger
import org.json.JSONObject
import java.io.File

object IconPacks {
    private const val TAG = "IconPacks"

    data class PackInfo(val id: String, val primary: String, val secondary: String)

    fun getAvailable(context: Context): List<PackInfo> {
        val iconsDir = File(context.noBackupFilesDir, "icons")

        if (!iconsDir.exists()) return emptyList()

        return iconsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { packDir ->
                val metaFile = File(packDir, "metadata.json")

                if (!metaFile.exists()) {
                    packDir.deleteRecursively()
                    return@mapNotNull null
                }

                try {
                    val json = JSONObject(metaFile.readText())

//                    if (json.getInt("version") != 1) return@mapNotNull null

                    PackInfo(
                        id = packDir.name,
                        primary = json.getString("primary"),
                        secondary = json.optString("secondary", "")
                    )
                } catch (e: Exception) {
                    Logger.e(TAG, "failed to parse metadata.json in ${packDir.name}, deleting", e)
                    packDir.deleteRecursively()
                    null
                }
            }
            ?: emptyList()
    }

    fun delete(context: Context, id: String) {
        if (id.isEmpty()) return
        File(context.noBackupFilesDir, "icons/$id").deleteRecursively()
    }
}