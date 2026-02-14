package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import app.ninesevennine.twofactorauthenticator.RustInterface
import app.ninesevennine.twofactorauthenticator.utils.Logger
import java.io.File

object AccountManagement {
    private const val TAG = "AccountManagement"
    private const val ACCOUNTS_USING_SEED_FILE = "accounts_using_seed.2fa"
    private const val ACCOUNTS_USING_AUTOMATIC_INTERNAL_BACKUPS = "accounts_using_automatic_internal_backups.2fa"

    fun saveAccountsUsingSeed(context: Context) {
        return try {
            val rust = RustInterface()
            val accountsData = rust.exportAccountsUsingSeed()

            val file = File(context.applicationContext.noBackupFilesDir, ACCOUNTS_USING_SEED_FILE)
            file.writeText(accountsData)
        } catch (e: Exception) {
            Logger.e(TAG, "failed to saveAccountsUsingSeed", e)
        }
    }

    fun loadAccountsUsingSeed(context: Context): Boolean {
        return try {
            val file = File(context.applicationContext.noBackupFilesDir, ACCOUNTS_USING_SEED_FILE)
            if (!file.exists()) return true

            val accountsData = file.readText()

            RustInterface().importAccountsUsingSeed(accountsData)
        } catch (e: Exception) {
            Logger.e(TAG, "failed to loadAccountsUsingSeed", e)
            false
        }
    }

    fun deleteAccountsUsingSeed(context: Context): Boolean {
        return try {
            val file = File(
                context.applicationContext.noBackupFilesDir,
                ACCOUNTS_USING_SEED_FILE
            )

            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Logger.e(TAG, "failed to deleteAccountsUsingSeed", e)
            false
        }
    }

    fun saveAccountsUsingAutomaticInternalBackups(context: Context) {
        return try {
            val rust = RustInterface()
            val accountsData = rust.exportAccountsUsingAutomaticInternalBackups()

            val file = File(context.applicationContext.noBackupFilesDir, ACCOUNTS_USING_AUTOMATIC_INTERNAL_BACKUPS)
            file.writeText(accountsData)
        } catch (e: Exception) {
            Logger.e(TAG, "failed to saveAccountsUsingAutomaticInternalBackups", e)
        }
    }

    fun hasAccountsUsingAutomaticInternalBackups(context: Context): Boolean {
        return try {
            val file = File(
                context.applicationContext.noBackupFilesDir,
                ACCOUNTS_USING_AUTOMATIC_INTERNAL_BACKUPS
            )
            file.exists()
        } catch (e: Exception) {
            Logger.e(TAG, "failed to check hasAccountsUsingAutomaticInternalBackups", e)
            false
        }
    }

    fun getAccountsUsingAutomaticInternalBackups(context: Context): String? {
        return try {
            val file = File(
                context.applicationContext.noBackupFilesDir,
                ACCOUNTS_USING_AUTOMATIC_INTERNAL_BACKUPS
            )

            if (!file.exists()) return null

            file.readText()
        } catch (e: Exception) {
            Logger.e(TAG, "failed to getAccountsUsingAutomaticInternalBackups", e)
            null
        }
    }
}