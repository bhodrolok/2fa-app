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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.Signals
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Document
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object ImportRoute

private const val TAG = "Import"

@Composable
fun Import(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var passphrase by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var importData by remember { mutableStateOf("") }
    var importFilename by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf(false) }
    var importErrorCode by remember { mutableIntStateOf(0) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                importFilename = Document.getName(context, uri) ?: "???"

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    importData = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            } catch (_: Exception) {
                navController.popBackStack()
                return@rememberLauncherForActivityResult
            }
        } else {
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        Signals.suppressMemoryLoss = true
        openDocumentLauncher.launch(arrayOf("application/octet-stream", "text/*"))
    }

    if (importData.isEmpty()) return

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
                    painter = painterResource(R.drawable.mi_download),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Theme.colors.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.common_title_import),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 18.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.common_import_description, importFilename),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(16.dp))

                ConfidentialTextBox(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.common_title_passphrase),
                    value = passphrase,
                    onValueChange = {
                        if (!isLoading) {
                            passphrase = it
                        }

                        importError = false
                    },
                    error = passphrase.isEmpty() || importError
                )

                AnimatedVisibility(importError) {
                    Text(
                        text = when (importErrorCode) {
                            AccountManagement.IMPORT_ERROR_NO_ACCOUNTS -> stringResource(R.string.common_import_error_no_accounts)
                            AccountManagement.IMPORT_ERROR_INCORRECT_PASSPHRASE -> stringResource(R.string.common_import_error_incorrect_passphrase)
                            AccountManagement.IMPORT_ERROR_OUTDATED_VERSION -> stringResource(R.string.common_import_error_outdated)
                            AccountManagement.IMPORT_ERROR_INVALID_FILE -> stringResource(R.string.common_import_error_invalid)
                            else -> "Unknown error"
                        },
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp),
                        color = Theme.colors.veryWeak,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = InterVariable
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                RoundedButton(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    text = if (isLoading) spinnerText() else stringResource(R.string.common_title_import)
                ) {
                    if (isLoading || importData.isEmpty()) return@RoundedButton

                    isLoading = true
                    importError = false

                    scope.launch(Dispatchers.IO) {
                        Logger.i(TAG, "importing accounts")

                        val result = Entry.rustInterface.importAccounts(passphrase, importData)

                        if (result > 0) {
                            Logger.i(TAG, "successfully imported $result account(s)")

                            AccountManagement.saveAccountsUsingKey(context)

                            if (Config.isUsingAutomaticBackups) {
                                AccountManagement.saveAccountsUsingAutomaticBackupsKey(context)
                            }

                            withContext(Dispatchers.Main) {
                                navController.popBackStack(MainRoute, false)
                            }
                        } else {
                            Logger.i(TAG, "failed importing accounts")
                            importError = true
                            importErrorCode = result
                            isLoading = false
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