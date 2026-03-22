package app.ninesevennine.twofactorauthenticator.gui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.BuildConfig
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Signals
import app.ninesevennine.twofactorauthenticator.features.Theme
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
    var wipeButtonTapsCount by remember { mutableIntStateOf(0) }
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
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)

    val promptTitle = stringResource(R.string.common_title_biometric_unlock)
    val promptNegative = stringResource(R.string.common_button_cancel)

    val internalBackupExists = AccountManagement.hasAccountsUsingAutomaticBackups(context)

    LaunchedEffect(Unit) {
        if (Config.hasCompletedSetup) {
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
                title = promptTitle,
                negative = promptNegative,
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
            KeyStoreAES.saveSecret(
                title = promptTitle,
                negative = promptNegative,
                secret = Entry.rustInterface.getSecret(),
                onSuccess = {
                    scope.launch(Dispatchers.IO) {
                        Config.hasCompletedSetup = true
                        Config.save(context)

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
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 512.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topInset + 16.dp))

                if (unableToLoadDecryptionKeys) {
                    Icon(
                        painter = painterResource(R.drawable.mi_warning),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = Theme.colors.veryWeak
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = stringResource(R.string.screen_greeting_title_invalidated),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Theme.colors.primary,
                        fontSize = 18.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.screen_greeting_description_invalidated),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Theme.colors.primary,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Justify
                    )
                } else if (hasError || promptCanceled){
                    Icon(
                        painter = painterResource(R.drawable.mi_warning),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = Theme.colors.weak
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = stringResource(R.string.screen_greeting_title_something_wrong),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Theme.colors.primary,
                        fontSize = 18.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.screen_greeting_description_something_wrong),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Theme.colors.primary,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Justify
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

                    if (internalBackupExists) {
                        Text(
                            text = stringResource(R.string.screen_greeting_backup_found),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            color = Theme.colors.good,
                            fontSize = 14.sp,
                            fontFamily = InterVariable,
                            textAlign = TextAlign.Justify
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.screen_greeting_backup_not_found),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            color = Theme.colors.veryWeak,
                            fontSize = 14.sp,
                            fontFamily = InterVariable,
                            textAlign = TextAlign.Justify
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    RoundedButton(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        text = if (wipeButtonTapsCount > 0) {
                            pluralStringResource(R.plurals.common_button_tap_n_times_to_confirm, 6 - wipeButtonTapsCount, 6 - wipeButtonTapsCount)
                        } else {
                            if (internalBackupExists)
                                stringResource(R.string.screen_greeting_button_wipe_and_recover)
                            else
                                stringResource(R.string.screen_greeting_button_wipe)
                        }
                    ) {
                        if (wipeButtonTapsCount < 5) {
                            wipeButtonTapsCount++
                            return@RoundedButton
                        }

                        navController.navigate(SetupGetStartedRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }
        }

        if (unableToLoadDecryptionKeys || hasError || promptCanceled) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                RoundedButtonClean(
                    modifier = Modifier.widthIn(max = 512.dp).fillMaxWidth().padding(bottomInset),
                    text = BuildConfig.VERSION_NAME
                ) {
                    if (versionNameTapsCount < 5) {
                        versionNameTapsCount++
                        return@RoundedButtonClean
                    }

                    Signals.suppressMemoryLoss = true
                    Logger.i(TAG, "downloading app log")
                    internalAppLogLauncher.launch("app.log")
                }
            }
        }
    }
}