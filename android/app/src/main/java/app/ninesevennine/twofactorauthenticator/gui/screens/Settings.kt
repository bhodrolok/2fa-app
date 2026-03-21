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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.BuildConfig
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.IconPacks
import app.ninesevennine.twofactorauthenticator.features.Locale
import app.ninesevennine.twofactorauthenticator.features.Signals
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.features.setScreenSecure
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonClean
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionGroup
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.serialization.Serializable

@Serializable
object SettingsRoute

private const val TAG = "Settings"

@Composable
fun Settings(navController: NavController) {
    val context = LocalContext.current

    var versionNameTapsCount by remember { mutableIntStateOf(0) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)

    val currentLanguageDisplayName by remember { mutableStateOf(Locale.currentDisplayName()) }
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
                modifier = Modifier.widthIn(max = 512.dp)
            ) {
                Spacer(modifier = Modifier.height(topInset + 64.dp))

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = "Appearance"
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.mi_language),
                        tint = Theme.colors.primary,
                        primaryText = "Language",
                        secondaryText = currentLanguageDisplayName
                    ) {
                        navController.navigate(SelectLanguageRoute)
                    }

                    SectionButton(
                        painter = if (Theme.themeOption == ThemeOption.PITCH_BLACK) {
                            painterResource(R.drawable.mi_contrast)
                        } else {
                            when (Theme.currentOption(context)) {
                                ThemeOption.LIGHT -> painterResource(R.drawable.mi_light_mode)
                                else -> painterResource(R.drawable.mi_dark_mode)
                            }
                        },
                        tint = Theme.colors.primary,
                        primaryText = "Theme",
                        secondaryText = when (currentThemeOption) {
                            ThemeOption.LIGHT -> "Light"
                            ThemeOption.DARK -> "Dark"
                            ThemeOption.PITCH_BLACK -> "Pitch Black"
                            else -> ""
                        }
                    ) {
                        navController.navigate(SelectThemeRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_cards_stack),
                        tint = Theme.colors.primary,
                        primaryText = "Card Style",
                        secondaryText = when (Config.UsingOtpCardStyle) {
                            OtpCardStyle.NORMAL.value -> "Normal"
                            OtpCardStyle.COMPACT.value -> "Compact"
                            OtpCardStyle.GRID.value -> "Grid"
                            else -> ""
                        }
                    ) {
                        navController.navigate(SelectOtpCardStyleRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_package),
                        tint = Theme.colors.primary,
                        primaryText = "Icon Packs",
                        secondaryText = IconPacks.getActivePrimary(context)
                    ) {
                        navController.navigate(SelectIconPacksRoute)
                    }


                    SectionButton(
                        painter = painterResource(R.drawable.mi_vertical_align_top),
                        tint = Theme.colors.primary,
                        primaryText = "Search on Top",
                        enabled = Config.isUsingSearchOnTop
                    ) {
                        Config.isUsingSearchOnTop = !Config.isUsingSearchOnTop

                        Logger.i(TAG, "search on top set to ${Config.isUsingSearchOnTop}")

                        Config.save(context)
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = "Behavior"
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.mi_touch_app),
                        tint = Theme.colors.primary,
                        primaryText = "Tap to Reveal Codes",
                        enabled = Config.isUsingTapToRevealCodes
                    ) {
                        Config.isUsingTapToRevealCodes = !Config.isUsingTapToRevealCodes

                        Logger.i(TAG, "tap to reveal codes set to ${Config.isUsingTapToRevealCodes}")

                        Config.save(context)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_file_copy),
                        tint = Theme.colors.primary,
                        primaryText = "Double Tap to Copy Codes",
                        enabled = Config.isUsingDoubleTapToCopyCodes
                    ) {
                        Config.isUsingDoubleTapToCopyCodes = !Config.isUsingDoubleTapToCopyCodes

                        Logger.i(TAG, "double tap to copy codes set to ${Config.isUsingDoubleTapToCopyCodes}")

                        Config.save(context)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_frame_inspect),
                        tint = Theme.colors.primary,
                        primaryText = "Focus Search on Launch",
                        enabled = Config.isUsingFocusSearch
                    ) {
                        Config.isUsingFocusSearch = !Config.isUsingFocusSearch

                        Logger.i(TAG, "focus search set to ${Config.isUsingFocusSearch}")

                        Config.save(context)
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = "Security"
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.mi_fingerprint),
                        tint = Theme.colors.primary,
                        primaryText = "Biometric Unlock",
                        enabled = Config.isUsingBiometrics
                    ) {
                        navController.navigate(BiometricUnlockRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_mobile_lock_portrait),
                        tint = Theme.colors.primary,
                        primaryText = "Screen Security",
                        secondaryText = "Block screenshots and recordings",
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
                        painter = painterResource(R.drawable.mi_visibility_lock),
                        tint = Theme.colors.primary,
                        primaryText = "Secure Clipboard",
                        secondaryText = "Hide copied codes from suggestions and previews",
                        enabled = Config.isUsingSecureClipboard
                    ) {
                        Config.isUsingSecureClipboard = !Config.isUsingSecureClipboard

                        Logger.i(TAG, "secure clipboard set to ${!Config.isUsingSecureClipboard}")

                        Config.save(context)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_cognition),
                        tint = Theme.colors.primary,
                        primaryText = "Memory Loss",
                        secondaryText = "Wipe sensitive memory when exiting",
                        enabled = Config.isUsingMemoryLoss
                    ) {
                        Config.isUsingMemoryLoss = !Config.isUsingMemoryLoss

                        Logger.i(TAG, "memory loss set to ${!Config.isUsingMemoryLoss}")

                        Config.save(context)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_block),
                        tint = Theme.colors.primary,
                        primaryText = "Block Intents",
                        secondaryText = "Ignore actions invoked by external apps",
                        enabled = Config.isUsingBlockIntents
                    ) {
                        Config.isUsingBlockIntents = !Config.isUsingBlockIntents

                        Logger.i(TAG, "block intents set to ${!Config.isUsingBlockIntents}")

                        Config.save(context)
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = "Account Management"
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.mi_settings_backup_restore),
                        tint = Theme.colors.primary,
                        primaryText = "Automatic Backups",
                        enabled = Config.isUsingAutomaticBackups
                    ) {
                        navController.navigate(AutomaticBackupsRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_upload),
                        tint = Theme.colors.primary,
                        primaryText = "Export",
                        secondaryText = ".2fa"
                    ) {
                        navController.navigate(ExportRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_download),
                        tint = Theme.colors.primary,
                        primaryText = "Import",
                        secondaryText = ".2fa"
                    ) {
                        navController.navigate(ImportRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.aegis),
                        tint = Theme.colors.primary,
                        primaryText = "Export to Aegis",
                        secondaryText = ".json"
                    ) {
                        navController.navigate(ExportAegisRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.aegis),
                        tint = Theme.colors.primary,
                        primaryText = "Import from Aegis",
                        secondaryText = ".json"
                    ) {
                        navController.navigate(ImportAegisRoute)
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = "Support"
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.codeberg),
                        tint = if (Theme.themeOption == ThemeOption.PITCH_BLACK) Theme.colors.primary else Theme.colors.strong,
                        primaryText = "Codeberg",
                        secondaryText = "View 2fa's source code",
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://codeberg.org/979/2fa".toUri())
                        context.startActivity(intent)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_favorite),
                        tint = if (Theme.themeOption == ThemeOption.PITCH_BLACK) Theme.colors.primary else Theme.colors.veryWeak,
                        primaryText = "Donate",
                        secondaryText = "979 needs your support to continue"
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://979.st/donate/".toUri())
                        context.startActivity(intent)
                    }
                }

                RoundedButtonClean(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    text = BuildConfig.VERSION_NAME
                ) {
                    if (versionNameTapsCount < 5) {
                        versionNameTapsCount++
                        return@RoundedButtonClean
                    }

                    Signals.suppressMemoryLoss = true
                    Logger.i(TAG, "downloading app log")
                    internalAppLogLauncher.launch("app.log")
                }
            }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }

        StartButtonHeader(title = "Settings") {
            navController.popBackStack()
        }
    }
}