package app.ninesevennine.twofactorauthenticator

import android.app.Application
import app.ninesevennine.twofactorauthenticator.features.management.ImportResult
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.utils.Signals

private lateinit var rustInterfaceInstance: RustInterface
val rustInterface: RustInterface
    get() = rustInterfaceInstance

class Entry : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            System.loadLibrary("appcore")
            rustInterfaceInstance = RustInterface()
        } catch (e: Throwable) {
            throw e
        }

        rustInterface.client_application_directory_set(dataDir.absolutePath)
        if (rustInterface.management_accounts_load() < ImportResult.SUCCESS.value) {
            Signals.requiresAuthentication = true
        }

        Config.load(this)
    }
}