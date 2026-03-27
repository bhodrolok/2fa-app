package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import android.util.Base64
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.utils.Logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object AccountManagement {
    private const val TAG = "AccountManagement"
    private const val ACCOUNTS_USING_KEY_FILE = "accounts_using_key.2fa"
    private const val ACCOUNTS_USING_KEY_FILE_OLD = "accounts_using_seed.2fa"
    private const val ACCOUNTS_USING_AUTOMATIC_BACKUPS_KEY_FILE = "accounts_using_automatic_backups_key.2fa"
    private const val ACCOUNTS_USING_AUTOMATIC_BACKUPS_KEY_FILE_OLD = "accounts_using_automatic_internal_backups.2fa"

    const val IMPORT_ERROR_NO_ACCOUNTS = 0
    const val IMPORT_ERROR_INCORRECT_PASSPHRASE = -1
    const val IMPORT_ERROR_OUTDATED_VERSION = -2
    const val IMPORT_ERROR_INVALID_FILE = -3
    const val IMPORT_ERROR_UNKNOWN = -4

    private fun forceWrite(file: File, data: String) {
        val fos = FileOutputStream(file)
        fos.use { fos ->
            fos.write(data.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.fd.sync()
        }
    }

    private fun forceRead(file: File): String {
        val fis = FileInputStream(file)
        fis.use { fis ->
            return fis.readBytes().toString(Charsets.UTF_8)
        }
    }

    fun saveAccountsUsingKey(context: Context) {
        return try {
            val accountsData = if (Config.hasMigratedToSecret002) {
                Entry.rustInterface.exportAccountsUsingKey002()
            } else {
                Entry.rustInterface.exportAccountsUsingSeed001()
            }

            forceWrite(
                File(context.applicationContext.noBackupFilesDir, ACCOUNTS_USING_KEY_FILE),
                accountsData
            )
        } catch (e: Exception) {
            Logger.e(TAG, "failed to saveAccountsUsingKey", e)
        }
    }

    fun loadAccountsUsingKey(context: Context): Boolean {
        return try {
            val dir = context.applicationContext.noBackupFilesDir
            val file = File(dir, ACCOUNTS_USING_KEY_FILE)
            val fileOld = File(dir, ACCOUNTS_USING_KEY_FILE_OLD)

            val target = when {
                file.exists() -> file
                fileOld.exists() -> fileOld
                else -> return true
            }

            val accountsData = forceRead(target)

            val result = if (Config.hasMigratedToSecret002) {
                Entry.rustInterface.importAccountsUsingKey002(accountsData)
            } else {
                Entry.rustInterface.importAccountsUsingSeed001(accountsData)
            }

            when (result) {
                IMPORT_ERROR_NO_ACCOUNTS -> Logger.e(TAG, "no accounts to import")
                IMPORT_ERROR_INCORRECT_PASSPHRASE -> Logger.e(TAG, "incorrect passphrase")
                IMPORT_ERROR_OUTDATED_VERSION -> Logger.e(TAG, "outdated version")
                IMPORT_ERROR_INVALID_FILE -> Logger.e(TAG, "invalid file")
                IMPORT_ERROR_UNKNOWN -> Logger.e(TAG, "unknown error")
                else -> if (result < 0) Logger.e(TAG, "unexpected error code $result")
            }

            result > 0
        } catch (e: Exception) {
            Logger.e(TAG, "failed to loadAccountsUsingKey", e)
            false
        }
    }

    fun deleteAccountsUsingKey(context: Context) {
        runCatching {
            val dir = context.applicationContext.noBackupFilesDir
            val file = File(dir, ACCOUNTS_USING_KEY_FILE)
            val fileOld = File(dir, ACCOUNTS_USING_KEY_FILE_OLD)

            if (file.exists()) file.delete()
            if (fileOld.exists()) fileOld.delete()
        }.onSuccess {
            Logger.i(TAG, "$ACCOUNTS_USING_KEY_FILE and $ACCOUNTS_USING_KEY_FILE_OLD deleted or already absent")
        }.onFailure { e ->
            Logger.e(TAG, "failed to delete $ACCOUNTS_USING_KEY_FILE_OLD", e)
        }
    }

    fun saveAccountsUsingAutomaticBackupsKey(context: Context) {
        return try {
            val accountsData = if (Config.hasMigratedToSecret002) {
                val salt = Base64.decode(Config.automaticBackupsSalt, Base64.NO_WRAP)
                Entry.rustInterface.exportAccountsUsingAutomaticBackupsKey002(salt)
            } else {
                Entry.rustInterface.exportAccountsUsingAutomaticBackupsKey001()
            }

            forceWrite(
                File(context.applicationContext.noBackupFilesDir, ACCOUNTS_USING_AUTOMATIC_BACKUPS_KEY_FILE),
                accountsData
            )
        } catch (e: Exception) {
            Logger.e(TAG, "failed to saveAccountsUsingAutomaticBackupsKey", e)
        }
    }

    fun hasAccountsUsingAutomaticBackupsKey(context: Context): Boolean {
        return try {
            val dir = context.applicationContext.noBackupFilesDir

            File(dir, ACCOUNTS_USING_AUTOMATIC_BACKUPS_KEY_FILE).exists() || File(dir, ACCOUNTS_USING_AUTOMATIC_BACKUPS_KEY_FILE_OLD).exists()
        } catch (e: Exception) {
            Logger.e(TAG, "failed to check hasAccountsUsingAutomaticBackupsKey", e)
            false
        }
    }

    fun getAccountsUsingAutomaticBackupsKey(context: Context): String? {
        return try {
            val dir = context.applicationContext.noBackupFilesDir
            val file = File(dir, ACCOUNTS_USING_AUTOMATIC_BACKUPS_KEY_FILE)
            val fileOld = File(dir, ACCOUNTS_USING_AUTOMATIC_BACKUPS_KEY_FILE_OLD)

            val target = when {
                file.exists() -> file
                fileOld.exists() -> fileOld
                else -> return null
            }

            forceRead(target)
        } catch (e: Exception) {
            Logger.e(TAG, "failed to getAccountsUsingAutomaticBackupsKey", e)
            null
        }
    }
}