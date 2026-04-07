package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.mutableFloatStateOf
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
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.StrengthIndicator
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Logger
import app.ninesevennine.twofactorauthenticator.utils.Signals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object ExportRoute

private const val TAG = "Export"

@Composable
fun Export(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var passphrase by remember { mutableStateOf("") }
    var passphraseStrength by remember { mutableFloatStateOf(0f) }
    var confirmPassphrase by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var pendingExportData by remember { mutableStateOf<String?>(null) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    val isPassphraseValid = passphraseStrength > 0.5f

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val data = pendingExportData

        if (uri == null || data == null) {
            isLoading = false
            return@rememberLauncherForActivityResult
        }

        scope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(data.toByteArray())
            }

            withContext(Dispatchers.Main) {
                isLoading = false
                navController.popBackStack()
            }
        }
    }

    BackHandler(enabled = isLoading) {
        // intercept the system back button when loading.
    }

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
                    painter = painterResource(R.drawable.mi_upload),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Theme.colors.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.common_title_export),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 18.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.screen_export_description),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(16.dp))

                ConfidentialTextBox(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.common_title_passphrase),
                    value = passphrase,
                    onValueChange = {
                        if (!isLoading) {
                            passphrase = it
                            passphraseStrength = Entry.rustInterface.estimatePassphraseStrength(it)
                        }
                    }
                )

                StrengthIndicator(
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
                    strength = passphraseStrength
                )

                AnimatedVisibility(isPassphraseValid) {
                    ConfidentialTextBox(
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                        title = stringResource(R.string.common_title_confirm_passphrase),
                        value = confirmPassphrase,
                        onValueChange = { if (!isLoading) confirmPassphrase = it },
                        error = passphrase != confirmPassphrase
                    )
                }

                AnimatedVisibility(isPassphraseValid && passphrase == confirmPassphrase) {
                    RoundedButton(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        text = if (isLoading) spinnerText() else stringResource(R.string.common_button_continue)
                    ) {
                        if (isLoading) return@RoundedButton

                        isLoading = true

                        scope.launch(Dispatchers.IO) {
                            Logger.i(TAG, "exporting accounts")

                            val data = Entry.rustInterface.exportAccounts(passphrase)

                            withContext(Dispatchers.Main) {
                                Signals.suppressLockOnExit = true
                                pendingExportData = data
                                createDocumentLauncher.launch("export.2fa")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 80.dp))
            }
        }

        StartButtonHeader("") {
            navController.popBackStack()
        }
    }
}