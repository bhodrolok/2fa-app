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

    private const val BIOMETRIC_KEY_ALIAS = "biometric"
    private const val UNLOCK_PASSPHRASE_HASH_FILE = "unlock_passphrase_hash.bin"

    private val supportedAesKeySizes = listOf(256, 128)

    private var activity: FragmentActivity? = null

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

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

    private fun generateBiometricAESKey(keySize: Int, isStrongBoxBacked: Boolean) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(keySize)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUnlockedDeviceRequired(true)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .setIsStrongBoxBacked(isStrongBoxBacked)
            .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            .build()

        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
    }

    fun generateBiometricKey(): Boolean {
        for (keySize in supportedAesKeySizes) {
            try {
                generateBiometricAESKey(keySize, isStrongBoxBacked = true)
                Logger.i(TAG, "successfully generated $BIOMETRIC_KEY_ALIAS key of size $keySize in strongbox")

                return true
            } catch (e: Exception) {
                Logger.i(TAG, "failed to generate $BIOMETRIC_KEY_ALIAS key of size $keySize in strongbox", e)
            }
        }

        for (keySize in supportedAesKeySizes) {
            try {
                generateBiometricAESKey(keySize, isStrongBoxBacked = false)
                Logger.i(TAG, "successfully generated $BIOMETRIC_KEY_ALIAS key of size $keySize")

                return true
            } catch (e: Exception) {
                Logger.i(TAG, "failed to generate $BIOMETRIC_KEY_ALIAS key of size $keySize", e)
            }
        }

        Logger.e(TAG, "failed to generate $BIOMETRIC_KEY_ALIAS key")

        return false
    }

    fun deleteBiometricKey() {
        try {
            if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)

                Logger.i(TAG, "successfully deleted $BIOMETRIC_KEY_ALIAS key")
            } else {
                Logger.i(TAG, "keystore does not contain $BIOMETRIC_KEY_ALIAS key")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "failed to delete $BIOMETRIC_KEY_ALIAS key", e)
        }
    }

    fun isBiometricKeyUsable(): Boolean {
        return try {
            if (biometricKey == null) {
                Logger.i(TAG, "$BIOMETRIC_KEY_ALIAS key was not loaded")
                return false
            }

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, biometricKey)

            true
        } catch (e: Exception) {
            Logger.e(TAG, "$BIOMETRIC_KEY_ALIAS key is not usable", e)
            false
        }
    }

    fun loadBiometricKey(): Boolean {
        try {
            if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                val entry = keyStore.getEntry(BIOMETRIC_KEY_ALIAS, null) as KeyStore.SecretKeyEntry
                biometricKey = entry.secretKey

                Logger.i(TAG, "successfully loaded $BIOMETRIC_KEY_ALIAS key")

                return true
            }
        } catch (e: Exception) {
            Logger.e(TAG, "failed to load $BIOMETRIC_KEY_ALIAS key", e)
        }

        return false
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
}