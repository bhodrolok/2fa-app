package app.ninesevennine.twofactorauthenticator.utils

import java.io.File
import java.io.InputStream
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

object Zip {
    private const val TAG = "Zip"

    fun unzipFromStream(inputStream: InputStream, destinationDir: File): Boolean{
        destinationDir.mkdirs()
        val canonicalDestination = destinationDir.canonicalPath + File.separator

        ZipInputStream(inputStream.buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outputFile = File(destinationDir, entry.name)

                if (!outputFile.canonicalPath.startsWith(canonicalDestination)) {
                    Logger.i(TAG, "skipping zip entry outside destination: ${entry.name}")
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue
                }

                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    try {
                        outputFile.outputStream().buffered().use { bos ->
                            zis.copyTo(bos)
                        }
                    } catch (e: ZipException) {
                        if (e.message?.contains("encrypted", ignoreCase = true) == true) {
                            Logger.i(TAG, "encrypted zip detected, rejecting and cleaning up")
                        } else {
                            Logger.e(TAG, "failed to extract zip entry: ${entry.name}", e)
                        }
                        destinationDir.deleteRecursively()

                        return false
                    } catch (e: Exception) {
                        Logger.e(TAG, "failed to extract zip entry: ${entry.name}", e)
                        destinationDir.deleteRecursively()

                        return false
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        return true
    }
}