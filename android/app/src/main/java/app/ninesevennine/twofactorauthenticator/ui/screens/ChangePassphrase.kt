package app.ninesevennine.twofactorauthenticator.ui.screens

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.rustInterface
import app.ninesevennine.twofactorauthenticator.ui.InterVariable
import app.ninesevennine.twofactorauthenticator.ui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.ui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.ui.elements.RoundedButtonClean
import app.ninesevennine.twofactorauthenticator.ui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.ui.elements.StrengthIndicator
import app.ninesevennine.twofactorauthenticator.ui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Signals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object ChangePassphraseRoute

@Composable
fun ChangePassphrase(navController: NavController) {
    var passphrase by remember { mutableStateOf("") }
    var passphraseStrength by remember { mutableFloatStateOf(0f) }
    var confirmPassphrase by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    val isPassphraseValid = passphraseStrength > 0.5f

    BackHandler(enabled = isLoading) {
        // Intercept
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 512.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topInset + 80.dp))

                Icon(
                    painter = painterResource(R.drawable.random_alphabet),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Theme.colors.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.common_title_change_passphrase),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 18.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.common_unlock_passphrase_description),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(16.dp))

                ConfidentialTextBox(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.common_title_passphrase),
                    value = passphrase,
                    onValueChange = {
                        if (!isLoading) {
                            passphrase = it
                            passphraseStrength = rustInterface.features_zxcvbn_normalized(it)
                        }
                    }
                )

                AnimatedVisibility(passphrase.isNotEmpty()) {
                    StrengthIndicator(
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
                        strength = passphraseStrength
                    )
                }

                AnimatedVisibility(isPassphraseValid) {
                    ConfidentialTextBox(
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                        title = stringResource(R.string.common_title_confirm_passphrase),
                        value = confirmPassphrase,
                        onValueChange = { if (!isLoading) confirmPassphrase = it },
                        error = passphrase != confirmPassphrase
                    )
                }

                AnimatedVisibility(isPassphraseValid && passphrase == confirmPassphrase) {
                    RoundedButton(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        text = if (isLoading) spinnerText() else stringResource(R.string.common_button_change)
                    ) {
                        if (isLoading) return@RoundedButton

                        isLoading = true

                        scope.launch(Dispatchers.IO) {
                            rustInterface.management_accounts_save(passphrase)

                            KeyStoreAES.deleteBiometricKey()

                            withContext(Dispatchers.Main) {
                                navController.popBackStack()
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 80.dp))
            }
        }

        StartButtonHeader("") {
            if (isLoading) return@StartButtonHeader

            navController.popBackStack()
        }
    }

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
                text = stringResource(R.string.common_button_disable)
            ) {
                if (isLoading) return@RoundedButtonClean

                isLoading = true

                scope.launch(Dispatchers.IO) {
                    rustInterface.management_accounts_save("")

                    KeyStoreAES.deleteBiometricKey()

                    Signals.requiresAuthentication = false

                    withContext(Dispatchers.Main) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}