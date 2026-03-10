package app.ninesevennine.twofactorauthenticator.gui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.KeyStoreRSA
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.start2faAndroidMigration
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonClean
import app.ninesevennine.twofactorauthenticator.utils.Logger
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

    var unableToLoadDecryptionKeys by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var promptCanceled by remember { mutableStateOf(false) }
    var startOverButtonTapsCount by remember { mutableIntStateOf(0) }

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

    val biometricUnlockText = stringResource(R.string.common_biometric_unlock)
    val cancelText = stringResource(R.string.common_cancel)

    LaunchedEffect(Unit) {
        if (Config.hasCompletedSetup) {
            if (Config.internalIsUsingKeyStoreAES) {
                if (Config.isUsingBiometrics) {
                    if (!KeyStoreAES.loadKey(true)) {
                        unableToLoadDecryptionKeys = true
                        return@LaunchedEffect
                    }

                    if (KeyStoreAES.isKeyInvalidated(true)) {
                        unableToLoadDecryptionKeys = true
                        return@LaunchedEffect
                    }
                } else {
                    if (!KeyStoreAES.loadKey(false)) {
                        unableToLoadDecryptionKeys = true
                        return@LaunchedEffect
                    }

                    if (KeyStoreAES.isKeyInvalidated(false)) {
                        unableToLoadDecryptionKeys = true
                        return@LaunchedEffect
                    }
                }

                KeyStoreAES.loadSecret(
                    title = biometricUnlockText,
                    negative = cancelText,
                    onSuccess = { secret ->
                        scope.launch(Dispatchers.IO) {
                            Entry.rustInterface.setSecret(secret)

                            if (!AccountManagement.loadAccountsUsingSeed(context)) {
                                hasError = true
                                return@launch
                            }

                            withContext(Dispatchers.Main) {
                                navController.navigate(MainRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    },
                    onError = {
                        unableToLoadDecryptionKeys = true
                    },
                    onCancel = {
                        Logger.i(TAG, "biometric prompt canceled")
                        promptCanceled = true
                    }
                )
            } else {
                if (Config.isUsingBiometrics) {
                    if (!KeyStoreRSA.loadBiometricKey()) {
                        unableToLoadDecryptionKeys = true
                        return@LaunchedEffect
                    }

                    if (KeyStoreRSA.isKeyInvalidated(true)) {
                        unableToLoadDecryptionKeys = true
                        return@LaunchedEffect
                    }
                } else {
                    if (!KeyStoreRSA.loadRegularKey()) {
                        unableToLoadDecryptionKeys = true
                        return@LaunchedEffect
                    }

                    if (KeyStoreRSA.isKeyInvalidated(false)) {
                        unableToLoadDecryptionKeys = true
                        return@LaunchedEffect
                    }
                }

                KeyStoreRSA.loadSecret(
                    title = biometricUnlockText,
                    negative = cancelText,
                    onSuccess = { secret ->
                        scope.launch(Dispatchers.IO) {
                            Entry.rustInterface.setSecret(secret)

                            if (!AccountManagement.loadAccountsUsingSeed(context)) {
                                hasError = true
                                return@launch
                            }

                            withContext(Dispatchers.Main) {
                                navController.navigate(MainRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    },
                    onError = {
                        hasError = true
                    },
                    onCancel = {
                        Logger.i(TAG, "biometric prompt canceled")
                        promptCanceled = true
                    }
                )
            }
        } else {
            KeyStoreAES.saveSecret(
                title = biometricUnlockText,
                negative = cancelText,
                secret = Entry.rustInterface.getSecret(),
                onSuccess = {
                    scope.launch(Dispatchers.IO) {
                        Config.hasCompletedSetup = true
                        Config.save(context)

                        start2faAndroidMigration(context)

                        withContext(Dispatchers.Main) {
                            navController.navigate(MainRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                },
                onError = {
                    hasError = true
                },
                onCancel = {
                    Logger.i(TAG, "biometric prompt canceled")
                    promptCanceled = true
                }
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (unableToLoadDecryptionKeys) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = Theme.colors.error
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.greeting_something_terribly_wrong),
                    modifier = Modifier.fillMaxWidth(),
                    color = Theme.colors.error,
                    fontSize = 20.sp,
                    fontFamily = InterVariable
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.greeting_stw_message),
                    modifier = Modifier.fillMaxWidth(),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable
                )
            } else if (hasError || promptCanceled){
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = Theme.colors.warning
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.greeting_something_wrong),
                    modifier = Modifier.fillMaxWidth(),
                    color = Theme.colors.warning,
                    fontSize = 20.sp,
                    fontFamily = InterVariable
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.greeting_sw_message),
                    modifier = Modifier.fillMaxWidth(),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.logo_rectangle),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = Theme.colors.primary
                )
            }

            if (unableToLoadDecryptionKeys || hasError || promptCanceled) {
                Spacer(modifier = Modifier.height(12.dp))

                if (AccountManagement.hasAccountsUsingAutomaticInternalBackups(context)) {
                    Text(
                        text = stringResource(R.string.greeting_internal_backup_found),
                        modifier = Modifier.fillMaxWidth(),
                        color = Theme.colors.info,
                        fontSize = 14.sp,
                        fontFamily = InterVariable
                    )
                } else {
                    Text(
                        text = stringResource(R.string.greeting_internal_backup_not_found),
                        modifier = Modifier.fillMaxWidth(),
                        color = Theme.colors.error,
                        fontSize = 14.sp,
                        fontFamily = InterVariable
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                RoundedButton(
                    if (startOverButtonTapsCount > 0)
                        pluralStringResource(R.plurals.greeting_wipe_and_start_over_variable, 6 - startOverButtonTapsCount, 6 - startOverButtonTapsCount)
                    else
                        stringResource(R.string.greeting_wipe_and_start_over)
                ) {
                    if (startOverButtonTapsCount < 5) {
                        startOverButtonTapsCount++
                        return@RoundedButton
                    }

                    navController.navigate(SetupGetStartedRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoundedButtonClean(stringResource(R.string.common_download_app_log)) {
                    Logger.i(TAG, "downloading app log")
                    internalAppLogLauncher.launch("app.log")
                }
            }
        }
    }
}