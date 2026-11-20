package app.ninesevennine.twofactorauthenticator.features

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.ninesevennine.twofactorauthenticator.rustInterface
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
    private const val BIOMETRIC_KEY_FILE = "biometric_key.bin"
    private const val OLD_BIOMETRIC_KEY_FILE = "unlock_passphrase_hash.bin"

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

                return true
            } catch (e: Exception) {
                Log.e(TAG, "generateBiometricKey -> generateBiometricAESKey($keySize, isStrongBoxBacked = true)", e)
            }
        }

        for (keySize in supportedAesKeySizes) {
            try {
                generateBiometricAESKey(keySize, isStrongBoxBacked = false)

                return true
            } catch (e: Exception) {
                Log.e(TAG, "generateBiometricKey -> generateBiometricAESKey($keySize, isStrongBoxBacked = false)", e)
            }
        }

        return false
    }

    fun hasBiometricKey(): Boolean {
        if (!keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            return false
        }

        val currentActivity = activity ?: return false
        return listOf(BIOMETRIC_KEY_FILE, OLD_BIOMETRIC_KEY_FILE).any {
            currentActivity.noBackupFilesDir.resolve(it).exists()
        }
    }

    fun deleteBiometricKey() {
        if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
        }

        biometricKey = null

        val currentActivity = activity ?: return
        for (name in listOf(BIOMETRIC_KEY_FILE, OLD_BIOMETRIC_KEY_FILE)) {
            val file = currentActivity.noBackupFilesDir.resolve(name)

            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun isBiometricKeyUsable(): Boolean {
        return biometricKey != null && try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, biometricKey)

            true
        } catch (e: Exception) {
            Log.e(TAG, "isBiometricKeyUsable", e)
            false
        }
    }

    fun loadBiometricKey(): Boolean {
        if (!keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            return false
        }

        val entry = keyStore.getEntry(BIOMETRIC_KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        biometricKey = entry.secretKey

        return true
    }

    fun saveBiometricKey(
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
                val key = rustInterface.client_key_get()

                try {
                    val authenticatedCipher = result.cryptoObject?.cipher ?: cipher
                    val data = authenticatedCipher.iv + authenticatedCipher.doFinal(key)

                    val file = currentActivity.noBackupFilesDir.resolve(BIOMETRIC_KEY_FILE)
                    file.writeBytes(data)

                    onSuccess()
                } catch (e: Exception) {
                    Log.e(TAG, "saveBiometricKey -> onAuthenticationSucceeded", e)
                    onError()
                } finally {
                    key.fill(0)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.e(TAG, "saveBiometricKey -> onAuthenticationError\n$errorCode\n$errString")

                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> onCancel()
                    else -> onError()
                }
            }

            override fun onAuthenticationFailed() {
                // Ignore
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

    fun loadBiometricKey(
        promptTitle: String,
        promptNegative: String,
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
        onError: () -> Unit,
    ) {
        val currentActivity = activity ?: run { onError(); return }
        val biometricKey = biometricKey ?: run { onError(); return }

        val newFile = currentActivity.noBackupFilesDir.resolve(BIOMETRIC_KEY_FILE)
        val oldFile = currentActivity.noBackupFilesDir.resolve(OLD_BIOMETRIC_KEY_FILE)

        val file = when {
            newFile.exists() -> newFile
            oldFile.exists() -> oldFile
            else -> { onError(); return }
        }

        val data = try { file.readBytes() } catch (_: Exception) { onError(); return }
        if (data.size < GCM_IV_LENGTH) { onError(); return }

        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = data.copyOfRange(GCM_IV_LENGTH, data.size)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, biometricKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                var key: ByteArray? = null

                try {
                    val authenticatedCipher = result.cryptoObject?.cipher ?: cipher
                    key = authenticatedCipher.doFinal(ciphertext)

                    rustInterface.client_key_set(key)

                    onSuccess()
                } catch (e: Exception) {
                    Log.e(TAG, "loadBiometricKey -> onAuthenticationSucceeded", e)
                    onError()
                } finally {
                    key?.fill(0)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.e(TAG, "loadBiometricKey -> onAuthenticationError\n$errorCode\n$errString")

                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> onCancel()
                    else -> onError()
                }
            }

            override fun onAuthenticationFailed() {
                // Ignore
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
}