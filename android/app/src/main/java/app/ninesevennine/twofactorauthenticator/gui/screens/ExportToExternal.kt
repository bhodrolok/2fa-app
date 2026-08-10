package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionGroup
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.utils.Logger
import app.ninesevennine.twofactorauthenticator.utils.Signals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.IOException


@Serializable
object ExportToExternalRoute

private const val TAG = "ExportToExternal"

@Composable
fun ExportToExternal(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    var isLoading by remember { mutableStateOf(false) }

    val createDocumentForPlain = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) {
            isLoading = false
            return@rememberLauncherForActivityResult
        }

        scope.launch(Dispatchers.IO) {
            try {
                Entry.rustInterface.management_zeroize_hostages()
                Entry.rustInterface.management_copy_accounts_into_hostages()

                val txt = Entry.rustInterface.features_management_plain_export_hostages()

                val out = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("openOutputStream returned null for $uri")
                out.use { it.write(txt.toByteArray()) }
            } catch (e: Exception) {
                Logger.e(TAG, "createDocumentForPlain", e)
            } finally {
                Entry.rustInterface.management_zeroize_hostages()
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    BackHandler(enabled = isLoading) {
        // Intercept the system back button when loading.
    }

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
                    title = stringResource(R.string.common_title_to)
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.mi_file_copy),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.common_title_plaintext),
                        secondaryText = ".txt"
                    ) {
                        Signals.suppressLockOnExit = true
                        createDocumentForPlain.launch("export.txt")
                    }

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
                        painter = painterResource(R.drawable.proton_authenticator),
                        primaryText = "Proton Authenticator",
                        secondaryText = ".json"
                    ) {

                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }
        }

        StartButtonHeader(title = stringResource(R.string.screen_settings_management_export_to_external)) {
            navController.popBackStack()
        }
    }
}