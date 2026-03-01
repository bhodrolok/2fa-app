package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.KeyStoreRSA
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.LeftButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
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

    // copy otherwise ui change will be visible on pop back.
    var isUsingBiometrics by remember { mutableStateOf(Config.isUsingBiometrics) }

    BackHandler(enabled = isLoading) {
        // intercept the system back button when loading.
    }

    val biometricUnlockText = stringResource(R.string.common_biometric_unlock)
    val cancelText = stringResource(R.string.common_cancel)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
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
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(152.dp),
                tint = Theme.colors.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.common_biometric_unlock),
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 20.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W700
            )

            Spacer(modifier = Modifier.height(12.dp))

            BasicText(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Theme.colors.warning)) {
                        append(stringResource(R.string.common_biometric_unlock_info_part_1) + " ")
                    }
                    withStyle(style = SpanStyle(color = Theme.colors.primary)) {
                        append(stringResource(R.string.common_biometric_unlock_info_part_2))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(fontSize = 14.sp, fontFamily = InterVariable)
            )

            if (!KeyStoreRSA.isBiometricAvailable()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.common_biometric_unlock_error),
                    modifier = Modifier.fillMaxWidth(),
                    color = Theme.colors.info,
                    fontSize = 14.sp,
                    fontFamily = InterVariable
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (KeyStoreAES.isBiometricAvailable()) {
                if (isUsingBiometrics) {
                    RoundedButton(
                        if (isLoading) spinnerText() else stringResource(R.string.common_button_disable)
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
                                    secret = Entry.rustInterface.getSecret(),
                                    onSuccess = {
                                        Logger.i(TAG, "biometrics set to ${Config.isUsingBiometrics}")

                                        Config.internalIsUsingKeyStoreAES = true
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
                        if (isLoading) spinnerText() else stringResource(R.string.common_button_enable)
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
                                    secret = Entry.rustInterface.getSecret(),
                                    onSuccess = {
                                        Logger.i(TAG, "biometrics set to ${Config.isUsingBiometrics}")

                                        Config.internalIsUsingKeyStoreAES = true
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