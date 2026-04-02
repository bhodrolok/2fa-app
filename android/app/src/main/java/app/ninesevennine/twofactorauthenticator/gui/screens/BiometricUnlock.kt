package app.ninesevennine.twofactorauthenticator.gui.screens

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
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object BiometricUnlockRoute

private const val TAG = "BiometricUnlock"

@Composable
fun BiometricUnlock(navController: NavController) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)

    // copy otherwise ui change will be visible on pop back.
    var isUsingBiometrics by remember { mutableStateOf(Config.isUsingBiometrics) }

    BackHandler(enabled = isLoading) {
        // intercept the system back button when loading.
    }

    val biometricUnlockText = "Confirm Biometrics"
    val cancelText = "Cancel"

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
                    text = stringResource(R.string.common_description_biometric_unlock),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.Normal
                )

                if (KeyStoreAES.isBiometricAvailable()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isUsingBiometrics) {
                        RoundedButton(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            text = if (isLoading) spinnerText() else stringResource(R.string.common_button_disable)
                        ) {
                            if (isLoading) return@RoundedButton

                            isLoading = true

                            scope.launch(Dispatchers.IO) {
                                val isBiometricKey = false

                                KeyStoreAES.deleteKey(isBiometricKey)

                                KeyStoreAES.generateKey(isBiometricKey)
                                if (!KeyStoreAES.loadKey(isBiometricKey)) {
                                    isLoading = false
                                    return@launch
                                }

                                withContext(Dispatchers.Main) {
                                    Config.isUsingBiometrics = isBiometricKey

                                    KeyStoreAES.saveSecret(
                                        title = biometricUnlockText,
                                        negative = cancelText,
                                        secret = if (Config.hasMigratedToSecret002) Entry.rustInterface.getSecret002() else Entry.rustInterface.getSecret001(),
                                        onSuccess = {
                                            Logger.i(TAG, "biometrics set to false")

                                            Config.save(context)

                                            navController.popBackStack()
                                        },
                                        onError = {
                                            isLoading = false
                                        },
                                        onCancel = {
                                            Logger.i(TAG, "biometric prompt canceled")
                                            isLoading = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        RoundedButton(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            text = if (isLoading) spinnerText() else stringResource(R.string.common_button_enable)
                        ) {
                            if (isLoading) return@RoundedButton

                            isLoading = true

                            scope.launch(Dispatchers.IO) {
                                val isBiometricKey = true

                                KeyStoreAES.deleteKey(isBiometricKey)

                                KeyStoreAES.generateKey(isBiometricKey)
                                if (!KeyStoreAES.loadKey(isBiometricKey)) {
                                    isLoading = false
                                    return@launch
                                }

                                withContext(Dispatchers.Main) {
                                    Config.isUsingBiometrics = isBiometricKey

                                    KeyStoreAES.saveSecret(
                                        title = biometricUnlockText,
                                        negative = cancelText,
                                        secret = if (Config.hasMigratedToSecret002) Entry.rustInterface.getSecret002() else Entry.rustInterface.getSecret001(),
                                        onSuccess = {
                                            Logger.i(TAG, "biometrics set to true")

                                            Config.save(context)

                                            navController.popBackStack()
                                        },
                                        onError = {
                                            isLoading = false
                                        },
                                        onCancel = {
                                            Logger.i(TAG, "biometric prompt canceled")
                                            isLoading = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.common_description_biometric_unlock_unsupported),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Theme.colors.info,
                        fontSize = 14.sp,
                        fontFamily = InterVariable
                    )
                }

                Spacer(modifier = Modifier.height(bottomInset + 80.dp))
            }
        }

        StartButtonHeader("") {
            navController.popBackStack()
        }
    }
}