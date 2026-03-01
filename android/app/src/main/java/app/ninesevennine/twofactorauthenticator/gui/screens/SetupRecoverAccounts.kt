package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonClean
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object SetupRecoverAccountsRoute

private const val TAG = "SetupRecoverAccounts"

@Composable
fun SetupRecoverAccounts(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pass by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var recoveryError by remember { mutableStateOf(true) }

    val isPassValid = pass.isNotBlank()

    BackHandler(enabled = isLoading) {
        // intercept the system back button when loading.
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SettingsBackupRestore,
                contentDescription = null,
                modifier = Modifier.size(152.dp),
                tint = Theme.colors.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.common_recover_accounts),
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 20.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W700
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.main_recover_accounts_info),
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 14.sp,
                fontFamily = InterVariable
            )

            Spacer(modifier = Modifier.height(12.dp))

            ConfidentialTextBox(
                title = stringResource(R.string.common_pass),
                value = pass,
                onValueChange = {
                    if (!isLoading) {
                        pass = it
                    }

                    recoveryError = false
                },
                error = !isPassValid || recoveryError
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                AnimatedVisibility(recoveryError) {
                    Text(
                        text = stringResource(R.string.common_incorrect_pass),
                        modifier = Modifier.fillMaxWidth(),
                        color = Theme.colors.error,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            RoundedButton(
                text = if (isLoading) spinnerText() else stringResource(R.string.common_recover_accounts)
            ) {
                if (isLoading) return@RoundedButton

                isLoading = true
                recoveryError = false

                scope.launch(Dispatchers.IO) {
                    val data = AccountManagement.getAccountsUsingAutomaticInternalBackups(context)
                    if (data == null) {
                        recoveryError = true
                        isLoading = false
                        return@launch
                    }

                    val result = Entry.rustInterface.importAccounts(pass, data)

                    if (result) {
                        AccountManagement.saveAccountsUsingSeed(context)
                        Entry.rustInterface.setAutomaticInternalBackupsPass(pass)

                        Config.isUsingAutomaticInternalBackups = true

                        Logger.i(TAG, "automatic internal backups set to ${Config.isUsingAutomaticInternalBackups}")

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

            Spacer(modifier = Modifier.height(12.dp))

            RoundedButtonClean(text = stringResource(R.string.common_button_continue_without)) {
                if (isLoading) return@RoundedButtonClean

                Config.isUsingAutomaticInternalBackups = false

                Logger.i(TAG, "automatic internal backups set to ${Config.isUsingAutomaticInternalBackups}")

                Config.save(context)

                navController.navigate(GreetingRoute) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}