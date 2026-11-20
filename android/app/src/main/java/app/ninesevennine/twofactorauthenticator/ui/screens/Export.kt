package app.ninesevennine.twofactorauthenticator.ui.screens

import android.util.Log
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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.ui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.ui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.ui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.ui.elements.StrengthIndicator
import app.ninesevennine.twofactorauthenticator.ui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.rustInterface
import app.ninesevennine.twofactorauthenticator.utils.Signals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
object ExportRoute

private const val TAG = "Export"

@Composable
fun Export(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        .let { if (it < 16.dp) 16.dp else it + 16.dp }

    var isLoading by remember { mutableStateOf(false) }

    val passphraseState = remember { TextFieldState() }
    val confirmPassphraseState = remember { TextFieldState() }

    val passphrase = passphraseState.text.toString()
    val confirmPassphrase = confirmPassphraseState.text.toString()
    val passphraseStrength = remember(passphrase) { rustInterface.features_zxcvbn_normalized(passphrase) }

    val isPassphraseValid = passphraseStrength > 0.5f

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) {
            isLoading = false
            return@rememberLauncherForActivityResult
        }

        if (isLoading) return@rememberLauncherForActivityResult

        isLoading = true

        val exportPassphrase = passphraseState.text.toString()

        scope.launch(Dispatchers.IO) {
            val bytes = rustInterface.features_management_native_export_accounts(exportPassphrase)

            try {
                val out = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("openOutputStream returned null for $uri")
                out.use { it.write(bytes) }
            } catch (e: Exception) {
                Log.e(TAG, "createDocumentLauncher", e)
            } finally {
                withContext(Dispatchers.Main) {
                    navController.popBackStack()
                }
            }
        }
    }

    BackHandler(enabled = isLoading) {

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

                ConfidentialTextBox(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.common_title_passphrase),
                    state = passphraseState,
                    enabled = !isLoading,
                    error = passphrase.isEmpty()
                )

                AnimatedVisibility(passphrase.isNotEmpty()) {
                    StrengthIndicator(
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
                        strength = passphraseStrength
                    )
                }

                AnimatedVisibility(isPassphraseValid) {
                    ConfidentialTextBox(
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                        title = stringResource(R.string.common_title_confirm_passphrase),
                        state = confirmPassphraseState,
                        enabled = !isLoading,
                        error = passphrase != confirmPassphrase
                    )
                }

                AnimatedVisibility(isPassphraseValid && passphrase == confirmPassphrase) {
                    RoundedButton(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        text = if (isLoading) spinnerText() else stringResource(R.string.common_title_export)
                    ) {
                        Signals.suppressLockOnExit = true
                        createDocumentLauncher.launch("export.2fa")
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 80.dp))
            }
        }

        StartButtonHeader(title = stringResource(R.string.common_title_export)) {
            if (isLoading) return@StartButtonHeader

            navController.popBackStack()
        }
    }
}