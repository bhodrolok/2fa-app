package app.ninesevennine.twofactorauthenticator.features

import android.content.Context
import app.ninesevennine.twofactorauthenticator.BuildConfig
import app.ninesevennine.twofactorauthenticator.RustInterface
import app.ninesevennine.twofactorauthenticator.utils.Logger
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64

private const val TAG = "start2faAndroidMigration"

private const val KEY_ALIAS = "${BuildConfig.APPLICATION_ID}.secure.crypto.key"
private const val FILE_NAME = "vault.json"

private const val IV_LENGTH = 12
private const val GCM_TAG_LENGTH = 128
private const val TRANSFORMATION_ALGORITHM = "AES/GCM/NoPadding"

// will be removed upon full google play release
fun start2faAndroidMigration(context: Context) {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    if (!keyStore.containsAlias(KEY_ALIAS)) return

    val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    if (secretKey == null) {
        Logger.e(TAG, "$KEY_ALIAS key is invalid or unusable")

        keyStore.deleteEntry(KEY_ALIAS)
        Logger.i(TAG, "deleted $KEY_ALIAS key")

        return
    }

    val file = File(context.applicationContext.noBackupFilesDir, FILE_NAME)
    if (!file.exists()) {
        Logger.i(TAG, "$FILE_NAME does not exist")

        keyStore.deleteEntry(KEY_ALIAS)
        Logger.i(TAG, "deleted $KEY_ALIAS key")
        return
    }

    try {
        val json = file.readText(Charsets.UTF_8)

        val obj = JSONObject(json)
        val rawData = obj.getString("data").let { Base64.decode(it) }

        if (rawData.size < IV_LENGTH + (GCM_TAG_LENGTH / 8)) {
            Logger.e(TAG, "invalid vault")
            return
        }

        val iv = rawData.copyOfRange(0, IV_LENGTH)
        val ciphertext = rawData.copyOfRange(IV_LENGTH, rawData.size)

        val cipher = Cipher.getInstance(TRANSFORMATION_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val data = cipher.doFinal(ciphertext)
        val array = String(data, Charsets.UTF_8)

        if (RustInterface().importAccountsDB001(array)) {
            Logger.i(TAG, "migration success")

            keyStore.deleteEntry(KEY_ALIAS)
            Logger.i(TAG, "deleted $KEY_ALIAS key")

            file.delete()
            Logger.i(TAG, "deleted $FILE_NAME")

            AccountManagement.saveAccountsUsingSeed(context)

            if (Config.isUsingAutomaticInternalBackups) {
                AccountManagement.saveAccountsUsingAutomaticInternalBackups(context)
            }
        }
    } catch (e: Exception) {
        Logger.e(TAG, "unable to migrate vault", e)
        return
    }
}