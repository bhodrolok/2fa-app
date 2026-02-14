package app.ninesevennine.twofactorauthenticator

import android.app.Application
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.utils.Logger
import app.ninesevennine.twofactorauthenticator.utils.PrivateApi

private const val TAG = "Entry"

class Entry : Application() {
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
            Logger.i(TAG, "${RustInterface.LIBRARY_NAME} loaded")
        }.onFailure { e ->
            Logger.e(TAG, "failed to load ${RustInterface.LIBRARY_NAME}", e)
            throw e
        }

        Config.load(this)
    }
}