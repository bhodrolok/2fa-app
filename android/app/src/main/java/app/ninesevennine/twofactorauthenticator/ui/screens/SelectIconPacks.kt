package app.ninesevennine.twofactorauthenticator.ui.screens

import android.text.format.DateUtils
import android.util.Log
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import app.ninesevennine.twofactorauthenticator.features.IconPacks
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.rustInterface
import app.ninesevennine.twofactorauthenticator.ui.elements.LeftButtonHeader
import app.ninesevennine.twofactorauthenticator.ui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.ui.elements.SectionButton
import app.ninesevennine.twofactorauthenticator.ui.elements.SectionGroup
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.utils.Signals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
object SelectIconPacksRoute

private const val TAG = "SelectIconPacks"

@Composable
fun SelectIconPacks(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    var slot by remember { mutableIntStateOf(rustInterface.features_icons_slot_get()) }
    var name by remember { mutableStateOf(rustInterface.features_icons_name_get()) }
    var timestamp by remember { mutableLongStateOf(rustInterface.features_icons_timestamp_get()) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        scope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    IconPacks.extractStreamToSlot1(context, it)
                } ?: return@launch
            } catch (e: Exception) {
                Log.e(TAG, "openDocumentLauncher", e)
                return@launch
            }

            rustInterface.features_icons_load(
                skipAndClearMap = !Config.isUsingIconPacks,
                Config.isUsingTexturedIcons
            )

            slot = rustInterface.features_icons_slot_get()
            name = rustInterface.features_icons_name_get()
            timestamp = rustInterface.features_icons_timestamp_get()

            if (slot != 1) {
                IconPacks.deleteSlot1(context)
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
                    title = stringResource(R.string.screen_select_icon_packs_style)
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.mi_texture),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.icon_packs_style_textured),
                        enabled = Config.isUsingTexturedIcons
                    ) {
                        Config.isUsingTexturedIcons = !Config.isUsingTexturedIcons
                        Config.save(context)

                        scope.launch(Dispatchers.IO) {
                            rustInterface.features_icons_load(skipAndClearMap = false, Config.isUsingTexturedIcons)
                        }
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_colors),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.icon_packs_style_tinted),
                        enabled = !Config.isUsingTexturedIcons
                    ) {
                        Config.isUsingTexturedIcons = !Config.isUsingTexturedIcons
                        Config.save(context)

                        scope.launch(Dispatchers.IO) {
                            rustInterface.features_icons_load(skipAndClearMap = false, Config.isUsingTexturedIcons)
                        }
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.screen_select_icon_packs_installed)
                ) {
                    SectionButton(
                        painter = if (slot == 0) painterResource(R.drawable.mi_blur_on) else painterResource(R.drawable.mi_extension),
                        tint = Theme.colors.primary,
                        primaryText = if (slot == 0) stringResource(R.string.common_title_default) else name,
                        secondaryText = DateUtils.formatDateTime(
                            context,
                            timestamp * 1000,
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
                        ),
                        enabled = Config.isUsingIconPacks
                    ) {
                        Config.isUsingIconPacks = !Config.isUsingIconPacks
                        Config.save(context)

                        scope.launch(Dispatchers.IO) {
                            rustInterface.features_icons_load(skipAndClearMap = !Config.isUsingIconPacks, Config.isUsingTexturedIcons)
                        }
                    }
                }

                if (slot == 0) {
                    RoundedButton(
                        modifier = Modifier.fillMaxWidth().padding(horizontal  = 16.dp),
                        text = stringResource(R.string.common_title_import)
                    ) {
                        Signals.suppressLockOnExit = true
                        openDocumentLauncher.launch(arrayOf("application/zip"))
                    }
                } else {
                    RoundedButton(
                        modifier = Modifier.fillMaxWidth().padding(horizontal  = 16.dp),
                        text = stringResource(R.string.common_title_delete)
                    ) {
                        scope.launch(Dispatchers.IO) {
                            IconPacks.deleteSlot1(context)
                            rustInterface.features_icons_load(skipAndClearMap = !Config.isUsingIconPacks, Config.isUsingTexturedIcons)
                            slot = rustInterface.features_icons_slot_get()
                            name = rustInterface.features_icons_name_get()
                            timestamp = rustInterface.features_icons_timestamp_get()
                        }
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