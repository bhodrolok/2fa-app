package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import app.ninesevennine.twofactorauthenticator.rustInterface
import app.ninesevennine.twofactorauthenticator.utils.ColorPicker
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.utils.Logger
import org.json.JSONObject
import java.io.File

enum class IconPacksStyle(val value: Int) {
    TEXTURED(0),
    TINTED(1);
}

object IconPacks {
    private const val TAG = "IconPacks"

    data class PackInfo(val id: String, val primary: String, val secondary: String)
    data class IconPackEntry(val file: String, val color: Color)
    data class IconPackResult(val bitmap: Bitmap?, val color: Color?)

    private var loadedIconsMap: Map<String, IconPackEntry> = emptyMap()

    fun load(context: Context, packId: String) {
        if (packId.isEmpty()) {
            loadedIconsMap = emptyMap()
            return
        }

        try {
            val metaFile = File(context.noBackupFilesDir, "icons/$packId/metadata.json")
            val json = JSONObject(metaFile.readText())
            val iconsArray = json.getJSONArray("icons")

            val newMap = mutableMapOf<String, IconPackEntry>()

            for (i in 0 until iconsArray.length()) {
                val entry = iconsArray.getJSONObject(i)
                val issuerArray = entry.getJSONArray("issuer")
                
                val iconFile = when (Config.UsingIconPacksStyle) {
                    IconPacksStyle.TEXTURED.value -> entry.optString("textured", "")
                    else -> entry.optString("tinted", "")
                }.takeIf { it.endsWith(".svg") } ?: ""

                val iconColor = (entry.optString("color", "").let { 
                    ColorPicker.fromHexString(it) 
                } ?: Color.White).copy(alpha = 1f)

                val packEntry = IconPackEntry(iconFile, iconColor)
                
                for (j in 0 until issuerArray.length()) {
                    newMap[issuerArray.getString(j).lowercase().trim()] = packEntry
                }
            }
            loadedIconsMap = newMap
        } catch (_: Exception) {
            Logger.e(TAG, "failed to load icons for pack $packId")
        }
    }

    fun get(context: Context, issuer: String): IconPackResult {
        val issuerNormalized = issuer.lowercase().trim()
        val icon = loadedIconsMap[issuerNormalized] ?: return IconPackResult(null, null)

        if (icon.file.isBlank()) return IconPackResult(null, icon.color)

        val svgFile = File(context.noBackupFilesDir, "icons/${Config.activeIconPackId}/assets/${icon.file}")
        if (!svgFile.exists()) return IconPackResult(null, icon.color)

        val sizePx = (52 * context.resources.displayMetrics.density + 0.5f).toInt()

        return try {
            val svgBytes = svgFile.readBytes()
            val bitmap = rustInterface.svgToBitmap(svgBytes, sizePx, sizePx)
            IconPackResult(bitmap, icon.color)
        } catch (_: Exception) {
            Logger.e(TAG, "Failed to load bitmap for $issuer")
            IconPackResult(null, icon.color)
        }
    }

    fun getAvailable(context: Context): List<PackInfo> {
        val iconsDir = File(context.noBackupFilesDir, "icons")
        if (!iconsDir.exists()) return emptyList()

        return iconsDir.listFiles()?.filter { it.isDirectory }?.mapNotNull { packDir ->
            val metaFile = File(packDir, "metadata.json")

            if (!metaFile.exists()) {
                packDir.deleteRecursively()
                return@mapNotNull null
            }

            try {
                val json = JSONObject(metaFile.readText())
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
        } ?: emptyList()
    }

    fun getActivePrimary(context: Context): String? {
        val currentId = Config.activeIconPackId
        if (currentId.isEmpty()) return null
        return getAvailable(context).firstOrNull { it.id == currentId }?.primary
    }

    fun delete(context: Context, id: String) {
        if (id.isEmpty()) return
        File(context.noBackupFilesDir, "icons/$id").deleteRecursively()
        if (Config.activeIconPackId == id) {
            loadedIconsMap = emptyMap()
        }
        Logger.i(TAG, "deleted pack $id")
    }
}