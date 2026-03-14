package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBoxNew
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonCleanNew
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonNew
import app.ninesevennine.twofactorauthenticator.gui.elements.StrengthIndicator
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object SetupAutomaticBackupsRoute

private const val TAG = "SetupAutomaticBackups"

@Composable
fun SetupAutomaticBackups(navController: NavController) {
    val context = LocalContext.current

    var passphrase by remember { mutableStateOf("") }
    var passphraseStrength by remember { mutableFloatStateOf(0f) }
    var confirmPassphrase by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)

    val isPassphraseValid = passphraseStrength > 0.5f

    BackHandler(enabled = isLoading) {
        // intercept the system back button when loading.
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

                Icon(
                    painter = painterResource(R.drawable.mi_settings_backup_restore),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Theme.colors.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Automatic Backups",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 18.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Whenever you add, modify, or import accounts, they are automatically exported to the app's internal storage. This location is confidential and inaccessible to other apps or system backup services.",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.secondary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "We highly recommend enabling this to ensure you can recover your accounts even if your decryption keys are ever invalidated.",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.secondary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Justify
                )

                if (AccountManagement.hasAccountsUsingAutomaticInternalBackups(context)) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "A backup was found in the app's internal storage. Enter your passphrase below to recover your accounts.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Theme.colors.info,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        textAlign = TextAlign.Justify
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ConfidentialTextBoxNew(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = "Passphrase",
                        value = passphrase,
                        onValueChange = {
                            if (!isLoading) {
                                passphrase = it
                            }

                            recoveryError = false
                        },
                        error = passphrase.isEmpty() || recoveryError
                    )

                    AnimatedVisibility(recoveryError) {
                        Text(
                            text = "Incorrect passphrase",
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp),
                            color = Theme.colors.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = InterVariable
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    RoundedButtonNew(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        text = if (isLoading) spinnerText() else "Recover"
                    ) {
                        if (isLoading) return@RoundedButtonNew

                        isLoading = true
                        recoveryError = false

                        scope.launch(Dispatchers.IO) {
                            val data = AccountManagement.getAccountsUsingAutomaticInternalBackups(context)
                            if (data == null) {
                                recoveryError = true
                                isLoading = false
                                return@launch
                            }

                            val result = Entry.rustInterface.importAccounts(passphrase, data)

                            if (result) {
                                AccountManagement.saveAccountsUsingSeed(context)
                                Entry.rustInterface.setAutomaticInternalBackupsPassphrase(passphrase)

                                Config.isUsingAutomaticInternalBackups = true

                                Logger.i(TAG, "automatic backups set to true")

                                Config.save(context)

                                withContext(Dispatchers.Main) {
                                    navController.navigate(GreetingRoute) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            } else {
                                recoveryError = true
                                isLoading = false
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))

                    ConfidentialTextBoxNew(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = "Passphrase",
                        value = passphrase,
                        onValueChange = {
                            if (!isLoading) {
                                passphrase = it
                                passphraseStrength = Entry.rustInterface.estimatePassphraseStrength(it)
                            }
                        }
                    )

                    StrengthIndicator(
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
                        strength = passphraseStrength
                    )

                    AnimatedVisibility(isPassphraseValid) {
                        ConfidentialTextBoxNew(
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                            title = "Confirm Passphrase",
                            value = confirmPassphrase,
                            onValueChange = { if (!isLoading) confirmPassphrase = it },
                            error = passphrase != confirmPassphrase
                        )
                    }

                    AnimatedVisibility(isPassphraseValid && passphrase == confirmPassphrase) {
                        RoundedButtonNew(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            text = if (isLoading) spinnerText() else "Enable"
                        ) {
                            if (isLoading) return@RoundedButtonNew

                            isLoading = true

                            scope.launch(Dispatchers.IO) {
                                Entry.rustInterface.setAutomaticInternalBackupsPassphrase(passphrase)

                                Config.isUsingAutomaticInternalBackups = true

                                Logger.i(TAG, "automatic backups set to true")

                                Config.save(context)

                                withContext(Dispatchers.Main) {
                                    navController.navigate(GreetingRoute) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            RoundedButtonCleanNew(
                modifier = Modifier.widthIn(max = 512.dp).fillMaxWidth().padding(bottomInset),
                text = "Continue Without"
            ) {
                if (isLoading) return@RoundedButtonCleanNew

                Config.isUsingAutomaticInternalBackups = false

                Logger.i(TAG, "automatic backups set to false")

                Config.save(context)

                navController.navigate(GreetingRoute) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}