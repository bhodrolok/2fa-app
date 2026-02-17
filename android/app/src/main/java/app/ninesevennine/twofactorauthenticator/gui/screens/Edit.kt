package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.AccountTruncated
import app.ninesevennine.twofactorauthenticator.Algorithm
import app.ninesevennine.twofactorauthenticator.CodeType
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.RustInterface
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.Tints
import app.ninesevennine.twofactorauthenticator.gui.elements.EditHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.EditSectionConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.EditSectionDropdownSingleChoice
import app.ninesevennine.twofactorauthenticator.gui.elements.EditSectionGroup
import app.ninesevennine.twofactorauthenticator.gui.elements.EditSectionTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.OtpTileNormal
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonTinted
import app.ninesevennine.twofactorauthenticator.utils.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object EditRoute

@Composable
fun Edit(navController: NavController) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val scope = rememberCoroutineScope()

    val colors = if (Theme.currentOption(context) == ThemeOption.LIGHT) Tints.light else Tints.dark
    val isUsingPitchBlack by remember { mutableStateOf(Theme.themeOption == ThemeOption.PITCH_BLACK) }

    val account = remember { RustInterface().getEditAccountWithSecretBase32NoPad()!! }

    var name by remember { mutableStateOf(account.name) }
    var issuer by remember { mutableStateOf(account.issuer) }
    var note by remember { mutableStateOf(account.note) }
    var secretBase32NoPad by remember { mutableStateOf(account.secretBase32NoPad) }
    var codeType by remember { mutableStateOf(account.codeType) }
    var algorithm by remember { mutableStateOf(account.algorithm) }
    var counter by remember { mutableStateOf(account.counter.toString()) }
    var period by remember { mutableStateOf(account.period.toString()) }
    var digits by remember { mutableStateOf(account.digits.toString()) }
    val color by remember { mutableIntStateOf(account.color) }

    var secretError by remember { mutableStateOf(secretBase32NoPad.isEmpty()) }
    var counterError by remember { mutableStateOf(account.counter < 0) }
    var periodError by remember { mutableStateOf(account.period < 10) }
    var digitsError by remember { mutableStateOf(account.digits !in 4..10) }

    var updateCount by remember { mutableIntStateOf(0) }
    var deleteButtonTapsCount by remember { mutableIntStateOf(0) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.statusBars.asPaddingValues().calculateBottomPadding()

    Time.Track()

    BackHandler {
        RustInterface().setEditAccountNone()
        navController.popBackStack(MainRoute, false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isUsingPitchBlack) {
                    Modifier
                } else {
                    Modifier
                        .graphicsLayer(
                            colorFilter = ColorFilter.tint(
                                Color(color),
                                BlendMode.Multiply
                            )
                        )
                        .background(colors.background)
                }
            )
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(topInset + 86.dp))

                // using OtpTileMinimalistic looks terrible, just don't.
                OtpTileNormal(
                    navController,
                    AccountTruncated(
                        account.id,
                        name,
                        issuer,
                        codeType,
                        counter.toLongOrNull()?.coerceAtLeast(0) ?: 0,
                        period.toLongOrNull()?.coerceAtLeast(10) ?: 10,
                        digits.toIntOrNull()?.coerceIn(4, 10) ?: 6,
                        color
                    ),
                    updateCount,
                    {
                        counter = it.toString()
                    },
                    true
                )

                Spacer(modifier = Modifier.height(16.dp))

                EditSectionGroup(stringResource(R.string.edit_account_information)) {
                    Spacer(Modifier.height(10.dp))
                    EditSectionTextBox(
                        stringResource(R.string.edit_account_information_name),
                        name,
                        {
                            RustInterface().setEditAccountName(it.trim())
                            name = it
                        }
                    )
                    EditSectionTextBox(
                        stringResource(R.string.edit_account_information_issuer),
                        issuer,
                        {
                            RustInterface().setEditAccountIssuer(it.trim())
                            issuer = it
                        }
                    )
                    EditSectionTextBox(
                        stringResource(R.string.edit_account_information_note),
                        note,
                        {
                            RustInterface().setEditAccountNote(it.trim())
                            note = it
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }

                EditSectionGroup(stringResource(R.string.edit_advanced_settings)) {
                    Spacer(Modifier.height(10.dp))
                    EditSectionConfidentialTextBox(
                        stringResource(R.string.edit_advanced_settings_secret),
                        secretBase32NoPad,
                        {
                            val trimmed = it.trim()
                            secretBase32NoPad = trimmed

                            secretError = !RustInterface().setEditAccountSecretBase32NoPad(trimmed)
                            if (!secretError) updateCount++
                        },
                        "Base32",
                        false,
                        secretError
                    )
                    EditSectionDropdownSingleChoice(
                        stringResource(R.string.edit_advanced_settings_type),
                        CodeType.entries.toTypedArray(),
                        codeType,
                        {
                            RustInterface().setEditAccountCodeType(it)
                            codeType = it

                            if (it == CodeType.STEAM) {
                                period = "30"
                                digits = "5"

                                RustInterface().setEditAccountPeriod(30)
                                RustInterface().setEditAccountDigits(5)

                                periodError = false
                                digitsError = false
                            }

                            updateCount++
                        },
                        { it.displayName },
                        account.color
                    )
                    if (codeType != CodeType.STEAM) {
                        EditSectionDropdownSingleChoice(
                            stringResource(R.string.edit_advanced_settings_algorithm),
                            Algorithm.entries.toTypedArray(),
                            algorithm,
                            {
                                RustInterface().setEditAccountAlgorithm(it)
                                algorithm = it
                                updateCount++
                            },
                            { it.displayName },
                            account.color
                        )
                        if (codeType == CodeType.HOTP) {
                            EditSectionTextBox(
                                stringResource(R.string.edit_advanced_settings_counter),
                                counter,
                                {
                                    val trimmed = it.trim()
                                    counter = trimmed

                                    counterError = !trimmed.matches(Regex("^0$|^[1-9]\\d*$"))
                                    if (!counterError) {
                                        RustInterface().setEditAccountCounter(trimmed.toLong())
                                        updateCount++
                                    }
                                },
                                "0+",
                                true,
                                counterError
                            )
                        } else {
                            EditSectionTextBox(
                                stringResource(R.string.edit_advanced_settings_period),
                                period,
                                {
                                    val trimmed = it.trim()
                                    period = trimmed

                                    periodError = !trimmed.matches(Regex("^[1-9]\\d+$"))
                                    if (!periodError) {
                                        RustInterface().setEditAccountPeriod(trimmed.toLong())
                                        updateCount++
                                    }
                                },
                                "10+",
                                true,
                                periodError
                            )
                        }

                        EditSectionTextBox(
                            stringResource(R.string.edit_advanced_settings_digits),
                            digits,
                            {
                                val trimmed = it.trim()
                                digits = trimmed

                                digitsError = !trimmed.matches(Regex("^[4-9]$|^10$"))
                                if (!digitsError) {
                                    RustInterface().setEditAccountDigits(trimmed.toInt())
                                    updateCount++
                                }
                            },
                            "4-10",
                            true,
                            digitsError
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                RoundedButtonTinted(
                    if (deleteButtonTapsCount > 0)
                        pluralStringResource(R.plurals.edit_delete_account_variable, 6 - deleteButtonTapsCount, 6 - deleteButtonTapsCount)
                    else
                        stringResource(R.string.edit_delete_account)
                ) {
                    if (deleteButtonTapsCount < 5) {
                        deleteButtonTapsCount++
                        return@RoundedButtonTinted
                    }

                    scope.launch(Dispatchers.IO) {
                        RustInterface().deleteAccount(account.id)
                        RustInterface().setEditAccountNone()

                        AccountManagement.saveAccountsUsingSeed(context)
                        withContext(Dispatchers.Main) {
                            navController.popBackStack(MainRoute, false)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 192.dp))
            }
        }

        EditHeader(
            title = stringResource(R.string.edit),
            onCancel = {
                RustInterface().setEditAccountNone()
                navController.popBackStack(MainRoute, false)
            },
            onDone = {
                if (secretError || digitsError) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    return@EditHeader
                }

                if (codeType == CodeType.HOTP && counterError) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    return@EditHeader
                }

                if (codeType == CodeType.TOTP && periodError) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    return@EditHeader
                }

                scope.launch(Dispatchers.IO) {
                    RustInterface().copyEditAccountIntoAccounts()
                    RustInterface().setEditAccountNone()

                    AccountManagement.saveAccountsUsingSeed(context)

                    if (Config.isUsingAutomaticInternalBackups) {
                        AccountManagement.saveAccountsUsingAutomaticInternalBackups(context)
                    }

                    withContext(Dispatchers.Main) {
                        navController.popBackStack(MainRoute, false)
                    }
                }
            }
        )
    }
}