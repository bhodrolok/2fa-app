package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionGroup
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import kotlinx.serialization.Serializable

@Serializable
object ImportFromExternalRoute

@Composable
fun ImportFromExternal(navController: NavController) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 512.dp)
            ) {
                Spacer(modifier = Modifier.height(topInset + 64.dp))

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.common_title_from)
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable._2fas),
                        primaryText = "2FAS Auth",
                        secondaryText = ".2fas"
                    ) {

                    }

                    SectionButton(
                        painter = painterResource(R.drawable.aegis),
                        primaryText = "Aegis",
                        secondaryText = ".json"
                    ) {

                    }

                    SectionButton(
                        painter = painterResource(R.drawable.ente_auth),
                        primaryText = "Ente Auth",
                        secondaryText = ".json"
                    ) {

                    }

                    SectionButton(
                        painter = painterResource(R.drawable.proton_authenticator),
                        primaryText = "Proton Authenticator",
                        secondaryText = ".json"
                    ) {

                    }

                    SectionButton(
                        painter = painterResource(R.drawable.steam),
                        primaryText = "Steam",
                        secondaryText = ".maFile"
                    ) {

                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }
        }

        StartButtonHeader(title = stringResource(R.string.screen_settings_management_import_from_external)) {
            navController.popBackStack()
        }
    }
}