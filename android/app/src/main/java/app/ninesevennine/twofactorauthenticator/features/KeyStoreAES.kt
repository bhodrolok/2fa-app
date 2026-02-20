package app.ninesevennine.twofactorauthenticator.features

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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
            true  to "secure element",
            false to "trusted execution environment"
        )

        for ((isStrongBoxBacked, issuer) in issuers) {
            for (keySize in supportedAesKeySizes) {
                try {
                    generateAesKey(alias, keySize, isBiometricKey, isStrongBoxBacked)
                    Logger.i(TAG, "$issuer generated a $keySize bit aes key ($alias)")

                    return true
                } catch (_: Exception) {
                    Logger.i(TAG, "$issuer cannot generate a $keySize bit aes key ($alias)")
                }
            }
        }

        return false
    }

    fun deleteKey(isBiometricKey: Boolean) {
        val alias = if (isBiometricKey) BIOMETRIC_KEY_ALIAS else REGULAR_KEY_ALIAS

        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)

            if (isBiometricKey) {
                biometricKey = null
            } else {
                regularKey = null
            }

            Logger.i(TAG, "deleted $alias key")
        }
    }

    fun loadKey(isBiometricKey: Boolean): Boolean {
        val alias = if (isBiometricKey) BIOMETRIC_KEY_ALIAS else REGULAR_KEY_ALIAS

        try {
            val entry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry

            if (isBiometricKey) {
                biometricKey = entry.secretKey
            } else {
                regularKey = entry.secretKey
            }

            return true
        } catch (e: Exception) {
            Logger.e(TAG, "$alias key is invalid or unusable", e)
        }

        return false
    }

    fun saveSecret(
        title: String,
        negative: String,
        secret: ByteArray,
        onSuccess: () -> Unit,
        onError: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val currentActivity = activity ?: run { onError(); return }

        if (Config.isUsingBiometrics) {
            val key = biometricKey ?: run { onError(); return }

            val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key)
            }

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    try {
                        val authenticatedCipher = result.cryptoObject?.cipher ?: cipher
                        val encryptedSecret = authenticatedCipher.iv + authenticatedCipher.doFinal(secret)

                        currentActivity.noBackupFilesDir.resolve(SECRET_FILENAME).writeBytes(encryptedSecret)
                        Logger.i(TAG, "secret saved")
                        onSuccess()
                    } catch (e: Exception) {
                        Logger.w(TAG, "failed to save secret", e)
                        onError()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED -> onCancel()
                        else -> onError()
                    }
                }

                override fun onAuthenticationFailed() {}
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(negative)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            BiometricPrompt(currentActivity, ContextCompat.getMainExecutor(currentActivity), callback)
                .authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } else {
            val key = regularKey ?: run { onError(); return }

            try {
                val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
                    init(Cipher.ENCRYPT_MODE, key)
                }
                val encryptedSecret = cipher.iv + cipher.doFinal(secret)

                currentActivity.noBackupFilesDir.resolve(SECRET_FILENAME).writeBytes(encryptedSecret)
                Logger.i(TAG, "secret saved")
                onSuccess()
            } catch (e: Exception) {
                Logger.w(TAG, "failed to save secret", e)
                onError()
            }
        }
    }

    fun loadSecret(
        title: String,
        negative: String,
        onSuccess: (ByteArray) -> Unit,
        onError: () -> Unit,
        onCancel: () -> Unit = {}
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

        if (Config.isUsingBiometrics) {
            val key = biometricKey ?: run { onError(); return }

            val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            }

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    try {
                        val authenticatedCipher = result.cryptoObject?.cipher ?: cipher
                        onSuccess(authenticatedCipher.doFinal(ciphertext))
                    } catch (e: Exception) {
                        Logger.w(TAG, "failed to load secret", e)
                        onError()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED -> onCancel()
                        else -> onError()
                    }
                }

                override fun onAuthenticationFailed() {}
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(negative)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            BiometricPrompt(currentActivity, ContextCompat.getMainExecutor(currentActivity), callback)
                .authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } else {
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
}