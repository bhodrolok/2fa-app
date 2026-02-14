package app.ninesevennine.twofactorauthenticator.features

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.ninesevennine.twofactorauthenticator.BuildConfig
import app.ninesevennine.twofactorauthenticator.utils.Logger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

object SecureElement {
    private const val TAG = "SecureElement"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPPadding"

    private val RSA_PARAMS = OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA1,
        PSource.PSpecified.DEFAULT
    )

    private const val REGULAR_KEY_ALIAS = "regular"
    private const val BIOMETRIC_KEY_ALIAS = "biometric"
    private const val SECRET_FILENAME = "secret.bin"

    private val supportedRsaKeySizes = listOf(4096, 3072, 2048)

    private var activity: FragmentActivity? = null

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private var regularPublicKey: PublicKey? = null
    private var regularPrivateKey: PrivateKey? = null
    private var biometricPublicKey: PublicKey? = null
    private var biometricPrivateKey: PrivateKey? = null

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
        return biometricPublicKey != null && biometricPrivateKey != null
    }

    // system can still invalidate the key if an update corrupts keystore.
    private fun generateRsaKey(
        alias: String,
        keySize: Int,
        isBiometricKey: Boolean
    ) {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(keySize)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)

            // requires device to have a lock screen pin/pattern/password when key is created.
            // invalidates key permanently if user later disables the lock screen completely.
            .setUnlockedDeviceRequired(isBiometricKey)
            .setUserAuthenticationRequired(isBiometricKey)

            // invalidates key if biometrics change.
            .setInvalidatedByBiometricEnrollment(isBiometricKey)
            .setIsStrongBoxBacked(!BuildConfig.DEBUG)
//            .setIsStrongBoxBacked(true)
            .apply {
                if (isBiometricKey) {
                    setUserAuthenticationParameters(
                        0,
                        KeyProperties.AUTH_BIOMETRIC_STRONG
                    )
                }
            }
            .build()

        keyPairGenerator.initialize(keySpec)
        keyPairGenerator.generateKeyPair()
    }

    fun generateRegularKey() {
        for (keySize in supportedRsaKeySizes) {
            runCatching {
                generateRsaKey(
                    alias = REGULAR_KEY_ALIAS,
                    keySize = keySize,
                    isBiometricKey = false
                )
            }.onSuccess {
                Logger.i(TAG, "generated $REGULAR_KEY_ALIAS key with size $keySize")
                return
            }
        }

        Logger.e(TAG, "no supported RSA key size for $REGULAR_KEY_ALIAS key")
        error("no supported RSA key size for $REGULAR_KEY_ALIAS key")
    }

    fun generateBiometricKey(): Boolean {
        for (keySize in supportedRsaKeySizes) {
            runCatching {
                generateRsaKey(
                    alias = BIOMETRIC_KEY_ALIAS,
                    keySize = keySize,
                    isBiometricKey = true
                )
            }.onSuccess {
                Logger.i(TAG, "generated $BIOMETRIC_KEY_ALIAS key with size $keySize")
                return true
            }
        }

        // user might not have lock screen set up.
        return false
    }

    fun deleteRegularKey() {
        if (keyStore.containsAlias(REGULAR_KEY_ALIAS)) {
            runCatching {
                keyStore.deleteEntry(REGULAR_KEY_ALIAS)
            }.onSuccess {
                Logger.i(TAG, "deleted $REGULAR_KEY_ALIAS key")
            }.onFailure { e ->
                Logger.w(TAG, "failed to delete $REGULAR_KEY_ALIAS", e)
            }
        }
    }

    fun deleteBiometricKey() {
        if (isBiometricAvailable() && keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            runCatching {
                keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
            }.onSuccess {
                Logger.i(TAG, "deleted $BIOMETRIC_KEY_ALIAS key")
            }.onFailure { e ->
                Logger.w(TAG, "failed to delete $BIOMETRIC_KEY_ALIAS", e)
            }
        }
    }

    fun loadRegularKey(): Boolean {
        return runCatching {
            val entry = keyStore.getEntry(REGULAR_KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
            regularPublicKey = entry.certificate.publicKey
            regularPrivateKey = entry.privateKey
            true
        }.onFailure { e ->
            Logger.w(TAG, "$REGULAR_KEY_ALIAS key is invalid or unusable", e)
        }.getOrNull() ?: false
    }

    fun loadBiometricKey(): Boolean {
        return runCatching {
            val entry = keyStore.getEntry(BIOMETRIC_KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
            biometricPublicKey = entry.certificate.publicKey
            biometricPrivateKey = entry.privateKey
            true
        }.onFailure { e ->
            Logger.w(TAG, "$BIOMETRIC_KEY_ALIAS key is invalid or unusable", e)
        }.getOrNull() ?: false
    }

    fun encryptRegular(data: ByteArray): ByteArray? {
        val key = regularPublicKey ?: return null

        return runCatching {
            Cipher.getInstance(RSA_TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key, RSA_PARAMS)
            }.doFinal(data)
        }.onFailure { e ->
            Logger.w(TAG, "failed to encrypt using $REGULAR_KEY_ALIAS key", e)
        }.getOrNull()
    }

    fun decryptRegular(data: ByteArray): ByteArray? {
        val key = regularPrivateKey ?: return null

        return runCatching {
            Cipher.getInstance(RSA_TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, RSA_PARAMS)
            }.doFinal(data)
        }.onFailure { e ->
            Logger.w(TAG, "failed to decrypt using $REGULAR_KEY_ALIAS key", e)
        }.getOrNull()
    }

    fun encryptBiometric(data: ByteArray): ByteArray? {
        val key = biometricPublicKey ?: return null

        return runCatching {
            Cipher.getInstance(RSA_TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key, RSA_PARAMS)
            }.doFinal(data)
        }.onFailure { e ->
            Logger.w(TAG, "failed to encrypt using $BIOMETRIC_KEY_ALIAS key", e)
        }.getOrNull()
    }

    fun decryptBiometric(
        data: ByteArray,
        onSuccess: (ByteArray) -> Unit,
        onError: () -> Unit,
        onCancel: () -> Unit
    ) {
        val currentActivity = activity ?: return
        val key = biometricPrivateKey ?: return

        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, RSA_PARAMS)

        val biometricPrompt = BiometricPrompt(
            currentActivity,
            ContextCompat.getMainExecutor(currentActivity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    runCatching {
                        val authenticatedCipher = result.cryptoObject?.cipher ?: cipher
                        authenticatedCipher.doFinal(data)
                    }.onSuccess(onSuccess)
                        .onFailure { e ->
                            Logger.w(TAG, "failed to decrypt using $BIOMETRIC_KEY_ALIAS key", e)
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

                override fun onAuthenticationFailed() {
                    onError()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Unlock")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    fun saveSecret(secret: ByteArray): Boolean {
        val currentActivity = activity ?: return false

        val encryptedSecret = if (Config.isUsingBiometrics) {
            encryptBiometric(secret)
        } else {
            encryptRegular(secret)
        } ?: return false

        return runCatching {
            currentActivity.noBackupFilesDir.resolve(SECRET_FILENAME).writeBytes(encryptedSecret)
            Logger.i(TAG, "secret saved")
            true
        }.onFailure { e ->
            Logger.w(TAG, "failed to save secret", e)
        }.getOrDefault(false)
    }

    fun loadSecret(
        onSuccess: (ByteArray) -> Unit,
        onError: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val currentActivity = activity ?: run {
            onError()
            return
        }

        val secretFile = currentActivity.noBackupFilesDir.resolve(SECRET_FILENAME)
        if (!secretFile.exists()) {
            onError()
            return
        }

        val encryptedSecret = runCatching {
            secretFile.readBytes()
        }.getOrNull() ?: run {
            onError()
            return
        }

        if (Config.isUsingBiometrics) {
            decryptBiometric(encryptedSecret, onSuccess, onError, onCancel)
        } else {
            val decryptedSecret = decryptRegular(encryptedSecret)
            if (decryptedSecret != null) {
                onSuccess(decryptedSecret)
            } else {
                onError()
            }
        }
    }
}