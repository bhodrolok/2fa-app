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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionGroup
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
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
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
                    painter = painterResource(R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp), clip = true),
                    tint = Color.Unspecified
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.screen_setup_get_started_welcome),
                    color = Theme.colors.primary,
                    fontSize = 18.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700
                )

                Spacer(modifier = Modifier.height(32.dp))

                SectionGroup(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.screen_setup_get_started_section_title)
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.random_alphabet),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_setup_get_started_section_step_1)
                    ) {

                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_fingerprint),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_setup_get_started_section_step_2)
                    ) {

                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                RoundedButton(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    text = stringResource(R.string.screen_setup_get_started_button)
                ) {
                    if (isLoading) return@RoundedButton

                    isLoading = true

                    scope.launch(Dispatchers.IO) {
                        Logger.i(TAG, "get started pressed")

                        KeyStoreAES.deleteKey(isBiometricKey = true)

                        if (KeyStoreAES.isBiometricAvailable()) {
                            KeyStoreAES.generateKey(isBiometricKey = true)
                            if (!KeyStoreAES.loadKey(isBiometricKey = true)) {
                                return@launch
                            }
                        }

                        withContext(Dispatchers.Main) {
                            navController.navigate(SetupCreateAPassphrase)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }
        }
    }
}