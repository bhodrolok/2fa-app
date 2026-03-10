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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.serialization.Serializable

@Serializable
object TempScreenRoute

private const val TAG = "TempScreen"

@Composable
fun TempScreen(navController: NavController) {
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
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(152.dp),
                tint = Theme.colors.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Important Message",
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 20.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W700
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "You are still using the old keystore. The app now uses a new AES-based keystore introduced in Beta 22. To migrate, turn biometric unlock off and then back on in the app. You can also tap \"Wipe and Start Over\" and recover your accounts if automatic internal backups are enabled. If the migration is successful, this message will no longer appear. Support for the old keystore will be removed soon.",
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 14.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W500
            )

            Spacer(modifier = Modifier.height(24.dp))

            RoundedButton("Okay, I understand") {
                Config.hasSeenKeyStoreMessage = true

                Logger.i(TAG, "hasSeenKeyStoreMessage set to true")

                navController.popBackStack()
            }
        }
    }
}