package app.ninesevennine.twofactorauthenticator.gui.screens

import android.app.Activity
import android.content.Intent
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.Locale
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.features.setScreenSecure
import app.ninesevennine.twofactorauthenticator.gui.elements.LeftButtonHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionGroup
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.serialization.Serializable

@Serializable
object SettingsRoute

private const val TAG = "Settings"

@Composable
fun Settings(navController: NavController) {
    val context = LocalContext.current

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.statusBars.asPaddingValues().calculateBottomPadding()

    val currentLanguageDisplayName by remember { mutableStateOf(Locale.currentDisplayName(context)) }
    val currentThemeOption by remember { mutableStateOf(Theme.currentOption(context)) }

    val internalAppLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val logContent = Logger.getFullLog()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(logContent.toByteArray())
                }
            } catch (e: Exception) {
                Logger.e("internalAppLogLauncher", "error saving log file ${e.message}")
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
                    title = stringResource(R.string.settings_appearance)
                ) {
                    SectionButton(
                        imageVector = Icons.Default.Language,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_appearance_language),
                        secondaryText = currentLanguageDisplayName
                    ) {
                        navController.navigate(SelectLanguageRoute)
                    }

                    SectionButton(
                        imageVector = if (Theme.themeOption == ThemeOption.PITCH_BLACK) {
                            Icons.Default.Contrast
                        } else {
                            when (Theme.currentOption(context)) {
                                ThemeOption.LIGHT -> Icons.Default.LightMode
                                else -> Icons.Default.DarkMode
                            }
                        },
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_appearance_theme),
                        secondaryText = when (currentThemeOption) {
                            ThemeOption.LIGHT -> stringResource(R.string.theme_light)
                            ThemeOption.DARK -> stringResource(R.string.theme_dark)
                            ThemeOption.PITCH_BLACK -> stringResource(R.string.theme_pitch_black)
                            else -> ""
                        }
                    ) {
                        navController.navigate(SelectThemeRoute)
                    }

                    SectionButton(
                        imageVector = Icons.Default.ViewAgenda,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_appearance_minimalistic_tiles),
                        enabled = Config.isUsingMinimalisticTiles
                    ) {
                        Config.isUsingMinimalisticTiles = !Config.isUsingMinimalisticTiles

                        Logger.i(TAG, "minimalistic tiles set to ${Config.isUsingMinimalisticTiles}")

                        Config.save(context)
                    }

                    SectionButton(
                        imageVector = Icons.Default.VerticalAlignTop,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_appearance_search_on_top),
                        enabled = Config.isUsingSearchOnTop
                    ) {
                        Config.isUsingSearchOnTop = !Config.isUsingSearchOnTop

                        Logger.i(TAG, "search on top set to ${Config.isUsingSearchOnTop}")

                        Config.save(context)
                    }
                }

                SectionGroup(
                    title = stringResource(R.string.settings_behavior)
                ) {
                    SectionButton(
                        imageVector = Icons.Default.TouchApp,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_behavior_tap_to_reveal_codes),
                        enabled = Config.isUsingTapToRevealCodes
                    ) {
                        Config.isUsingTapToRevealCodes = !Config.isUsingTapToRevealCodes

                        Logger.i(TAG, "tap to reveal codes set to ${Config.isUsingTapToRevealCodes}")

                        Config.save(context)
                    }

                    SectionButton(
                        imageVector = Icons.Default.FileCopy,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_behavior_double_tap_to_copy_codes),
                        enabled = Config.isUsingDoubleTapToCopyCodes
                    ) {
                        Config.isUsingDoubleTapToCopyCodes = !Config.isUsingDoubleTapToCopyCodes

                        Logger.i(TAG, "double tap to copy codes set to ${Config.isUsingDoubleTapToCopyCodes}")

                        Config.save(context)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.focus_search),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_behavior_focus_search_on_launch),
                        enabled = Config.isUsingFocusSearch
                    ) {
                        Config.isUsingFocusSearch = !Config.isUsingFocusSearch

                        Logger.i(TAG, "focus search set to ${Config.isUsingFocusSearch}")

                        Config.save(context)
                    }
                }

                SectionGroup(
                    title = stringResource(R.string.settings_security)
                ) {
                    SectionButton(
                        imageVector = Icons.Default.Fingerprint,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_security_biometric_unlock),
                        enabled = Config.isUsingBiometrics
                    ) {
                        navController.navigate(BiometricUnlockRoute)
                    }

                    SectionButton(
                        imageVector = Icons.Default.ScreenLockPortrait,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_security_screen_security),
                        secondaryText = stringResource(R.string.settings_security_block_screenshots_and_recordings),
                        enabled = Config.isUsingScreenSecurity
                    ) {
                        Config.isUsingScreenSecurity = !Config.isUsingScreenSecurity

                        Logger.i(TAG, "screen security set to ${Config.isUsingScreenSecurity}")

                        (context as? Activity)?.let { activity ->
                            setScreenSecure(activity, Config.isUsingScreenSecurity)
                        }

                        Config.save(context)
                    }

                    SectionButton(
                        imageVector = Icons.Default.Block,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_security_block_intents_primary),
                        secondaryText = stringResource(R.string.settings_security_block_intents_secondary),
                        enabled = Config.isUsingBlockIntents
                    ) {
                        Config.isUsingBlockIntents = !Config.isUsingBlockIntents

                        Logger.i(TAG, "block intents set to $!Config.isUsingBlockIntents}")

                        Config.save(context)
                    }
                }

                SectionGroup(
                    title = stringResource(R.string.settings_account_management)
                ) {
                    SectionButton(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_account_management_automatic_internal_backups),
                        enabled = Config.isUsingAutomaticInternalBackups
                    ) {
                        navController.navigate(AutomaticInternalBackupsRoute)
                    }

                    SectionButton(
                        imageVector = Icons.Default.Upload,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.common_export),
                        secondaryText = ".2fa"
                    ) {
                        navController.navigate(ExportRoute)
                    }

                    SectionButton(
                        imageVector = Icons.Default.Download,
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.common_import),
                        secondaryText = ".2fa"
                    ) {
                        navController.navigate(ImportRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.aegis),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_account_management_export_to_aegis),
                        secondaryText = ".json"
                    ) {
                        navController.navigate(ExportAegisRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.aegis),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.settings_account_management_import_from_aegis),
                        secondaryText = ".json"
                    ) {
                        navController.navigate(ImportAegisRoute)
                    }
                }

                SectionGroup(
                    title = stringResource(R.string.settings_support)
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.codeberg),
                        primaryText = "Codeberg",
                        secondaryText = stringResource(R.string.settings_support_view_2fa_source_code),
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://codeberg.org/979/2fa".toUri())
                        context.startActivity(intent)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.donate),
                        primaryText = stringResource(R.string.settings_support_donate),
                        secondaryText = stringResource(R.string.settings_support_donate_message)
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://979.st/en/donate/".toUri())
                        context.startActivity(intent)
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    RoundedButton(stringResource(R.string.common_download_app_log)) {
                        Logger.i(TAG, "downloading app log")
                        internalAppLogLauncher.launch("app.log")
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 192.dp))
            }
        }

        LeftButtonHeader(
            title = stringResource(R.string.settings),
            onBack = {
                navController.popBackStack()
            }
        )
    }
}