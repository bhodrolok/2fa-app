package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.elements.ConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionGroup
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.StrengthIndicator
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
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

    var showPassphraseScreen by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    var passphraseStrength by remember { mutableFloatStateOf(0f) }
    var confirmPassphrase by remember { mutableStateOf("") }

    val isPassphraseValid = passphraseStrength > 0.5f

    val createDocumentForPlain = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) {
            isLoading = false
            return@rememberLauncherForActivityResult
        }

        isLoading = true

        scope.launch(Dispatchers.IO) {
            try {
                Entry.rustInterface.client_zeroize_hostages()
                Entry.rustInterface.management_copy_accounts_into_hostages()

                val txt = Entry.rustInterface.features_management_plain_export_hostages()

                val out = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("openOutputStream returned null for $uri")
                out.use { it.write(txt.toByteArray()) }
            } catch (e: Exception) {
                Logger.e(TAG, "createDocumentForPlain", e)
            } finally {
                Entry.rustInterface.client_zeroize_hostages()
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    val createDocumentForJson = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            isLoading = false
            return@rememberLauncherForActivityResult
        }

        isLoading = true

        scope.launch(Dispatchers.IO) {
            try {
                Entry.rustInterface.client_zeroize_hostages()
                Entry.rustInterface.management_copy_accounts_into_hostages()

                val json = Entry.rustInterface.features_management_aegis_export_hostages(passphrase)

                val out = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("openOutputStream returned null for $uri")
                out.use { it.write(json.toByteArray()) }
            } catch (e: Exception) {
                Logger.e(TAG, "createDocumentForJson", e)
            } finally {
                Entry.rustInterface.client_zeroize_hostages()
                withContext(Dispatchers.Main) {
                    passphrase = ""
                    confirmPassphrase = ""
                    passphraseStrength = 0f
                    showPassphraseScreen = false
                    isLoading = false
                }
            }
        }
    }

    BackHandler(enabled = isLoading || showPassphraseScreen) {
        if (!isLoading) showPassphraseScreen = false
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = showPassphraseScreen,
            transitionSpec = {
                (slideInVertically { it / 6 } + fadeIn()) togetherWith (slideOutVertically { it / 6 } + fadeOut())
            }
        ) { passphraseScreen ->
            if (passphraseScreen) {
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
                            value = passphrase,
                            error = passphrase.isEmpty(),
                            onValueChange = {
                                if (!isLoading) {
                                    passphrase = it
                                    passphraseStrength = Entry.rustInterface.features_zxcvbn_normalized(it)
                                }
                            }
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
                                value = confirmPassphrase,
                                onValueChange = { if (!isLoading) confirmPassphrase = it },
                                error = passphrase != confirmPassphrase
                            )
                        }

                        AnimatedVisibility(isPassphraseValid && passphrase == confirmPassphrase) {
                            RoundedButton(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                                text = if (isLoading) spinnerText() else stringResource(R.string.common_title_export)
                            ) {
                                Signals.suppressLockOnExit = true
                                createDocumentForJson.launch("aegis.json")
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
                            title = stringResource(R.string.common_title_to)
                        ) {
                            SectionButton(
                                painter = painterResource(R.drawable.mi_file_copy),
                                tint = Theme.colors.primary,
                                primaryText = stringResource(R.string.common_title_plaintext),
                                secondaryText = ".txt"
                            ) {
                                Signals.suppressLockOnExit = true
                                createDocumentForPlain.launch("plain.txt")
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
                                showPassphraseScreen = true
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
            }
        }

        StartButtonHeader(title = stringResource(R.string.screen_settings_management_export_to_external)) {
            if (isLoading) return@StartButtonHeader

            if (showPassphraseScreen) {
                showPassphraseScreen = false
            } else {
                navController.popBackStack()
            }
        }
    }
}