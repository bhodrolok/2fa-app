package app.ninesevennine.twofactorauthenticator.gui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonCleanNew
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonNew
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.serialization.Serializable


@Serializable
object SetupBiometricUnlockRoute

private const val TAG = "SetupBiometricUnlock"

@Composable
fun SetupBiometricUnlock(navController: NavController) {
    val isFullBiometricAvailable by remember {
        mutableStateOf(
            KeyStoreAES.isBiometricAvailable() && KeyStoreAES.isBiometricKeyLoaded()
        )
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 512.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topInset + 16.dp))

                Icon(
                    painter = painterResource(R.drawable.mi_fingerprint),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Theme.colors.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Biometric Unlock",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 18.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Using biometric unlock may invalidate your decryption keys if your lock screen or private spaces change, your device updates, or if the app hasn't been opened in a while. You can always recover your accounts by having automatic backups enabled.",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.secondary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Justify
                )

                if (isFullBiometricAvailable) {
                    Spacer(modifier = Modifier.height(16.dp))

                    RoundedButtonNew(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        text = "Enable"
                    ) {
                        Config.isUsingBiometrics = true

                        Logger.i(TAG, "biometrics set to true")

                        navController.navigate(SetupAutomaticBackupsRoute)
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Biometric unlock may not be supported on this device. Make sure you have biometrics set up in your device settings.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Theme.colors.info,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        textAlign = TextAlign.Justify
                    )
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
                Config.isUsingBiometrics = false

                Logger.i(TAG, "biometrics set to false")

                navController.navigate(SetupAutomaticBackupsRoute)
            }
        }
    }
}