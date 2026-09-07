package app.ninesevennine.twofactorauthenticator.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
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
import app.ninesevennine.twofactorauthenticator.ui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.ui.elements.StartButtonHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object BiometricUnlockRoute

@Composable
fun BiometricUnlock(navController: NavController) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    val hasPassphraseUnlock by remember { mutableStateOf(rustInterface.client_has_salt_and_key()) }
    val isBiometricAvailable by remember { mutableStateOf(KeyStoreAES.isBiometricAvailable()) }
    val hasBiometricUnlock by remember { mutableStateOf(KeyStoreAES.hasBiometricKey()) }

    BackHandler(enabled = isLoading) {
        // Intercept
    }

    val promptTitle = stringResource(R.string.common_title_confirm_biometrics)
    val promptNegative = stringResource(R.string.common_button_cancel)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 512.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topInset + 80.dp))

                Icon(
                    painter = painterResource(R.drawable.mi_fingerprint),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Theme.colors.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.common_title_biometric_unlock),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 18.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.common_biometric_unlock_description),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.Normal
                )

                if (!hasPassphraseUnlock) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.common_description_biometric_unlock_requires_passphrase),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Theme.colors.info,
                        fontSize = 14.sp,
                        fontFamily = InterVariable
                    )
                } else if (!isBiometricAvailable) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.common_description_biometric_unlock_unsupported),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Theme.colors.info,
                        fontSize = 14.sp,
                        fontFamily = InterVariable
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (hasBiometricUnlock) {
                        RoundedButton(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            text = stringResource(R.string.common_button_disable)
                        ) {
                            scope.launch(Dispatchers.IO) {
                                KeyStoreAES.deleteBiometricKey()

                                withContext(Dispatchers.Main) {
                                    navController.popBackStack()
                                }
                            }
                        }
                    } else {
                        RoundedButton(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            text = stringResource(R.string.common_button_enable)
                        ) {
                            if (isLoading) return@RoundedButton

                            isLoading = true

                            scope.launch(Dispatchers.IO) {
                                KeyStoreAES.deleteBiometricKey()

                                if (!KeyStoreAES.generateBiometricKey() || !KeyStoreAES.loadBiometricKey() || !KeyStoreAES.isBiometricKeyUsable()) {
                                    isLoading = false

                                    return@launch
                                }

                                withContext(Dispatchers.Main) {
                                    KeyStoreAES.saveBiometricKey(
                                        promptTitle = promptTitle,
                                        promptNegative = promptNegative,
                                        onSuccess = {
                                            navController.popBackStack()
                                        },
                                        onCancel = {
                                            isLoading = false
                                        },
                                        onError = {
                                            isLoading = false
                                        }
                                    )
                                }
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
}