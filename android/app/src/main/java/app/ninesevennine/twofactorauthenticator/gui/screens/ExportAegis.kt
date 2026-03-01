package app.ninesevennine.twofactorauthenticator.gui.screens

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
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.LeftButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import app.ninesevennine.twofactorauthenticator.utils.Logger
import app.ninesevennine.twofactorauthenticator.utils.Pass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object ExportAegisRoute

private const val TAG = "ExportAegis"

@Composable
fun ExportAegis(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var pendingExportData by remember { mutableStateOf<String?>(null) }

    val isPassValid = Pass.isValid(pass)

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
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
                text = stringResource(R.string.common_export),
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnimatedVisibility(!Pass.hasSufficientLength(pass)) {
                    Text(
                        text = stringResource(R.string.common_pass_8_chars),
                        modifier = Modifier.fillMaxWidth(),
                        color = Theme.colors.error,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )
                }
                AnimatedVisibility(!Pass.hasNumber(pass)) {
                    Text(
                        text = stringResource(R.string.common_pass_1_num),
                        modifier = Modifier.fillMaxWidth(),
                        color = Theme.colors.error,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )
                }
                AnimatedVisibility(!Pass.hasSpecialCharacter(pass)) {
                    Text(
                        text = stringResource(R.string.common_pass_1_special),
                        modifier = Modifier.fillMaxWidth(),
                        color = Theme.colors.error,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )
                }
                AnimatedVisibility(!Pass.hasNoLeadingOrTrailingWhitespace(pass)) {
                    Text(
                        text = stringResource(R.string.common_pass_no_w_or_t),
                        modifier = Modifier.fillMaxWidth(),
                        color = Theme.colors.error,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )
                }
            }

            AnimatedVisibility(
                modifier = Modifier.padding(top = 6.dp),
                visible = isPassValid
            ) {
                ConfidentialTextBox(
                    title = stringResource(R.string.common_confirm_pass),
                    value = confirmPass,
                    onValueChange = { if (!isLoading) confirmPass = it },
                    error = pass != confirmPass
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                modifier = Modifier.padding(top = 6.dp),
                visible = isPassValid && pass == confirmPass
            ) {
                RoundedButton(
                    text = if (isLoading) spinnerText() else stringResource(R.string.common_button_continue)
                ) {
                    if (isLoading) return@RoundedButton

                    isLoading = true

                    scope.launch(Dispatchers.IO) {
                        Logger.i(TAG, "exporting accounts to aegis")

                        val data = Entry.rustInterface.exportAccountsAegis(pass)

                        withContext(Dispatchers.Main) {
                            pendingExportData = data
                            createDocumentLauncher.launch("aegis.json")
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