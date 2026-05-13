package app.ninesevennine.twofactorauthenticator

import android.app.Application
import app.ninesevennine.twofactorauthenticator.utils.AccountManagement
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.utils.Logger
import app.ninesevennine.twofactorauthenticator.utils.PrivateApi

private const val TAG = "Entry"

class Entry : Application() {
    companion object {
        lateinit var rustInterface: RustInterface
            private set
    }


    override fun onCreate() {
        super.onCreate()

        Logger.init(this)
        Logger.i(TAG, BuildConfig.APPLICATION_ID)
        Logger.i(TAG, BuildConfig.BUILD_TYPE)
        Logger.i(TAG, "${BuildConfig.VERSION_CODE} | ${BuildConfig.VERSION_NAME}")
        Logger.i(TAG, PrivateApi.getSystemProperty("ro.build.fingerprint"))

        runCatching {
            System.loadLibrary(RustInterface.LIBRARY_NAME)
        }.onSuccess {
            rustInterface = RustInterface()
            Logger.i(TAG, "${RustInterface.LIBRARY_NAME} loaded")
        }.onFailure { e ->
            Logger.e(TAG, "failed to load ${RustInterface.LIBRARY_NAME}", e)
            throw e
        }

        Config.load(this)

        if (!AccountManagement.hasAccounts(this)) {
            if (Config.isUsingAutomaticBackups) {
                val data = AccountManagement.getAccountsUsingAutomaticBackupsKey(this)
                if (data == null) {
                    Config.hasCompletedSetup = false
                }
            } else {
                Config.hasCompletedSetup = false
            }
        }
    }
}