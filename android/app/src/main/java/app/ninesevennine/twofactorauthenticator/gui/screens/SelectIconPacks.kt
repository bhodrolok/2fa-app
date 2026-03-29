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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.IconPacks
import app.ninesevennine.twofactorauthenticator.features.IconPacksStyle
import app.ninesevennine.twofactorauthenticator.features.Signals
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
                modifier = Modifier.widthIn(max = 512.dp)
            ) {
                Spacer(modifier = Modifier.height(topInset + 64.dp))

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.screen_select_icon_packs_available)
                ) {
                    availableIconPacks.forEach { pack ->
                        SectionButton(
                            painter = painterResource(R.drawable.mi_extension),
                            tint = Theme.colors.primary,
                            primaryText = pack.primary,
                            secondaryText = pack.secondary.takeIf { it.isNotBlank() },
                            enabled = Config.activeIconPackId == pack.id
                        ) {
                            Config.activeIconPackId = if (Config.activeIconPackId == pack.id) "" else pack.id

                            if (Config.activeIconPackId.isEmpty()) {
                                Logger.i(TAG, "active icon pack id set to ${Config.activeIconPackId}")
                            } else {
                                Logger.i(TAG, "active icon pack id set to none")
                            }

                            IconPacks.load(context, Config.activeIconPackId)
                            Config.save(context)
                        }
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_add),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_select_icon_packs_add),
                        secondaryText = ".zip"
                    ) {
                        Signals.suppressLockOnExit = true
                        openDocumentLauncher.launch(arrayOf("application/zip"))
                    }
                }

                RoundedButton(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    text = stringResource(R.string.screen_select_icon_packs_delete_active)
                ) {
                    IconPacks.delete(context, Config.activeIconPackId)
                    availableIconPacks = IconPacks.getAvailable(context)
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.screen_select_icon_packs_style)
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.mi_texture),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.icon_packs_style_textured),
                        enabled = Config.UsingIconPacksStyle == IconPacksStyle.TEXTURED.value
                    ) {
                        Logger.i(TAG, "icon pack style set to textured")

                        Config.UsingIconPacksStyle = IconPacksStyle.TEXTURED.value
                        IconPacks.load(context, Config.activeIconPackId)
                        Config.save(context)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_colors),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.icon_packs_style_tinted),
                        enabled = Config.UsingIconPacksStyle == IconPacksStyle.TINTED.value
                    ) {
                        Logger.i(TAG, "icon pack style set to tinted")

                        Config.UsingIconPacksStyle = IconPacksStyle.TINTED.value
                        IconPacks.load(context, Config.activeIconPackId)
                        Config.save(context)
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.screen_select_icon_packs_other)
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.mi_format_color_reset),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_select_icon_packs_other_dont_suggest_colors),
                        secondaryText = stringResource(R.string.screen_select_icon_packs_other_dont_suggest_colors_secondary),
                        enabled = Config.isUsingIconPacksDontSuggestColors
                    ) {
                        Config.isUsingIconPacksDontSuggestColors = !Config.isUsingIconPacksDontSuggestColors

                        Logger.i(TAG, "dont suggest colors set to ${Config.isUsingIconPacksDontSuggestColors}")

                        Config.save(context)
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }
        }

        LeftButtonHeader(
            title = stringResource(R.string.screen_settings_appearance_icon_packs),
            onBack = {
                navController.popBackStack()
            }
        )
    }
}