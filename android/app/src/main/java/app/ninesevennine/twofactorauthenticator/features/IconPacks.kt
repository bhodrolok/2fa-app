package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

private const val TAG = "IconPacks"

object IconPacks {
    fun extractDipToSlot0(context: Context) {
        val outDir = File(context.noBackupFilesDir, "icons/0")

        try {
            outDir.deleteRecursively()
            outDir.mkdirs()

            ZipInputStream(context.assets.open("dip.zip").buffered()).use { zip ->
                var entry = zip.nextEntry

                while (entry != null) {
                    val target = File(outDir, entry.name)

                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().buffered().use { zip.copyTo(it) }
                    }

                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "extractDipToSlot0", e)
            outDir.deleteRecursively()
        }
    }

    private const val MAX_ENTRIES = 32768
    private const val MAX_TOTAL_BYTES = 32L * 1024 * 1024 // 32 MB
    private const val MAX_ENTRY_BYTES = 1L * 1024 * 1024 // 1 MB

    fun extractStreamToSlot1(context: Context, input: InputStream): Boolean {
        val outDir = File(context.noBackupFilesDir, "icons/1")
        val root = outDir.canonicalPath + File.separator

        var entries = 0
        var total = 0L
        val buffer = ByteArray(64 * 1024)

        return try {
            outDir.deleteRecursively()
            outDir.mkdirs()

            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry

                while (entry != null) {
                    if (++entries > MAX_ENTRIES) throw IOException("too many entries")

                    val target = File(outDir, entry.name)

                    if (!target.canonicalPath.startsWith(root)) {
                        throw IOException("entry escapes target directory")
                    }

                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()

                        var written = 0L

                        target.outputStream().buffered().use { out ->
                            while (true) {
                                val read = zip.read(buffer)
                                if (read < 0) break

                                written += read
                                total += read

                                if (written > MAX_ENTRY_BYTES) throw IOException("entry too large")
                                if (total > MAX_TOTAL_BYTES) throw IOException("archive too large")

                                out.write(buffer, 0, read)
                            }
                        }
                    }

                    entry = zip.nextEntry
                }
            }

            File(outDir, "metadata.json").isFile
        } catch (e: Exception) {
            Log.e(TAG, "extractStreamToSlot1", e)
            outDir.deleteRecursively()
            false
        }
    }

    fun deleteSlot1(context: Context) {
        File(context.noBackupFilesDir, "icons/1").deleteRecursively()
    }
}