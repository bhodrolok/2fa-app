package app.ninesevennine.twofactorauthenticator.features

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.utils.AtomicFileSystem
import app.ninesevennine.twofactorauthenticator.utils.Logger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeyStoreAES {
    private const val TAG = "KeyStoreAES"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"

    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private const val REGULAR_KEY_ALIAS = "regular"
    private const val BIOMETRIC_KEY_ALIAS = "biometric"
    private const val SECRET_FILENAME = "secret.bin"
    private const val UNLOCK_PASSPHRASE_HASH_FILE = "unlock_passphrase_hash.bin"

    private val supportedAesKeySizes = listOf(256, 128)

    private var activity: FragmentActivity? = null

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private var regularKey: SecretKey? = null
    private var biometricKey: SecretKey? = null

    fun setActivity(activity: FragmentActivity) {
        this.activity = activity
    }

    fun isBiometricAvailable(): Boolean {
        val currentActivity = activity ?: return false

        return BiometricManager.from(currentActivity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricKeyLoaded(): Boolean {
        return biometricKey != null
    }

    // system can still invalidate the key if an update corrupts keystore.
    private fun generateAesKey(
        alias: String,
        keySize: Int,
        isBiometricKey: Boolean,
        isStrongBoxBacked: Boolean
    ) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(keySize)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUnlockedDeviceRequired(isBiometricKey)
            .setUserAuthenticationRequired(isBiometricKey)
            .setInvalidatedByBiometricEnrollment(isBiometricKey)
            .setIsStrongBoxBacked(isStrongBoxBacked)
            .apply {
                if (isBiometricKey) {
                    setUserAuthenticationParameters(
                        0,
                        KeyProperties.AUTH_BIOMETRIC_STRONG
                    )
                }
            }
            .build()

        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
    }

    fun generateKey(isBiometricKey: Boolean): Boolean {
        val alias = if (isBiometricKey) BIOMETRIC_KEY_ALIAS else REGULAR_KEY_ALIAS

        val issuers = listOf(
            true to "secure element",
            false to "trusted execution environment"
        )

        val success = issuers.firstNotNullOfOrNull { (isStrongBoxBacked, issuer) ->
            supportedAesKeySizes.firstNotNullOfOrNull { keySize ->
                runCatching {
                    generateAesKey(alias, keySize, isBiometricKey, isStrongBoxBacked)
                }.onSuccess {
                    Logger.i(TAG, "$issuer generated a $keySize bit $alias aes key")
                }.onFailure {
                    Logger.i(TAG, "$issuer cannot generate a $keySize bit $alias aes key")
                }.getOrNull()
            }
        }

        return success != null
    }

    fun deleteKey(isBiometricKey: Boolean) {
        val alias = if (isBiometricKey) BIOMETRIC_KEY_ALIAS else REGULAR_KEY_ALIAS

        runCatching {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)

                if (isBiometricKey) biometricKey = null else regularKey = null

                Logger.i(TAG, "successfully deleted $alias key")
            } else {
                Logger.i(TAG, "$alias key not found; nothing to delete")
            }
        }.onFailure { e ->
            Logger.e(TAG, "failed to delete $alias key", e)
        }
    }

    fun isKeyUsable(isBiometricKey: Boolean): Boolean {
        val alias = if (isBiometricKey) BIOMETRIC_KEY_ALIAS else REGULAR_KEY_ALIAS

        return try {
            val key = if (isBiometricKey) biometricKey else regularKey
            if (key == null) return true

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            true
        } catch (e: Exception) {
            Logger.e(TAG, "$alias key is not usable", e)
            false
        }
    }

    fun loadKey(isBiometricKey: Boolean): Boolean {
        val alias = if (isBiometricKey) BIOMETRIC_KEY_ALIAS else REGULAR_KEY_ALIAS

        return runCatching {
            val entry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry

            if (isBiometricKey) biometricKey = entry.secretKey else regularKey = entry.secretKey

            true
        }.onFailure { e ->
            Logger.e(TAG, "failed to load $alias key", e)
        }.getOrDefault(false)
    }

    fun saveUnlockPassphraseHash(
        promptTitle: String,
        promptNegative: String,
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
        onError: () -> Unit,
    ) {
        val currentActivity = activity ?: run { onError(); return }
        val key = biometricKey ?: run { onError(); return }

        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    val authenticatedCipher = result.cryptoObject?.cipher ?: cipher
                    val data = authenticatedCipher.iv + authenticatedCipher.doFinal(Entry.rustInterface.getUnlockPassphraseHash())

                    val file = currentActivity.noBackupFilesDir.resolve(UNLOCK_PASSPHRASE_HASH_FILE)
                    AtomicFileSystem.write(file, data)

                    Logger.i(TAG, "unlock passphrase hash saved")
                    onSuccess()
                } catch (e: Exception) {
                    Logger.w(TAG, "failed to save unlock passphrase hash", e)
                    onError()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Logger.w(TAG, "failed to save unlock passphrase hash with errorCode $errorCode and reason $errString")

                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> onCancel()
                    else -> onError()
                }
            }

            override fun onAuthenticationFailed() {
                Logger.w(TAG, "failed to save unlock passphrase hash")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setNegativeButtonText(promptNegative)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        BiometricPrompt(currentActivity, ContextCompat.getMainExecutor(currentActivity), callback)
            .authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    fun loadUnlockPassphraseHash(
        promptTitle: String,
        promptNegative: String,
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
        onError: () -> Unit,
    ) {
        val currentActivity = activity ?: run { onError(); return }
        val key = biometricKey ?: run { onError(); return }

        val file = currentActivity.noBackupFilesDir.resolve(UNLOCK_PASSPHRASE_HASH_FILE)
        if (!file.exists()) { onError(); return }

        val data = try { AtomicFileSystem.read(file) } catch (_: Exception) { onError(); return }
        if (data.size < GCM_IV_LENGTH) { onError(); return }

        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = data.copyOfRange(GCM_IV_LENGTH, data.size)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    val authenticatedCipher = result.cryptoObject?.cipher ?: cipher
                    Entry.rustInterface.setUnlockPassphraseHash(authenticatedCipher.doFinal(ciphertext))

                    Logger.i(TAG, "unlock passphrase hash loaded")
                    onSuccess()
                } catch (e: Exception) {
                    Logger.w(TAG, "failed to load unlock passphrase hash", e)
                    onError()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Logger.w(TAG, "failed to load unlock passphrase hash with errorCode $errorCode and reason $errString")

                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> onCancel()
                    else -> onError()
                }
            }

            override fun onAuthenticationFailed() {
                Logger.w(TAG, "failed to load unlock passphrase hash")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setNegativeButtonText(promptNegative)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        BiometricPrompt(currentActivity, ContextCompat.getMainExecutor(currentActivity), callback)
            .authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    fun deleteUnlockPassphraseHashFile() {
        val currentActivity = activity ?: return
        val file = currentActivity.noBackupFilesDir.resolve(UNLOCK_PASSPHRASE_HASH_FILE)

        try {
            if (file.exists()) {
                if (file.delete()) {
                    Logger.i(TAG, "successfully deleted unlock passphrase hash file")
                } else {
                    Logger.w(TAG, "failed to delete unlock passphrase hash file")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "failed to delete unlock passphrase hash file", e)
        }
    }

    fun loadSecret(
        onSuccess: (ByteArray) -> Unit,
        onError: () -> Unit
    ) {
        val currentActivity = activity ?: run { onError(); return }

        val secretFile = currentActivity.noBackupFilesDir.resolve(SECRET_FILENAME)
        if (!secretFile.exists()) { onError(); return }

        val encryptedSecret = try {
            secretFile.readBytes()
        } catch (e: Exception) {
            Logger.w(TAG, "failed to read secret", e)
            onError()
            return
        }

        val iv = encryptedSecret.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encryptedSecret.copyOfRange(GCM_IV_LENGTH, encryptedSecret.size)

        val key = regularKey ?: run { onError(); return }

        try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            }
            onSuccess(cipher.doFinal(ciphertext))
        } catch (e: Exception) {
            Logger.w(TAG, "failed to load secret", e)
            onError()
        }
    }
}