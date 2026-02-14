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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.SecureElement
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object SetupGetStartedRoute

@Composable
fun SetupGetStarted(navController: NavController) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                fontWeight = FontWeight.W700,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.setup_get_started_message_info),
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 14.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            RoundedButton(
                text = if (isLoading) spinnerText() else stringResource(R.string.setup_get_started_button)
            ) {
                if (isLoading) return@RoundedButton

                isLoading = true

                scope.launch(Dispatchers.IO) {
                    Config.hasCompletedSetup = false

                    SecureElement.deleteRegularKey()
                    SecureElement.deleteBiometricKey()

                    SecureElement.generateRegularKey()
                    if (!SecureElement.loadRegularKey()) {
                        return@launch
                    }

                    if (SecureElement.isBiometricAvailable()) {
                        if (!SecureElement.generateBiometricKey() || !SecureElement.loadBiometricKey()) {
                            return@launch
                        }
                    }

                    withContext(Dispatchers.Main) {
                        navController.navigate(SetupGenerateYourRandomSeedRoute)
                    }
                }
            }
        }
    }
}