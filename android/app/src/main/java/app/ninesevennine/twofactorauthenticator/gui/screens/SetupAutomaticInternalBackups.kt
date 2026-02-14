package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.RustInterface
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.SecureElement
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.start2faAndroidMigration
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonClean
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Password
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object SetupAutomaticInternalBackupsRoute

@Composable
fun SetupAutomaticInternalBackups(navController: NavController) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isPasswordValid = Password.isValid(password)

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

            ConfidentialTextBox(
                title = stringResource(R.string.common_password),
                value = password,
                onValueChange = { if (!isLoading) password = it },
                error = !isPasswordValid
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnimatedVisibility(!Password.hasSufficientLength(password)) {
                    Text(
                        text = stringResource(R.string.common_password_8_chars),
                        modifier = Modifier.fillMaxWidth(),
                        color = Theme.colors.error,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )
                }
                AnimatedVisibility(!Password.hasNumber(password)) {
                    Text(
                        text = stringResource(R.string.common_password_1_num),
                        modifier = Modifier.fillMaxWidth(),
                        color = Theme.colors.error,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )
                }
                AnimatedVisibility(!Password.hasSpecialCharacter(password)) {
                    Text(
                        text = stringResource(R.string.common_password_1_special),
                        modifier = Modifier.fillMaxWidth(),
                        color = Theme.colors.error,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )
                }
                AnimatedVisibility(!Password.hasNoLeadingOrTrailingWhitespace(password)) {
                    Text(
                        text = stringResource(R.string.common_password_no_w_or_t),
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
                visible = isPasswordValid
            ) {
                ConfidentialTextBox(
                    title = stringResource(R.string.common_confirm_password),
                    value = confirmPassword,
                    onValueChange = { if (!isLoading) confirmPassword = it },
                    error = password != confirmPassword
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = isPasswordValid && password == confirmPassword,
                transitionSpec = {
                    val slideIn = slideInVertically(
                        animationSpec = tween(durationMillis = 300),
                        initialOffsetY = { fullHeight -> fullHeight }
                    )
                    val fadeInAnim = fadeIn(
                        animationSpec = tween(durationMillis = 300)
                    )
                    val slideOut = slideOutVertically(
                        animationSpec = tween(durationMillis = 300),
                        targetOffsetY = { fullHeight -> -fullHeight }
                    )
                    val fadeOutAnim = fadeOut(
                        animationSpec = tween(durationMillis = 300)
                    )

                    (slideIn + fadeInAnim) togetherWith (slideOut + fadeOutAnim)
                }
            ) {
                if (it) {
                    RoundedButton(
                        text = if (isLoading) spinnerText() else stringResource(R.string.common_button_enable)
                    ) {
                        if (isLoading) return@RoundedButton

                        isLoading = true

                        scope.launch(Dispatchers.IO) {
                            RustInterface().setAutomaticInternalBackupsPassword(password)

                            Config.isUsingAutomaticInternalBackups = true
                            Config.hasCompletedSetup = true
                            SecureElement.saveSecret(RustInterface().getSecret())
                            Config.save(context)

                            start2faAndroidMigration(context)

                            withContext(Dispatchers.Main) {
                                navController.navigate(MainRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                } else {
                    RoundedButtonClean(text = stringResource(R.string.common_button_continue_without)) {
                        if (isLoading) return@RoundedButtonClean

                        Config.isUsingAutomaticInternalBackups = false
                        Config.hasCompletedSetup = true
                        SecureElement.saveSecret(RustInterface().getSecret())
                        Config.save(context)

                        start2faAndroidMigration(context)

                        navController.navigate(MainRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}