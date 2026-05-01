package app.ninesevennine.twofactorauthenticator.utils

import android.util.AtomicFile
import java.io.File

object AtomicFileSystem {
    fun write(file: File, data: ByteArray) {
        val atomicFile = AtomicFile(file)
        atomicFile.startWrite().use { fos ->
            fos.write(data)
            atomicFile.finishWrite(fos)
        }
    }

    fun read(file: File): ByteArray {
        return AtomicFile(file).readFully()
    }
}