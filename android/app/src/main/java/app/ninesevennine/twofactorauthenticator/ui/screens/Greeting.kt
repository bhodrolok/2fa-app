package app.ninesevennine.twofactorauthenticator.ui.screens

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.management.ImportResult
import app.ninesevennine.twofactorauthenticator.rustInterface
import app.ninesevennine.twofactorauthenticator.ui.InterVariable
import app.ninesevennine.twofactorauthenticator.ui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.ui.elements.InfoPrompt
import app.ninesevennine.twofactorauthenticator.ui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.ui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Signals
import app.ninesevennine.twofactorauthenticator.utils.navigateOnce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object GreetingRoute

@Composable
fun Greeting(navController: NavController) {
    val scope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var fallbackToPassphrase by remember { mutableStateOf(false) }
    var biometricKeyRecreationRequired by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    var importResult by remember { mutableIntStateOf(0) }
    var showImportResultPrompt by remember { mutableStateOf(false) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    val promptTitle = stringResource(R.string.common_title_biometric_unlock)
    val promptTitle2 = stringResource(R.string.common_title_confirm_biometrics)
    val promptNegative = stringResource(R.string.common_button_cancel)

    LaunchedEffect(Signals.authenticate) {
        if (!KeyStoreAES.isBiometricAvailable() || !KeyStoreAES.hasBiometricKey()) {
            fallbackToPassphrase = true
            return@LaunchedEffect
        }

        if (KeyStoreAES.loadBiometricKey() && KeyStoreAES.isBiometricKeyUsable()) {
            KeyStoreAES.loadBiometricKey(
                promptTitle = promptTitle,
                promptNegative = promptNegative,
                onSuccess = {
                    scope.launch(Dispatchers.IO) {
                        val hasError = rustInterface.management_accounts_load() < ImportResult.SUCCESS.value

                        withContext(Dispatchers.Main) {
                            if (hasError) {
                                biometricKeyRecreationRequired = true
                                fallbackToPassphrase = true
                            } else {
                                navController.navigate(MainRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                },
                onCancel = {
                    fallbackToPassphrase = true
                },
                onError = {
                    biometricKeyRecreationRequired = true
                    fallbackToPassphrase = true
                }
            )
        } else {
            biometricKeyRecreationRequired = true
            fallbackToPassphrase = true
        }
    }

    val performPassphraseUnlock = performPassphraseUnlock@ {
        if (isLoading) return@performPassphraseUnlock

        isLoading = true
        importResult = ImportResult.SUCCESS.value

        focusManager.clearFocus()
        keyboardController?.hide()

        scope.launch(Dispatchers.IO) {
            val result = rustInterface.management_accounts_load(passphrase)

            if (result < ImportResult.SUCCESS.value) {
                isLoading = false
                importResult = result
            } else {
                if (biometricKeyRecreationRequired) {
                    KeyStoreAES.deleteBiometricKey()

                    if (!KeyStoreAES.generateBiometricKey() || !KeyStoreAES.loadBiometricKey() || !KeyStoreAES.isBiometricKeyUsable()) {
                        withContext(Dispatchers.Main) {
                            navController.navigateOnce(MainRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }

                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        KeyStoreAES.saveBiometricKey(
                            promptTitle = promptTitle2,
                            promptNegative = promptNegative,
                            onSuccess = {
                                navController.navigate(MainRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onCancel = {
                                navController.navigate(MainRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onError = {
                                navController.navigate(MainRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        navController.navigateOnce(MainRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        }

        Unit
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 512.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topInset + 80.dp))

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
                        error = passphrase.isEmpty() || importResult < ImportResult.SUCCESS.value,
                        onValueChange = {
                            if (!isLoading) {
                                passphrase = it
                            }

                            importResult = ImportResult.SUCCESS.value // Reset
                        },
                        onDone = performPassphraseUnlock
                    )

                    AnimatedVisibility(importResult == ImportResult.INCORRECT_PASSPHRASE.value) {
                        Text(
                            text = stringResource(R.string.common_import_error_incorrect_passphrase),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 8.dp, end = 20.dp),
                            color = Theme.colors.veryWeak,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = InterVariable
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(passphrase.isNotEmpty()) {
                        RoundedButton(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            text = if (isLoading) spinnerText() else stringResource(R.string.screen_greeting_button_unlock),
                            onClick = performPassphraseUnlock
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.logo_rectangle),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Theme.colors.primary
                    )
                }

                Spacer(modifier = Modifier.height(bottomInset + 80.dp))
            }
        }
    }

    InfoPrompt(
        visible = showImportResultPrompt,
        title = when (importResult) {
            ImportResult.INVALID_FORMAT.value -> stringResource(R.string.import_error_title_invalid_format)
            ImportResult.UNSUPPORTED_VERSION.value -> stringResource(R.string.import_error_title_unsupported_version)
            else -> "Unknown"
        },
        body = when (importResult) {
            ImportResult.INVALID_FORMAT.value -> stringResource(R.string.import_error_body_invalid_format)
            ImportResult.UNSUPPORTED_VERSION.value -> stringResource(R.string.import_error_body_unsupported_version)
            else -> "Unknown"
        },
        buttonText = stringResource(R.string.common_button_ok),
        onDismiss = { showImportResultPrompt = false }
    )
}