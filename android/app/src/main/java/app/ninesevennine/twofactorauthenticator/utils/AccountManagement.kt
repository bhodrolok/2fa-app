package app.ninesevennine.twofactorauthenticator.utils

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import app.ninesevennine.twofactorauthenticator.Entry
import java.io.File

object AccountManagement {
    private const val TAG = "AccountManagement"

    private const val ACCOUNTS_USING_KEY_FILE = "accounts_using_key.2fa"
    private const val ACCOUNTS_USING_KEY_FILE_OLD = "accounts_using_seed.2fa"

    private const val ACCOUNTS_FILE = "accounts.2fa"
    private const val ACCOUNTS_USING_AUTOMATIC_BACKUPS_KEY_FILE = "accounts_using_automatic_backups_key.2fa"
    private const val ACCOUNTS_USING_AUTOMATIC_BACKUPS_KEY_FILE_OLD = "accounts_using_automatic_internal_backups.2fa"

    const val IMPORT_ERROR_NO_ACCOUNTS = 0
    const val IMPORT_ERROR_INCORRECT_PASSPHRASE = -1
    const val IMPORT_ERROR_INCORRECT_VERSION = -2
    const val IMPORT_ERROR_INVALID_FILE = -3
    const val IMPORT_ERROR_UNKNOWN = -4

    private fun atomicRead(file: File): String {
        return AtomicFile(file).readFully().toString(Charsets.UTF_8)
    }

    private fun resolveFile(dir: File, name: String, nameOld: String): File? {
        val file = File(dir, name)
        if (file.exists()) return file

        val fileOld = File(dir, nameOld)
        if (fileOld.exists()) return fileOld

        return null
    }

    fun loadAccountsUsingKey(context: Context): Boolean {
        return try {
            val dir = context.applicationContext.noBackupFilesDir
            val target = resolveFile(dir, ACCOUNTS_USING_KEY_FILE, ACCOUNTS_USING_KEY_FILE_OLD)
                ?: return true

            val accountsData = atomicRead(target)

            val result = Entry.rustInterface.importAccountsUsingKey003(accountsData)

            when (result) {
                IMPORT_ERROR_NO_ACCOUNTS -> Logger.i(TAG, "no accounts to import") // not an error
                IMPORT_ERROR_INCORRECT_PASSPHRASE -> Logger.e(TAG, "incorrect passphrase")
                IMPORT_ERROR_INCORRECT_VERSION -> Logger.e(TAG, "incorrect version")
                IMPORT_ERROR_INVALID_FILE -> Logger.e(TAG, "invalid file")
                IMPORT_ERROR_UNKNOWN -> Logger.e(TAG, "unknown error")
                else -> if (result < 0) Logger.e(TAG, "unexpected error code $result")
            }

            result >= 0
        } catch (e: Exception) {
            Logger.e(TAG, "failed to loadAccountsUsingKey", e)
            false
        }
    }

    fun saveAccounts(context: Context): Boolean {
        return try {
            if (Entry.rustInterface.isClientSecretEmpty()) {
                throw Exception("unlock passphrase hash is empty")
            }

            val salt = Base64.decode(Config.unlockPassphraseSalt, Base64.DEFAULT)
            val data = Entry.rustInterface.exportAccountsUsingUnlockPassphraseHash(salt)

            val file = context.dataDir.resolve(ACCOUNTS_FILE)
            AtomicFileSystem.write(file, data.toByteArray(Charsets.UTF_8))

            true
        } catch (e: Exception) {
            Logger.e(TAG, "failed to save accounts", e)
            false
        }
    }

    fun hasAccounts(context: Context): Boolean {
        val file = context.dataDir.resolve(ACCOUNTS_FILE)
        return file.exists()
    }

    fun loadAccounts(context: Context): Int {
        return try {
            val file = context.dataDir.resolve(ACCOUNTS_FILE)
            val data = AtomicFileSystem.read(file)

            Entry.rustInterface.importAccountsUsingUnlockPassphraseHash(data.toString(Charsets.UTF_8))
        } catch (e: Exception) {
            Logger.e(TAG, "failed to load accounts", e)
            IMPORT_ERROR_UNKNOWN
        }
    }

    fun getAccountsUsingAutomaticBackupsKey(context: Context): String? {
        return try {
            val dir = context.applicationContext.noBackupFilesDir
            val target = resolveFile(dir, ACCOUNTS_USING_AUTOMATIC_BACKUPS_KEY_FILE, ACCOUNTS_USING_AUTOMATIC_BACKUPS_KEY_FILE_OLD)
                ?: return null

            atomicRead(target)
        } catch (e: Exception) {
            Logger.e(TAG, "failed to getAccountsUsingAutomaticBackupsKey", e)
            null
        }
    }
}