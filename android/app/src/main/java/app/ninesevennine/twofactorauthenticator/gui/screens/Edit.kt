package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.AccountTruncated
import app.ninesevennine.twofactorauthenticator.Algorithm
import app.ninesevennine.twofactorauthenticator.CodeType
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.IconPacks
import app.ninesevennine.twofactorauthenticator.gui.elements.EditHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.OtpTileNormal
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionDropdownSingleChoice
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionDualButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionGroup
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionHueSlider
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionSaturationSlider
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionTextBox
import app.ninesevennine.twofactorauthenticator.utils.Clipboard
import app.ninesevennine.twofactorauthenticator.utils.ColorPicker
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

    val account = remember { Entry.rustInterface.getEditAccountWithSecretBase32NoPad()!! }

    var name by remember { mutableStateOf(account.name) }
    var issuer by remember { mutableStateOf(account.issuer) }
    var note by remember { mutableStateOf(account.note) }
    var secretBase32NoPad by remember { mutableStateOf(account.secretBase32NoPad) }
    var codeType by remember { mutableStateOf(account.codeType) }
    var algorithm by remember { mutableStateOf(account.algorithm) }
    var counter by remember { mutableStateOf(account.counter.toString()) }
    var period by remember { mutableStateOf(account.period.toString()) }
    var digits by remember { mutableStateOf(account.digits.toString()) }
    var color by remember { mutableStateOf(Color(account.color)) }

    if (color == Color.Black) {
        color = if (Config.isUsingIconPacksDontSuggestColors) {
            Color.White
        } else {
            IconPacks.get(context, account.issuer)?.color ?: Color.White
        }

        Entry.rustInterface.setEditAccountColor(color.toArgb())
    }

    var secretError by remember { mutableStateOf(secretBase32NoPad.isEmpty()) }
    var counterError by remember { mutableStateOf(account.counter < 0) }
    var periodError by remember { mutableStateOf(account.period < 10) }
    var digitsError by remember { mutableStateOf(account.digits !in 4..10) }

    var updateCount by remember { mutableIntStateOf(0) }
    var deleteButtonTapsCount by remember { mutableIntStateOf(0) }

    var showColorPicker by remember { mutableStateOf(false) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)

    Time.Track()

    BackHandler {
        Entry.rustInterface.setEditAccountNone()
        navController.popBackStack(MainRoute, false)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 512.dp)) {
                Spacer(modifier = Modifier.height(topInset + 80.dp))

                // using OtpTileMinimalistic looks terrible, just don't.
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
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
                            color.toArgb()
                        ),
                        updateCount,
                        {
                            counter = it.toString()
                        },
                        {
                            showColorPicker = !showColorPicker
                        },
                        true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = showColorPicker,
                    enter = expandVertically(
                        animationSpec = tween(200),
                        expandFrom = Alignment.Top
                    )
                            + scaleIn(animationSpec = tween(200), initialScale = 0.9f)
                            + fadeIn(animationSpec = tween(200)),
                    exit = shrinkVertically(
                        animationSpec = tween(200),
                        shrinkTowards = Alignment.Top
                    )
                            + scaleOut(animationSpec = tween(200), targetScale = 0.9f)
                            + fadeOut(animationSpec = tween(200))
                ) {
                    SectionGroup(
                        modifier = Modifier.padding(16.dp),
                        title = "Color Picker"
                    ) {
                        Spacer(Modifier.height(8.dp))

                        SectionHueSlider(color) { hue ->
                            val oldHsl = ColorPicker.colorToHsl(color)
                            color = ColorPicker.hslToColor(hue, oldHsl[1], 0.5f)
                            Entry.rustInterface.setEditAccountColor(color.toArgb())
                        }

                        SectionSaturationSlider(color) { saturation ->
                            val oldHsv = ColorPicker.colorToHsl(color)
                            color = ColorPicker.hslToColor(oldHsv[0], saturation, 0.5f)
                            Entry.rustInterface.setEditAccountColor(color.toArgb())
                        }

                        SectionDualButton(
                            leftText = "Copy",
                            rightText = "Paste",
                            onLeft = {
                                Clipboard.copy(context, "color", ColorPicker.toRgbHexString(color), false)
                            },
                            onRight = {
                                val text = Clipboard.paste(context) ?: return@SectionDualButton
                                val parsed = ColorPicker.fromHexString(text) ?: return@SectionDualButton
                                val hsl = ColorPicker.colorToHsl(parsed)
                                color = ColorPicker.hslToColor(hsl[0], hsl[1], 0.5f).copy(alpha = 1f)
                                Entry.rustInterface.setEditAccountColor(color.toArgb())
                            }
                        )

                        Spacer(Modifier.height(10.dp))
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = "Account Information"
                ) {
                    Spacer(Modifier.height(8.dp))

                    SectionTextBox(
                        title = "Name",
                        value = name,
                        onValueChange = {
                            Entry.rustInterface.setEditAccountName(it.trim())
                            name = it
                        }
                    )

                    SectionTextBox(
                        title = "Issuer",
                        value = issuer,
                        onValueChange = {
                            Entry.rustInterface.setEditAccountIssuer(it.trim())
                            issuer = it
                        }
                    )

                    SectionTextBox(
                        title = stringResource(R.string.edit_account_information_note),
                        value = note,
                        onValueChange = {
                            Entry.rustInterface.setEditAccountNote(it.trim())
                            note = it
                        }
                    )

                    Spacer(Modifier.height(10.dp))
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = "Advanced Settings"
                ) {
                    Spacer(Modifier.height(8.dp))

                    SectionConfidentialTextBox(
                        title = "Secret",
                        value = secretBase32NoPad,
                        onValueChange = {
                            val trimmed = it.trim()
                            secretBase32NoPad = trimmed

                            secretError = !Entry.rustInterface.setEditAccountSecretBase32NoPad(trimmed)
                            if (!secretError) updateCount++
                        },
                        placeholder = "Base32",
                        error = secretError
                    )

                    SectionDropdownSingleChoice(
                        title = "Type",
                        options = CodeType.entries.toTypedArray(),
                        selectedOption = codeType,
                        onOptionSelected = {
                            Entry.rustInterface.setEditAccountCodeType(it)
                            codeType = it

                            if (it == CodeType.STEAM) {
                                period = "30"
                                digits = "5"

                                Entry.rustInterface.setEditAccountPeriod(30)
                                Entry.rustInterface.setEditAccountDigits(5)

                                periodError = false
                                digitsError = false
                            }

                            updateCount++
                        },
                        label = { it.displayName }
                    )

                    if (codeType != CodeType.STEAM) {
                        SectionDropdownSingleChoice(
                            title = "Algorithm",
                            options = Algorithm.entries.toTypedArray(),
                            selectedOption = algorithm,
                            onOptionSelected = {
                                Entry.rustInterface.setEditAccountAlgorithm(it)
                                algorithm = it
                                updateCount++
                            },
                            label = { it.displayName }
                        )

                        if (codeType == CodeType.HOTP) {
                            SectionTextBox(
                                title = "Counter",
                                value = counter,
                                onValueChange = {
                                    val trimmed = it.trim()
                                    counter = trimmed

                                    counterError = !trimmed.matches(Regex("^0$|^[1-9]\\d*$"))
                                    if (!counterError) {
                                        Entry.rustInterface.setEditAccountCounter(trimmed.toLong())
                                        updateCount++
                                    }
                                },
                                placeholder = "0+",
                                numberField = true,
                                counterError
                            )
                        } else {
                            SectionTextBox(
                                title = "Period",
                                value = period,
                                onValueChange = {
                                    val trimmed = it.trim()
                                    period = trimmed

                                    periodError = !trimmed.matches(Regex("^[1-9]\\d+$"))
                                    if (!periodError) {
                                        Entry.rustInterface.setEditAccountPeriod(trimmed.toLong())
                                        updateCount++
                                    }
                                },
                                placeholder = "10+",
                                numberField = true,
                                periodError
                            )
                        }

                        SectionTextBox(
                            title = "Digits",
                            value = digits,
                            onValueChange = {
                                val trimmed = it.trim()
                                digits = trimmed

                                digitsError = !trimmed.matches(Regex("^[4-9]$|^10$"))
                                if (!digitsError) {
                                    Entry.rustInterface.setEditAccountDigits(trimmed.toInt())
                                    updateCount++
                                }
                            },
                            placeholder = "4-10",
                            numberField = true,
                            digitsError
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                }

                RoundedButton(
                    modifier = Modifier.fillMaxWidth().padding(horizontal  = 16.dp),
                    text = if (deleteButtonTapsCount > 0) {
                        "Tap ${6 - deleteButtonTapsCount} more times to delete account"
                    } else {
                        "Delete Account"
                    }
                ) {
                    if (deleteButtonTapsCount < 5) {
                        deleteButtonTapsCount++
                        return@RoundedButton
                    }

                    scope.launch(Dispatchers.IO) {
                        Entry.rustInterface.deleteAccount(account.id)
                        Entry.rustInterface.setEditAccountNone()

                        AccountManagement.saveAccountsUsingSeed(context)
                        withContext(Dispatchers.Main) {
                            navController.popBackStack(MainRoute, false)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 80.dp))
            }
        }

        EditHeader(
            title = "Edit",
            onCancel = {
                Entry.rustInterface.setEditAccountNone()
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
                    Entry.rustInterface.copyEditAccountIntoAccounts()
                    Entry.rustInterface.setEditAccountNone()

                    AccountManagement.saveAccountsUsingSeed(context)

                    if (Config.isUsingAutomaticBackups) {
                        AccountManagement.saveAccountsUsingAutomaticBackups(context)
                    }

                    withContext(Dispatchers.Main) {
                        navController.popBackStack(MainRoute, false)
                    }
                }
            }
        )
    }
}