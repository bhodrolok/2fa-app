package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.SecureElement
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonClean
import app.ninesevennine.twofactorauthenticator.utils.Logger
import app.ninesevennine.twofactorauthenticator.utils.Tests
import kotlinx.serialization.Serializable

private const val TAG = "SetupBiometricUnlock"

@Serializable
object SetupBiometricUnlockRoute

@Composable
fun SetupBiometricUnlock(navController: NavController) {
    val isFullBiometricAvailable by remember {
        mutableStateOf(
            SecureElement.isBiometricAvailable() && SecureElement.isBiometricKeyLoaded()
        )
    }

    val biometricUnlockText = stringResource(R.string.common_biometric_unlock)
    val cancelText = stringResource(R.string.common_cancel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
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

            if (!isFullBiometricAvailable) {
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

            if (isFullBiometricAvailable) {
                RoundedButton(text = stringResource(R.string.common_button_enable)) {
                    val plainTextBytes = Tests.PSEUDO_SECRET.toByteArray(Charsets.UTF_8)

                    val encrypted = SecureElement.encryptBiometric(plainTextBytes) ?: run {
                        Logger.e(TAG, "failed to encrypt with biometric key")
                        return@RoundedButton
                    }

                    SecureElement.decryptBiometric(
                        title = biometricUnlockText,
                        negative = cancelText,
                        data = encrypted,
                        onSuccess = { decryptedBytes ->
                            if (decryptedBytes.contentEquals(plainTextBytes)) {
                                Logger.i(TAG, "biometric test success")
                                Config.isUsingBiometrics = true
                                navController.navigate(SetupAutomaticInternalBackupsRoute)
                            } else {
                                Logger.e(TAG, "biometric test failed")
                            }
                        },
                        onError = {
                            Logger.e(TAG, "failed to decrypt with biometric key")
                        },
                        onCancel = {
                            Logger.i(TAG, "biometric unlock canceled")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            RoundedButtonClean(text = stringResource(R.string.common_button_continue_without)) {
                val plainTextBytes = Tests.PSEUDO_SECRET.toByteArray(Charsets.UTF_8)

                val encrypted = SecureElement.encryptRegular(plainTextBytes)
                val decrypted = encrypted?.let { SecureElement.decryptRegular(it) }

                if (decrypted != null && decrypted.contentEquals(plainTextBytes)) {
                    Logger.i(TAG, "regular key test success")

                    Config.isUsingBiometrics = false
                    navController.navigate(SetupAutomaticInternalBackupsRoute)
                } else {
                    Logger.e(TAG, "regular key test failed")
                }
            }
        }
    }
}