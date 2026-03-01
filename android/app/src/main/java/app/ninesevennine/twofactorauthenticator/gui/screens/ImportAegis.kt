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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.LeftButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Document
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object ImportAegisRoute

private const val TAG = "ImportAegis"

@Composable
fun ImportAegis(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pass by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var importData by remember { mutableStateOf("") }
    var importFilename by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf(true) }

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
        openDocumentLauncher.launch(arrayOf("application/json", "text/json"))
    }

    if (importData.isEmpty()) return

    BackHandler(enabled = isLoading) {
        // intercept the system back button when loading.
    }

    val isPassValid = pass.isNotBlank()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.aegis),
                contentDescription = null,
                modifier = Modifier.size(152.dp),
                tint = Theme.colors.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = importFilename,
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 20.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W700
            )

            Spacer(modifier = Modifier.height(12.dp))

            ConfidentialTextBox(
                title = stringResource(R.string.common_pass),
                value = pass,
                onValueChange = { if (!isLoading) pass = it },
                error = !isPassValid
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                AnimatedVisibility(importError) {
                    Text(
                        text = stringResource(R.string.common_incorrect_pass_or_invalid_file),
                        modifier = Modifier.fillMaxWidth(),
                        color = Theme.colors.error,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                modifier = Modifier.padding(top = 6.dp),
                visible = pass.isNotBlank() && importData.isNotEmpty()
            ) {
                RoundedButton(
                    text = if (isLoading) spinnerText() else stringResource(R.string.common_import)
                ) {
                    if (isLoading || importData.isEmpty()) return@RoundedButton

                    isLoading = true
                    importError = false

                    scope.launch(Dispatchers.IO) {
                        Logger.i(TAG, "importing accounts from aegis")
                        val result = Entry.rustInterface.importAccountsAegis(pass, importData)

                        if (result) {
                            AccountManagement.saveAccountsUsingSeed(context)

                            if (Config.isUsingAutomaticInternalBackups) {
                                AccountManagement.saveAccountsUsingAutomaticInternalBackups(context)
                            }

                            withContext(Dispatchers.Main) {
                                navController.popBackStack(MainRoute, false)
                            }
                        } else {
                            Logger.i(TAG, "failed importing accounts from aegis")
                            importError = true
                            isLoading = false
                        }
                    }
                }
            }
        }

        LeftButtonHeader(
            title = "",
            onBack = {
                if (isLoading) return@LeftButtonHeader

                navController.popBackStack()
            }
        )
    }
}