package app.ninesevennine.twofactorauthenticator.gui.screens

import android.net.Uri
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
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.management.ImportResult
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.InfoPrompt
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionGroup
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Logger
import app.ninesevennine.twofactorauthenticator.utils.Signals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object ImportFromExternalRoute

private const val TAG = "ImportFromExternal"

private enum class ExternalProvider {
    NONE,
    PLAIN,
    _2FAS,
    AEGIS,
    PROTON
}

@Composable
fun ImportFromExternal(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    var selectedProvider by remember { mutableStateOf(ExternalProvider.NONE) }
    var importData by remember { mutableStateOf("") }
    var importResult by remember { mutableIntStateOf(0) }
    var showInfoPrompt by remember { mutableStateOf(false) }
    var showPassphraseScreen by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true

            scope.launch(Dispatchers.IO) {
                try {
                    val fileData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    }

                    if (fileData != null) {

                        Entry.rustInterface.management_zeroize_hostages()

                        val result = when (selectedProvider) {
                            ExternalProvider.PLAIN -> {
                                Entry.rustInterface.features_management_plain_import_into_hostages(fileData)
                            }
                            ExternalProvider._2FAS -> {
                                Entry.rustInterface.features_management_2fas_import_into_hostages(fileData, "")
                            }
                            ExternalProvider.AEGIS -> {
                                Entry.rustInterface.features_management_aegis_import_into_hostages(fileData, "")
                            }
                            ExternalProvider.PROTON -> {
                                Entry.rustInterface.features_management_proton_import_into_hostages(fileData, "")
                            }
                            else -> ImportResult.INVALID_FORMAT.value
                        }

                        withContext(Dispatchers.Main) {
                            importResult = result

                            if (importResult < ImportResult.SUCCESS.value) {
                                if (importResult == ImportResult.INCORRECT_PASSPHRASE.value) {
                                    importData = fileData
                                    importResult = ImportResult.SUCCESS.value // Reset
                                    showPassphraseScreen = true
                                } else {
                                    showInfoPrompt = true
                                }
                            } else {
                                Logger.i(TAG, "successfully imported $result account(s) into hostages")

                                Entry.rustInterface.management_merge_hostages_into_accounts()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "openDocumentLauncher", e)
                } finally {
                    Entry.rustInterface.management_zeroize_hostages()
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            }
        }
    }

    BackHandler(enabled = isLoading) {
        // Intercept the system back button when loading.
    }

    val importWithPassphrase = importWithPassphrase@ {
        if (isLoading) return@importWithPassphrase

        isLoading = true
        importResult = ImportResult.SUCCESS.value // Reset

        focusManager.clearFocus()
        keyboardController?.hide()

        scope.launch(Dispatchers.IO) {
            try {
                val result = when (selectedProvider) {
                    ExternalProvider._2FAS -> {
                        Entry.rustInterface.features_management_2fas_import_into_hostages(importData, passphrase)
                    }
                    ExternalProvider.AEGIS -> {
                        Entry.rustInterface.features_management_aegis_import_into_hostages(importData, passphrase)
                    }
                    ExternalProvider.PROTON -> {
                        Entry.rustInterface.features_management_proton_import_into_hostages(importData, passphrase)
                    }
                    else -> ImportResult.INVALID_FORMAT.value
                }

                withContext(Dispatchers.Main) {
                    importResult = result

                    if (result < ImportResult.SUCCESS.value) {
                        if (result != ImportResult.INCORRECT_PASSPHRASE.value) {
                            showInfoPrompt = true
                        }
                    } else {
                        Logger.i(TAG, "successfully imported $result account(s) into hostages")

                        Entry.rustInterface.management_merge_hostages_into_accounts()
                        Entry.rustInterface.management_zeroize_hostages()
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "importWithPassphrase", e)
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }

        Unit
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (showPassphraseScreen) {
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
                        painter = painterResource(R.drawable.mi_download),
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
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp),
                            color = Theme.colors.veryWeak,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = InterVariable
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(passphrase.isNotEmpty()) {
                        RoundedButton(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            text = if (isLoading) spinnerText() else stringResource(R.string.common_button_continue),
                        ) {
                            importWithPassphrase()
                        }
                    }

                    Spacer(modifier = Modifier.height(bottomInset + 80.dp))
                }
            }
        } else {
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
                            painter = painterResource(R.drawable.mi_file_copy),
                            tint = Theme.colors.primary,
                            primaryText = stringResource(R.string.common_title_plaintext),
                            secondaryText = ".txt"
                        ) {
                            selectedProvider = ExternalProvider.PLAIN

                            Signals.suppressLockOnExit = true
                            openDocumentLauncher.launch(arrayOf(
                                "text/plain",
                                "application/octet-stream"
                            ))
                        }

                        SectionButton(
                            painter = painterResource(R.drawable._2fas),
                            primaryText = "2FAS Auth",
                            secondaryText = ".2fas"
                        ) {
                            selectedProvider = ExternalProvider._2FAS

                            Signals.suppressLockOnExit = true
                            openDocumentLauncher.launch(arrayOf(
                                "application/json",
                                "application/octet-stream",
                                "text/plain"
                            ))
                        }

                        SectionButton(
                            painter = painterResource(R.drawable.aegis),
                            primaryText = "Aegis",
                            secondaryText = ".json"
                        ) {
                            selectedProvider = ExternalProvider.AEGIS

                            Signals.suppressLockOnExit = true
                            openDocumentLauncher.launch(arrayOf(
                                "application/json",
                                "text/json",
                                "application/octet-stream"
                            ))
                        }

                        SectionButton(
                            painter = painterResource(R.drawable.proton_authenticator),
                            primaryText = "Proton Authenticator",
                            secondaryText = ".json"
                        ) {
                            selectedProvider = ExternalProvider.PROTON

                            Signals.suppressLockOnExit = true
                            openDocumentLauncher.launch(arrayOf(
                                "application/json",
                                "text/json",
                                "application/octet-stream"
                            ))
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
        }

        StartButtonHeader(title = stringResource(R.string.screen_settings_management_import_from_external)) {
            navController.popBackStack()
        }
    }

    InfoPrompt(
        visible = showInfoPrompt,
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
        onDismiss = { showInfoPrompt = false }
    )
}