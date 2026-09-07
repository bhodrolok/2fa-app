package app.ninesevennine.twofactorauthenticator.ui.screens

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.KeyStoreAES
import app.ninesevennine.twofactorauthenticator.features.Locale
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.features.management.ImportResult
import app.ninesevennine.twofactorauthenticator.features.setScreenSecure
import app.ninesevennine.twofactorauthenticator.rustInterface
import app.ninesevennine.twofactorauthenticator.ui.elements.InfoPrompt
import app.ninesevennine.twofactorauthenticator.ui.elements.SectionButton
import app.ninesevennine.twofactorauthenticator.ui.elements.SectionGroup
import app.ninesevennine.twofactorauthenticator.ui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.utils.Signals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
object SettingsRoute

@Composable
fun Settings(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    val currentThemeOption by remember { mutableStateOf(Theme.currentOption(context)) }
    val iconPackSlot by remember { mutableIntStateOf(rustInterface.features_icons_slot_get()) }
    val iconPackName by remember { mutableStateOf(rustInterface.features_icons_name_get()) }
    val hasPassphraseUnlock by remember { mutableStateOf(rustInterface.client_has_salt_and_key()) }
    val hasBiometricUnlock by remember { mutableStateOf(KeyStoreAES.hasBiometricKey()) }

    var importChoiceNative by remember { mutableStateOf(false) }
    var importResult by remember { mutableIntStateOf(0) }
    var showImportResultPrompt by remember { mutableStateOf(false) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        scope.launch(Dispatchers.IO) {
            val bytes = try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("openInputStream returned null for $uri")
                input.use { it.readBytes() }
            } catch (e: Exception) {
                Log.e("Settings", "openDocumentLauncher", e)
                return@launch
            }

            val result = if (importChoiceNative) {
                rustInterface.features_management_native_import_into_hostages(bytes, "")
            } else {
                rustInterface.features_management_aegis_import_into_hostages(bytes, "")
            }

            bytes.fill(0)

            if (result >= ImportResult.SUCCESS.value) {
                withContext(Dispatchers.Main) {
                    Signals.checkHostages = true
                    navController.popBackStack()
                }
            } else {
                if (result == ImportResult.INCORRECT_PASSPHRASE.value) {
                    withContext(Dispatchers.Main) {
                        if (importChoiceNative) {
                            navController.navigate(ImportEncryptedRoute(uri.toString()))
                        } else {
                            navController.navigate(ImportFromAegisEncryptedRoute(uri.toString()))
                        }
                    }

                    return@launch
                }

                withContext(Dispatchers.Main) {
                    importResult = result
                    showImportResultPrompt = true
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
                    title = stringResource(R.string.screen_settings_appearance)
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.mi_language),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_appearance_language),
                        secondaryText = Locale.currentEffectiveLocale(context).displayName

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
                        primaryText = stringResource(R.string.screen_settings_appearance_theme),
                        secondaryText = when (currentThemeOption) {
                            ThemeOption.LIGHT -> stringResource(R.string.theme_option_light)
                            ThemeOption.DARK -> stringResource(R.string.theme_option_dark)
                            ThemeOption.PITCH_BLACK -> stringResource(R.string.theme_option_pitch_black)
                            else -> ""
                        }
                    ) {
                        navController.navigate(SelectThemeRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_cards_stack),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_appearance_card_style),
                        secondaryText = when (Config.UsingOtpCardStyle) {
                            OtpCardStyle.NORMAL.value -> stringResource(R.string.otp_card_style_normal)
                            OtpCardStyle.COMPACT.value -> stringResource(R.string.otp_card_style_compact)
                            OtpCardStyle.GRID.value -> stringResource(R.string.otp_card_style_grid)
                            else -> ""
                        }
                    ) {
                        navController.navigate(SelectOtpCardStyleRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_package_2),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_appearance_icon_packs),
                        secondaryText = if (Config.isUsingIconPacks) {
                            if (iconPackSlot == 0) {
                                stringResource(R.string.common_title_default)
                            } else {
                                iconPackName
                            }
                        } else {
                            null
                        }
                    ) {
                        navController.navigate(SelectIconPacksRoute)
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.screen_settings_behavior)
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.mi_touch_app),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_behavior_tap_to_reveal_codes),
                        enabled = Config.isUsingTapToRevealCodes
                    ) {
                        Config.isUsingTapToRevealCodes = !Config.isUsingTapToRevealCodes
                        Config.save(context)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_file_copy),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_behavior_double_tap_to_copy_codes),
                        enabled = Config.isUsingDoubleTapToCopyCodes
                    ) {
                        Config.isUsingDoubleTapToCopyCodes = !Config.isUsingDoubleTapToCopyCodes
                        Config.save(context)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_frame_inspect),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_behavior_focus_search_on_launch),
                        enabled = Config.isUsingFocusSearchOnLaunch
                    ) {
                        Config.isUsingFocusSearchOnLaunch = !Config.isUsingFocusSearchOnLaunch
                        Config.save(context)
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.screen_settings_security)
                ) {
                    if (hasPassphraseUnlock) {
                        SectionButton(
                            painter = painterResource(R.drawable.random_alphabet),
                            tint = Theme.colors.primary,
                            primaryText = stringResource(R.string.common_title_change_passphrase)
                        ) {
                            navController.navigate(ChangePassphraseRoute)
                        }
                    } else {
                        SectionButton(
                            painter = painterResource(R.drawable.random_alphabet),
                            tint = Theme.colors.primary,
                            primaryText = stringResource(R.string.common_title_set_passphrase)
                        ) {
                            navController.navigate(SetPassphraseRoute)
                        }
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_fingerprint),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.common_title_biometric_unlock),
                        enabled = hasBiometricUnlock
                    ) {
                        navController.navigate(BiometricUnlockRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_mobile_lock_portrait),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_security_screen_security),
                        secondaryText = stringResource(R.string.screen_settings_security_screen_security_secondary),
                        enabled = Config.isUsingScreenSecurity
                    ) {
                        Config.isUsingScreenSecurity = !Config.isUsingScreenSecurity

                        (context as? Activity)?.let { activity ->
                            setScreenSecure(activity, Config.isUsingScreenSecurity)
                        }

                        Config.save(context)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_visibility_lock),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_security_secure_clipboard),
                        secondaryText = stringResource(R.string.screen_settings_security_secure_clipboard_secondary),
                        enabled = Config.isUsingSecureClipboard
                    ) {
                        Config.isUsingSecureClipboard = !Config.isUsingSecureClipboard
                        Config.save(context)
                    }

                    if (hasPassphraseUnlock) {
                        SectionButton(
                            painter = painterResource(R.drawable.mi_lock),
                            tint = Theme.colors.primary,
                            primaryText = stringResource(R.string.screen_settings_security_lock_on_exit),
                            secondaryText = stringResource(R.string.screen_settings_security_lock_on_exit_secondary),
                            enabled = Config.isUsingLockOnExit
                        ) {
                            Config.isUsingLockOnExit = !Config.isUsingLockOnExit
                            Config.save(context)
                        }
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_block),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_security_block_intents),
                        secondaryText = stringResource(R.string.screen_settings_security_block_intents_secondary),
                        enabled = Config.isUsingBlockIntents
                    ) {
                        Config.isUsingBlockIntents = !Config.isUsingBlockIntents
                        Config.save(context)
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.screen_settings_management)
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.mi_upload),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.common_title_export),
                        secondaryText = ".2fa"
                    ) {
                        navController.navigate(ExportRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_download),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.common_title_import),
                        secondaryText = ".2fa"
                    ) {
                        importChoiceNative = true
                        Signals.suppressLockOnExit = true
                        openDocumentLauncher.launch(arrayOf(
                            "application/json",
                            "text/json",
                            "application/octet-stream"
                        ))
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.aegis),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_management_export_to_aegis),
                        secondaryText = ".json"
                    ) {
                        navController.navigate(ExportToAegisRoute)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.aegis),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_management_import_from_aegis),
                        secondaryText = ".json"
                    ) {
                        importChoiceNative = false
                        Signals.suppressLockOnExit = true
                        openDocumentLauncher.launch(arrayOf(
                            "application/json",
                            "text/json",
                            "application/octet-stream"
                        ))
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.screen_settings_support)
                ) {
                    SectionButton(
                        painter = painterResource(R.drawable.codeberg),
                        tint = if (Theme.themeOption == ThemeOption.PITCH_BLACK) Theme.colors.primary else Theme.colors.strong,
                        primaryText = stringResource(R.string.screen_settings_support_codeberg_primary),
                        secondaryText = stringResource(R.string.screen_settings_support_codeberg_secondary),
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://codeberg.org/979/2fa".toUri())
                        context.startActivity(intent)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_translate),
                        tint = Theme.colors.primary,
                        primaryText = stringResource(R.string.screen_settings_support_translations_primary),
                        secondaryText = stringResource(R.string.screen_settings_support_translations_secondary)
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://translate.codeberg.org/projects/979/2fa/".toUri())
                        context.startActivity(intent)
                    }

                    SectionButton(
                        painter = painterResource(R.drawable.mi_favorite),
                        tint = if (Theme.themeOption == ThemeOption.PITCH_BLACK) Theme.colors.primary else Theme.colors.veryWeak,
                        primaryText = stringResource(R.string.screen_settings_support_donate),
                        secondaryText = stringResource(R.string.screen_settings_support_donate_secondary)
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://ko-fi.com/ninesevennine".toUri())
                        context.startActivity(intent)
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }
        }

        StartButtonHeader(title = stringResource(R.string.screen_settings_title)) {
            navController.popBackStack()
        }
    }

    InfoPrompt(
        visible = showImportResultPrompt,
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
        onDismiss = { showImportResultPrompt = false }
    )
}