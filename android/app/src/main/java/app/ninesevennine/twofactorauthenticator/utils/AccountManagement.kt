package app.ninesevennine.twofactorauthenticator.utils

import android.content.Context
import android.util.Base64
import app.ninesevennine.twofactorauthenticator.rustInterface

object AccountManagement {
    private const val TAG = "AccountManagement"

    private const val ACCOUNTS_FILE = "accounts.2fa"

    const val IMPORT_ERROR_NO_ACCOUNTS = 0
    const val IMPORT_ERROR_INCORRECT_PASSPHRASE = -1
    const val IMPORT_ERROR_INCORRECT_VERSION = -2
    const val IMPORT_ERROR_INVALID_FILE = -3
    const val IMPORT_ERROR_UNKNOWN = -4

    fun saveAccounts(context: Context): Boolean {
        return try {
            if (rustInterface.isClientUnlockPassphraseHashEmpty()) {
                throw Exception("unlock passphrase hash is empty")
            }

            val salt = Base64.decode(Config.unlockPassphraseSalt, Base64.DEFAULT)
            val data = rustInterface.exportAccountsUsingUnlockPassphraseHash(salt)

            val file = context.dataDir.resolve(ACCOUNTS_FILE)
            AtomicFileSystem.write(file, data.toByteArray(Charsets.UTF_8))

            true
        } catch (e: Exception) {
            Logger.e(TAG, "failed to save accounts", e)
            false
        }
    }

    fun loadAccounts(context: Context): Int {
        return try {
            val file = context.dataDir.resolve(ACCOUNTS_FILE)
            val data = AtomicFileSystem.read(file)

            rustInterface.importAccountsUsingUnlockPassphraseHash(data.toString(Charsets.UTF_8))
        } catch (e: Exception) {
            Logger.e(TAG, "failed to load accounts", e)
            IMPORT_ERROR_UNKNOWN
        }
    }
}