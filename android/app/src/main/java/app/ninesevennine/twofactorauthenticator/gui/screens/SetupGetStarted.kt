package app.ninesevennine.twofactorauthenticator.gui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonClean
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object SetupGetStartedRoute

private const val TAG = "SetupGetStarted"

@Composable
fun SetupGetStarted(navController: NavController) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val internalAppLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val logContent = Logger.getFullLog()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(logContent.toByteArray())
                }
            } catch (e: Exception) {
                Logger.e(TAG, "error saving log file ${e.message}")
            }
        }
    }

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
                painter = painterResource(R.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(152.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(48.dp), clip = false)
                    .clip(RoundedCornerShape(48.dp)),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.setup_get_started_message_welcome),
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 20.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W700
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.setup_get_started_message_info),
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 14.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W500
            )

            Spacer(modifier = Modifier.height(24.dp))

            RoundedButton(
                text = if (isLoading) spinnerText() else stringResource(R.string.setup_get_started_button)
            ) {
                if (isLoading) return@RoundedButton

                isLoading = true

                scope.launch(Dispatchers.IO) {
                    AccountManagement.deleteAccountsUsingSeed(context)

                    Config.internalIsUsingKeyStoreAES = true
                    Config.hasCompletedSetup = false

                    Config.save(context)

                    KeyStoreAES.deleteKey(isBiometricKey = false)
                    KeyStoreAES.deleteKey(isBiometricKey = true)

                    KeyStoreAES.generateKey(isBiometricKey = false)
                    if (!KeyStoreAES.loadKey(isBiometricKey = false)) {
                        hasError = true
                        return@launch
                    }

                    if (KeyStoreAES.isBiometricAvailable()) {
                        KeyStoreAES.generateKey(isBiometricKey = true)
                        if (!KeyStoreAES.loadKey(isBiometricKey = true)) {
                            hasError = true
                            return@launch
                        }
                    }

                    withContext(Dispatchers.Main) {
                        navController.navigate(SetupGenerateYourRandomSeedRoute)
                    }
                }
            }

            if (hasError) {
                Spacer(modifier = Modifier.height(12.dp))

                RoundedButtonClean(stringResource(R.string.common_download_app_log)) {
                    Logger.i(TAG, "downloading app log")
                    internalAppLogLauncher.launch("app.log")
                }
            }

        }
    }
}