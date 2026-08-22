package app.ninesevennine.twofactorauthenticator.gui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.management.ImportResult
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.InfoPrompt
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.rustInterface
import app.ninesevennine.twofactorauthenticator.utils.Signals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
data class ImportFromAegisEncryptedRoute(val uri: String)

@Composable
fun ImportFromAegisEncrypted(navController: NavController, uri: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    var isLoading by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    var importResult by remember { mutableIntStateOf(0) }
    var showImportResultPrompt by remember { mutableStateOf(false) }

    val importWithPassphrase = importWithPassphrase@{
        if (isLoading) return@importWithPassphrase

        isLoading = true

        focusManager.clearFocus()
        keyboardController?.hide()

        scope.launch(Dispatchers.IO) {
            val bytes = try {
                val input = context.contentResolver.openInputStream(uri.toUri())
                    ?: throw IOException("openInputStream returned null for $uri")
                input.use { it.readBytes() }
            } catch (e: Exception) {
                Log.e("ImportFromAegisEncrypted", "importWithPassphrase", e)
                withContext(Dispatchers.Main) { isLoading = false }
                return@launch
            }

            val result = rustInterface.features_management_aegis_import_into_hostages(bytes, passphrase)
            withContext(Dispatchers.Main) {
                if (result >= ImportResult.SUCCESS.value) {
                    Signals.checkHostages = true
                    navController.popBackStack(MainRoute, false)
                } else {
                    importResult = result

                    if (result != ImportResult.INCORRECT_PASSPHRASE.value) {
                        showImportResultPrompt = true
                    }

                    isLoading = false
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
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 512.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topInset + 80.dp))

                Icon(
                    painter = painterResource(R.drawable.aegis),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Theme.colors.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                ConfidentialTextBox(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.common_title_passphrase),
                    value = passphrase,
                    error = passphrase.isEmpty() || importResult < ImportResult.SUCCESS.value,
                    onValueChange = {
                        if (!isLoading) {
                            passphrase = it
                        }

                        importResult = ImportResult.SUCCESS.value // Reset
                    },
                    onDone = importWithPassphrase
                )

                AnimatedVisibility(importResult == ImportResult.INCORRECT_PASSPHRASE.value) {
                    Text(
                        text = stringResource(R.string.common_import_error_incorrect_passphrase),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 8.dp, end = 20.dp),
                        color = Theme.colors.veryWeak,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = InterVariable
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(passphrase.isNotEmpty()) {
                    RoundedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        text = if (isLoading) spinnerText() else stringResource(R.string.common_title_import),
                    ) {
                        importWithPassphrase()
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 80.dp))
            }
        }

        StartButtonHeader(title = stringResource(R.string.screen_settings_management_import_from_aegis)) {
            navController.popBackStack()
        }
    }

    InfoPrompt(
        visible = showImportResultPrompt,
        title = when (importResult) {
            ImportResult.INVALID_FORMAT.value -> stringResource(R.string.import_error_title_invalid_format)
            ImportResult.UNSUPPORTED_VERSION.value -> stringResource(R.string.import_error_title_unsupported_version)
            else -> "Unknown"
        },
        body = when (importResult) {
            ImportResult.INVALID_FORMAT.value -> stringResource(R.string.import_error_body_invalid_format)
            ImportResult.UNSUPPORTED_VERSION.value -> stringResource(R.string.import_error_body_unsupported_version)
            else -> "Unknown"
        },
        buttonText = stringResource(R.string.common_button_ok),
        onDismiss = { showImportResultPrompt = false }
    )
}