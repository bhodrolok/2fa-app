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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.RustInterface
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.KeyStoreManagement
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.LeftButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Pass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object AutomaticInternalBackupsRoute

@Composable
fun AutomaticInternalBackups(navController: NavController) {
    val context = LocalContext.current

    var pass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isPassValid = Pass.isValid(pass)

    // copy otherwise ui change will be visible on pop back.
    var isUsingAutomaticInternalBackups by remember { mutableStateOf(Config.isUsingAutomaticInternalBackups) }

    BackHandler(enabled = isLoading) {
        // intercept the system back button when loading.
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding(),
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
                text = stringResource(R.string.common_automatic_internal_backups),
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 20.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W700
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (Config.isUsingBiometrics) {
                Text(
                    text = stringResource(R.string.common_automatic_internal_backups_info),
                    modifier = Modifier.fillMaxWidth(),
                    color = Theme.colors.warning,
                    fontSize = 14.sp,
                    fontFamily = InterVariable
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isUsingAutomaticInternalBackups) {
                RoundedButton(text = stringResource(R.string.common_button_disable)) {
                    navController.popBackStack()
                    Config.isUsingAutomaticInternalBackups = false
                    Config.save(context)
                }
            } else {
                ConfidentialTextBox(
                    title = stringResource(R.string.common_pass),
                    value = pass,
                    onValueChange = { if (!isLoading) pass = it },
                    error = !isPassValid
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnimatedVisibility(!Pass.hasSufficientLength(pass)) {
                        Text(
                            text = stringResource(R.string.common_pass_8_chars),
                            modifier = Modifier.fillMaxWidth(),
                            color = Theme.colors.error,
                            fontSize = 14.sp,
                            fontFamily = InterVariable,
                            fontWeight = FontWeight.W700
                        )
                    }
                    AnimatedVisibility(!Pass.hasNumber(pass)) {
                        Text(
                            text = stringResource(R.string.common_pass_1_num),
                            modifier = Modifier.fillMaxWidth(),
                            color = Theme.colors.error,
                            fontSize = 14.sp,
                            fontFamily = InterVariable,
                            fontWeight = FontWeight.W700
                        )
                    }
                    AnimatedVisibility(!Pass.hasSpecialCharacter(pass)) {
                        Text(
                            text = stringResource(R.string.common_pass_1_special),
                            modifier = Modifier.fillMaxWidth(),
                            color = Theme.colors.error,
                            fontSize = 14.sp,
                            fontFamily = InterVariable,
                            fontWeight = FontWeight.W700
                        )
                    }
                    AnimatedVisibility(!Pass.hasNoLeadingOrTrailingWhitespace(pass)) {
                        Text(
                            text = stringResource(R.string.common_pass_no_w_or_t),
                            modifier = Modifier.fillMaxWidth(),
                            color = Theme.colors.error,
                            fontSize = 14.sp,
                            fontFamily = InterVariable,
                            fontWeight = FontWeight.W700
                        )
                    }
                }

                AnimatedVisibility(
                    modifier = Modifier.padding(top = 6.dp),
                    visible = isPassValid
                ) {
                    ConfidentialTextBox(
                        title = stringResource(R.string.common_confirm_pass),
                        value = confirmPass,
                        onValueChange = { if (!isLoading) confirmPass = it },
                        error = pass != confirmPass
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    modifier = Modifier.padding(top = 6.dp),
                    visible = isPassValid && pass == confirmPass
                ) {
                    RoundedButton(
                        text = if (isLoading) spinnerText() else stringResource(R.string.common_button_enable)
                    ) {
                        if (isLoading) return@RoundedButton

                        isLoading = true

                        scope.launch(Dispatchers.IO) {
                            RustInterface().setAutomaticInternalBackupsPass(pass)

                            KeyStoreManagement.saveSecret(RustInterface().getSecret())
                            AccountManagement.saveAccountsUsingAutomaticInternalBackups(context)

                            Config.isUsingAutomaticInternalBackups = true
                            Config.save(context)

                            withContext(Dispatchers.Main) {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }

        LeftButtonHeader(
            title = "",
            onBack = {
                if (isLoading) return@LeftButtonHeader

                navController.popBackStack()
            }
        )
    }
}