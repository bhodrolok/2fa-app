package app.ninesevennine.twofactorauthenticator.gui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.IconPacks
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.elements.LeftButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionGroup
import app.ninesevennine.twofactorauthenticator.utils.Document
import app.ninesevennine.twofactorauthenticator.utils.Logger
import app.ninesevennine.twofactorauthenticator.utils.Zip.unzipFromStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File
import java.security.SecureRandom

@Serializable
object SelectIconPacksRoute

private const val TAG = "SelectIconPacks"

@Composable
fun SelectIconPacks(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.statusBars.asPaddingValues().calculateBottomPadding()

    var availableIconPacks by remember { mutableStateOf(IconPacks.getAvailable(context)) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val importFilename = Document.getName(context, uri) ?: "???"

                    val randomHex = ByteArray(8).also { SecureRandom().nextBytes(it) }
                        .joinToString("") { "%02x".format(it) }

                    val extractDir = File(context.noBackupFilesDir, "icons/$randomHex")

                    val result = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        unzipFromStream(inputStream, extractDir)
                    } ?: false

                    if (result) {
                        Logger.i(TAG, "successfully extracted $importFilename")
                        availableIconPacks = IconPacks.getAvailable(context)
                    } else {
                        Logger.i(TAG, "failed to extract zip")

                        return@launch
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "failed to extract zip", e)
                }
            }
        }
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
                modifier = Modifier.widthIn(max = 500.dp)
            ) {
                Spacer(modifier = Modifier.height(topInset + 68.dp))

                SectionGroup(
                    title = stringResource(R.string.settings_icon_packs_available)
                ) {
                    availableIconPacks.forEach { pack ->
                        SectionButton(
                            imageVector = Icons.Default.Extension,
                            tint = Theme.colors.primary,
                            primaryText = pack.primary,
                            secondaryText = pack.secondary.takeIf { it.isNotBlank() },
                            enabled = Config.activeIconPackId == pack.id
                        ) {
                            Config.activeIconPackId = if (Config.activeIconPackId == pack.id) "" else pack.id
                            IconPacks.load(context, Config.activeIconPackId)
                            Config.save(context)
                        }
                    }

                    SectionButton(
                        imageVector = Icons.Default.Add,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_icon_packs_add),
                        secondaryText = ".zip"
                    ) {
                        openDocumentLauncher.launch(arrayOf("application/zip"))
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    RoundedButton(stringResource(R.string.settings_icon_packs_delete_current)) {
                        IconPacks.delete(context, Config.activeIconPackId)
                        availableIconPacks = IconPacks.getAvailable(context)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                SectionGroup(
                    title = stringResource(R.string.common_other)
                ) {
                    SectionButton(
                        imageVector = Icons.Default.ChangeCircle,
                        primaryText = stringResource(R.string.settings_icon_packs_override_colors_primary),
                        secondaryText = stringResource(R.string.settings_icon_packs_override_colors_secondary),
                        enabled = Config.isUsingIconPacksOverrideColors
                    ) {
                        Config.isUsingIconPacksOverrideColors = !Config.isUsingIconPacksOverrideColors
                        Config.save(context)
                    }

                    SectionButton(
                        imageVector = Icons.Default.FormatColorReset,
                        primaryText = stringResource(R.string.settings_icon_packs_dont_suggest_colors_primary),
                        secondaryText = stringResource(R.string.settings_icon_packs_dont_suggest_colors_secondary),
                        enabled = Config.isUsingIconPacksDontSuggestColors
                    ) {
                        Config.isUsingIconPacksDontSuggestColors = !Config.isUsingIconPacksDontSuggestColors
                        Config.save(context)
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 192.dp))
            }
        }

        LeftButtonHeader(
            title = stringResource(R.string.settings_icon_packs),
            onBack = {
                navController.popBackStack()
            }
        )
    }
}