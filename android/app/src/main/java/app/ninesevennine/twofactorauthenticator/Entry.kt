package app.ninesevennine.twofactorauthenticator

import android.app.Application
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.utils.Logger

private lateinit var rustInterfaceInstance: RustInterface
val rustInterface: RustInterface
    get() = rustInterfaceInstance

class Entry : Application() {
    override fun onCreate() {
        super.onCreate()

        Logger.init(this)

        try {
            System.loadLibrary(RustInterface.LIBRARY_NAME)
            rustInterfaceInstance = RustInterface()
        } catch (e: Throwable) {
            throw e
        }

        rustInterface.client_application_directory_set(dataDir.absolutePath)
        rustInterface.management_save_test()

        Config.load(this)
    }
}