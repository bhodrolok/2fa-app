package app.ninesevennine.twofactorauthenticator.gui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.BuildConfig
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonClean
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.AccountManagement
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.utils.Logger
import app.ninesevennine.twofactorauthenticator.utils.Signals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object GreetingRoute

private const val TAG = "Greeting"

@Composable
fun Greeting(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fallbackToPassphrase by remember { mutableStateOf(false) }
    var biometricKeyRecreationRequired by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var unlockError by remember { mutableStateOf(false) }
    var unlockErrorCode by remember { mutableIntStateOf(0) }
    var versionNameTapsCount by remember { mutableIntStateOf(0) }

    val internalAppLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val logContent = Logger.getFullLog()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(logContent.toByteArray())
                }
            } catch (e: Exception) {
                Logger.e("internalAppLogLauncher", "error saving log file ${e.message}")
            }
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    val promptTitle = stringResource(R.string.common_title_biometric_unlock)
    val promptTitle2 = stringResource(R.string.common_title_confirm_biometrics)
    val promptNegative = stringResource(R.string.common_button_cancel)

    LaunchedEffect(Signals.authenticate) {
        if (!Config.isUsingBiometrics) {
            fallbackToPassphrase = true
            return@LaunchedEffect
        }

        if (!KeyStoreAES.loadKey(isBiometricKey = true)) {
            biometricKeyRecreationRequired = true
            fallbackToPassphrase = true
            return@LaunchedEffect
        }

        if (KeyStoreAES.isKeyInvalidated(isBiometricKey = true)) {
            biometricKeyRecreationRequired = true
            fallbackToPassphrase = true
            return@LaunchedEffect
        }

        KeyStoreAES.loadUnlockPassphraseHash(
            promptTitle = promptTitle,
            promptNegative = promptNegative,
            onSuccess = {
                if (AccountManagement.loadAccounts(context) < 0) {
                    fallbackToPassphrase = true
                } else {
                    Logger.i(TAG, "access granted via biometrics")
                    navController.navigate(MainRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            onCancel = {
                fallbackToPassphrase = true
            },
            onError = {
                fallbackToPassphrase = true
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 512.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topInset + 16.dp))

                if (fallbackToPassphrase) {
                    Icon(
                        painter = painterResource(R.drawable.random_alphabet),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = Theme.colors.primary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    ConfidentialTextBox(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = stringResource(R.string.common_title_passphrase),
                        value = passphrase,
                        onValueChange = {
                            if (!isLoading) {
                                passphrase = it
                            }
                        },
                        error = unlockError
                    )

                    AnimatedVisibility(unlockError) {
                        Text(
                            text = when (unlockErrorCode) {
                                AccountManagement.IMPORT_ERROR_NO_ACCOUNTS -> stringResource(R.string.common_import_error_no_accounts) // Won't ever happen.
                                AccountManagement.IMPORT_ERROR_INCORRECT_PASSPHRASE -> stringResource(R.string.common_import_error_incorrect_passphrase)
                                AccountManagement.IMPORT_ERROR_INCORRECT_VERSION -> stringResource(R.string.common_import_error_incorrect_version)
                                AccountManagement.IMPORT_ERROR_INVALID_FILE -> stringResource(R.string.common_import_error_invalid)
                                else -> stringResource(R.string.common_import_error_unknown)
                            },
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp),
                            color = Theme.colors.veryWeak,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = InterVariable
                        )
                    }

                    RoundedButton(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        text = if (isLoading) spinnerText() else stringResource(R.string.screen_greeting_button_unlock)
                    ) {
                        if (isLoading) return@RoundedButton

                        isLoading = true

                        unlockError = false

                        scope.launch(Dispatchers.IO) {
                            val salt = Base64.decode(Config.unlockPassphraseSalt, Base64.DEFAULT)
                            Entry.rustInterface.changeUnlockPassphraseHash(salt, passphrase)

                            val result = AccountManagement.loadAccounts(context)
                            if (result < 0) {
                                isLoading = false
                                unlockError = true
                                unlockErrorCode = result
                            } else {
                                Logger.i(TAG, "access granted via passphrase")

                                if (biometricKeyRecreationRequired) {
                                    KeyStoreAES.deleteKey(isBiometricKey = true)

                                    KeyStoreAES.generateKey(isBiometricKey = true)
                                    if (!KeyStoreAES.loadKey(isBiometricKey = true)) {
                                        Logger.i(TAG, "unable to load recreated biometric key")

                                        Config.isUsingBiometrics = false
                                        Config.save(context)

                                        Logger.i(TAG, "biometrics set to false")

                                        navController.navigate(MainRoute) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }

                                    withContext(Dispatchers.Main) {
                                        KeyStoreAES.saveUnlockPassphraseHash(
                                            promptTitle = promptTitle2,
                                            promptNegative = promptNegative,
                                            onSuccess = {
                                                Logger.i(TAG, "successfully recreated biometric key")

                                                navController.navigate(MainRoute) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            },
                                            onCancel = {
                                                Logger.i(TAG, "biometric recreation confirmation cancelled")

                                                Config.isUsingBiometrics = false
                                                Config.save(context)

                                                Logger.i(TAG, "biometrics set to false")

                                                navController.navigate(MainRoute) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            },
                                            onError = {
                                                Logger.e(TAG, "biometric recreation confirmation failed")

                                                Config.isUsingBiometrics = false
                                                Config.save(context)

                                                Logger.i(TAG, "biometrics set to false")

                                                navController.navigate(MainRoute) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        navController.navigate(MainRoute) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.logo_rectangle),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Theme.colors.primary
                    )
                }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }
        }

        if (fallbackToPassphrase) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 512.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoundedButtonClean(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = bottomInset),
                        text = BuildConfig.VERSION_NAME
                    ) {
                        if (versionNameTapsCount < 5) {
                            versionNameTapsCount++
                            return@RoundedButtonClean
                        }

                        Signals.suppressLockOnExit = true
                        Logger.i(TAG, "downloading app log")
                        internalAppLogLauncher.launch("app.log")
                    }
                }
            }
        }
    }
}