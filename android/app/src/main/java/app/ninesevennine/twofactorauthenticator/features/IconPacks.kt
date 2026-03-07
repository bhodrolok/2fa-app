package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.utils.ColorPicker
import app.ninesevennine.twofactorauthenticator.utils.Logger
import org.json.JSONObject
import java.io.File

object IconPacks {
    private const val TAG = "IconPacks"

    data class PackInfo(val id: String, val primary: String, val secondary: String)
    data class IconEntry(val issuers: List<String>, val file: String, val color: Color)
    data class IconResult(val bitmap: Bitmap, val color: Color)

    private var loadedIcons: List<IconEntry> = emptyList()

    fun load(context: Context, packId: String) {
        if (packId.isEmpty()) {
            loadedIcons = emptyList()
            return
        }

        try {
            val metaFile = File(context.noBackupFilesDir, "icons/$packId/metadata.json")
            val json = JSONObject(metaFile.readText())
            val iconsArray = json.getJSONArray("icons")

            loadedIcons = (0 until iconsArray.length()).map { i ->
                val entry = iconsArray.getJSONObject(i)
                val issuerArray = entry.getJSONArray("issuer")
                IconEntry(
                    issuers = (0 until issuerArray.length()).map { issuerArray.getString(it) },
                    file = entry.optString("file", "").takeIf { it.endsWith(".svg") } ?: "",
                    color = (entry.optString("color", "").let { ColorPicker.fromHex(it) } ?: Color.White).copy(alpha = 1f))
            }
        } catch (_: Exception) {
            Logger.e(TAG, "failed to load icons for pack $packId")
        }
    }

    fun get(context: Context, issuer: String): IconResult? {
        val normalized = issuer.lowercase().trim()
        val entry = loadedIcons.firstOrNull { it.issuers.any { s -> s == normalized } }
            ?: return null

        if (entry.file.isBlank()) return null

        val svgFile = File(context.noBackupFilesDir, "icons/${Config.activeIconPackId}/assets/${entry.file}")
        if (!svgFile.exists()) return null

        val sizePx = (52 * context.resources.displayMetrics.density + 0.5f).toInt()

        return try {
            val svgBytes = svgFile.readBytes()
            val bitmap = Entry.rustInterface.svgToBitmap(svgBytes, sizePx, sizePx) ?: return null
            IconResult(bitmap, entry.color)
        } catch (_: Exception) {
            Logger.e(TAG, "Failed to load bitmap for $issuer")
            null
        }
    }

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
                } catch (_: Exception) {
                    Logger.e(TAG, "failed to parse metadata.json in ${packDir.name}, deleting")
                    packDir.deleteRecursively()
                    null
                }
            }
            ?: emptyList()
    }

    fun delete(context: Context, id: String) {
        if (id.isEmpty()) return

        File(context.noBackupFilesDir, "icons/$id").deleteRecursively()

        if (Config.activeIconPackId == id) {
            loadedIcons = emptyList()
        }

        Logger.i(TAG, "deleted pack $id")
    }
}