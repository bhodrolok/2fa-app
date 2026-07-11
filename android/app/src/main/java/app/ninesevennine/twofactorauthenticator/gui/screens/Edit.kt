package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.AccountTruncated
import app.ninesevennine.twofactorauthenticator.Algorithm
import app.ninesevennine.twofactorauthenticator.CodeType
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.IconPacks
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.EditHeader
import app.ninesevennine.twofactorauthenticator.gui.elements.OtpCardNormal
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionButtonSmall
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionConfidentialTextBox
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionDropdownSingleChoice
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionDualButtonSmall
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionGroup
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionHueSlider
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionSaturationSlider
import app.ninesevennine.twofactorauthenticator.gui.elements.SectionTextBox
import app.ninesevennine.twofactorauthenticator.utils.AccountManagement
import app.ninesevennine.twofactorauthenticator.utils.Clipboard
import app.ninesevennine.twofactorauthenticator.utils.ColorPicker
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.utils.Logger
import app.ninesevennine.twofactorauthenticator.utils.Signals
import app.ninesevennine.twofactorauthenticator.utils.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object EditRoute

private const val TAG = "Edit"

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

    val iconResult = remember(issuer) {
        IconPacks.get(context, issuer)
    }

    if (color == Color.Black) {
        color = if (Config.isUsingIconPacksDontSuggestColors) {
            Color.White
        } else {
            iconResult.color ?: Color.White
        }

        Entry.rustInterface.setEditAccountColor(color.toArgb())
    }

    var secretError by remember { mutableStateOf(secretBase32NoPad.isEmpty()) }
    var counterError by remember { mutableStateOf(account.counter < 0) }
    var periodError by remember { mutableStateOf(account.period < 10) }
    var digitsError by remember { mutableStateOf(account.digits !in 4..10) }

    var updateCount by remember { mutableIntStateOf(0) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showDeletePrompt by remember { mutableStateOf(false) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    Time.Track()

    BackHandler {
        Signals.checkEditAccountSome = false
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

                Text(
                    text = stringResource(R.string.common_button_preview),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = InterVariable
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    OtpCardNormal(
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
                        title = stringResource(R.string.screen_edit_section_color)
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

                        SectionDualButtonSmall(
                            leftText = stringResource(R.string.screen_edit_section_color_copy),
                            rightText = stringResource(R.string.screen_edit_section_color_paste),
                            onLeft = {
                                Clipboard.copy(context, "color", ColorPicker.toRgbHexString(color), false)
                            },
                            onRight = {
                                val text = Clipboard.paste(context) ?: return@SectionDualButtonSmall
                                val parsed = ColorPicker.fromHexString(text) ?: return@SectionDualButtonSmall
                                val hsl = ColorPicker.colorToHsl(parsed)
                                color = ColorPicker.hslToColor(hsl[0], hsl[1], 0.5f).copy(alpha = 1f)
                                Entry.rustInterface.setEditAccountColor(color.toArgb())
                            }
                        )

                        if (iconResult.color != null) {
                            SectionButtonSmall(
                                text = stringResource(R.string.screen_edit_section_color_preset),
                                onClick = {
                                    color = iconResult.color
                                    Entry.rustInterface.setEditAccountColor(color.toArgb())
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                    }
                }

                SectionGroup(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.screen_edit_section_details)
                ) {
                    Spacer(Modifier.height(8.dp))

                    SectionTextBox(
                        title = stringResource(R.string.screen_edit_section_details_name),
                        value = name,
                        onValueChange = {
                            Entry.rustInterface.setEditAccountName(it.trim())
                            name = it
                        }
                    )

                    SectionTextBox(
                        title = stringResource(R.string.screen_edit_section_details_issuer),
                        value = issuer,
                        onValueChange = {
                            Entry.rustInterface.setEditAccountIssuer(it.trim())
                            issuer = it
                        }
                    )

                    SectionTextBox(
                        title = stringResource(R.string.screen_edit_section_details_note),
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
                    title = stringResource(R.string.screen_edit_section_parameters)
                ) {
                    Spacer(Modifier.height(8.dp))

                    SectionConfidentialTextBox(
                        title = stringResource(R.string.screen_edit_section_parameters_secret),
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
                        title = stringResource(R.string.screen_edit_section_parameters_type),
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
                            title = stringResource(R.string.screen_edit_section_parameters_algorithm),
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
                                title = stringResource(R.string.screen_edit_section_parameters_counter),
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
                                title = stringResource(R.string.screen_edit_section_parameters_period),
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
                            title = stringResource(R.string.screen_edit_section_parameters_digits),
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
                    text = stringResource(R.string.screen_edit_delete),
                    hazardColors = true
                ) {
                    showDeletePrompt = true
                }

                Spacer(modifier = Modifier.height(bottomInset + 80.dp))
            }
        }

        EditHeader(
            title = stringResource(R.string.screen_edit_title),
            onCancel = {
                Signals.checkEditAccountSome = false
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

                    if (!AccountManagement.saveAccounts(context)) {
                        return@launch
                    }

                    if (Signals.checkEditAccountSome) {
                        Signals.checkEditAccountSome = false
                        Signals.accountAdded = true

                        Logger.i(TAG, "successfully added an account")
                    } else {
                        Logger.i(TAG, "successfully modified an account")
                    }

                    withContext(Dispatchers.Main) {
                        navController.popBackStack(MainRoute, false)
                    }
                }
            }
        )
    }

    AnimatedVisibility(
        visible = showDeletePrompt,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        DeletePrompt(
            onDelete = {
                scope.launch(Dispatchers.IO) {
                    Entry.rustInterface.deleteAccount(account.id)
                    Entry.rustInterface.setEditAccountNone()

                    if (!AccountManagement.saveAccounts(context)) {
                        return@launch
                    }

                    Logger.i(TAG, "successfully deleted an account")

                    withContext(Dispatchers.Main) {
                        navController.popBackStack(MainRoute, false)
                    }
                }
            },
            onCancel = {
                showDeletePrompt = false
            }
        )
    }
}

@Composable
fun AnimatedVisibilityScope.DeletePrompt(
    onDelete: () -> (Unit),
    onCancel: () -> (Unit)
) {
    val backgroundInteractionSource = remember { MutableInteractionSource() }

    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = backgroundInteractionSource,
                indication = null,
                onClick = onCancel
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .animateEnterExit(
                    enter = scaleIn(
                        animationSpec = tween(200),
                        initialScale = 0.9f
                    ),
                    exit = scaleOut(
                        animationSpec = tween(200),
                        targetScale = 0.9f
                    )
                )
                .widthIn(max = 512.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(Theme.colors.surface, shape)
                .border(1.dp, Theme.colors.border, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_edit_delete_title),
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 18.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W700
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.screen_edit_delete_body),
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 14.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(32.dp))

            RoundedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.screen_edit_delete),
                hazardColors = true,
                onClick = onDelete
            )

            Spacer(modifier = Modifier.height(12.dp))

            RoundedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.common_button_cancel),
                hazardColors = false,
                onClick = onCancel
            )
        }
    }
}