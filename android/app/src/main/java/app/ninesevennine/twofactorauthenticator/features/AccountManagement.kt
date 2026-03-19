package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.utils.Logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object AccountManagement {
    private const val TAG = "AccountManagement"
    private const val ACCOUNTS_USING_SEED_FILE = "accounts_using_seed.2fa"
    private const val ACCOUNTS_USING_AUTOMATIC_BACKUPS = "accounts_using_automatic_internal_backups.2fa"

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

    fun saveAccountsUsingSeed(context: Context) {
        return try {
            val accountsData = Entry.rustInterface.exportAccountsUsingSeed()

            forceWrite(
                File(context.applicationContext.noBackupFilesDir, ACCOUNTS_USING_SEED_FILE),
                accountsData
            )
        } catch (e: Exception) {
            Logger.e(TAG, "failed to saveAccountsUsingSeed", e)
        }
    }

    fun loadAccountsUsingSeed(context: Context): Boolean {
        return try {
            val file = File(context.applicationContext.noBackupFilesDir, ACCOUNTS_USING_SEED_FILE)
            if (!file.exists()) return true

            val accountsData = forceRead(file)

            val result = Entry.rustInterface.importAccountsUsingSeed(accountsData)

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
            Logger.e(TAG, "failed to loadAccountsUsingSeed", e)
            false
        }
    }

    fun deleteAccountsUsingSeed(context: Context): Boolean {
        return runCatching {
            val file = File(context.applicationContext.noBackupFilesDir, ACCOUNTS_USING_SEED_FILE)

            if (file.exists()) file.delete() else true
        }.onSuccess {
            Logger.i(TAG, "$ACCOUNTS_USING_SEED_FILE deleted or already absent")
        }.onFailure { e ->
            Logger.e(TAG, "failed to delete $ACCOUNTS_USING_SEED_FILE", e)
        }.getOrDefault(false)
    }

    fun saveAccountsUsingAutomaticBackups(context: Context) {
        return try {
            val accountsData = Entry.rustInterface.exportAccountsUsingAutomaticBackups()

            forceWrite(
                File(context.applicationContext.noBackupFilesDir, ACCOUNTS_USING_AUTOMATIC_BACKUPS),
                accountsData
            )
        } catch (e: Exception) {
            Logger.e(TAG, "failed to saveAccountsUsingAutomaticBackups", e)
        }
    }

    fun hasAccountsUsingAutomaticBackups(context: Context): Boolean {
        return try {
            val file = File(
                context.applicationContext.noBackupFilesDir,
                ACCOUNTS_USING_AUTOMATIC_BACKUPS
            )

            file.exists()
        } catch (e: Exception) {
            Logger.e(TAG, "failed to check hasAccountsUsingAutomaticBackups", e)
            false
        }
    }

    fun getAccountsUsingAutomaticBackups(context: Context): String? {
        return try {
            val file = File(
                context.applicationContext.noBackupFilesDir,
                ACCOUNTS_USING_AUTOMATIC_BACKUPS
            )

            if (!file.exists()) return null

            forceRead(file)
        } catch (e: Exception) {
            Logger.e(TAG, "failed to getAccountsUsingAutomaticBackups", e)
            null
        }
    }
}