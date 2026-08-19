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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.AccountShort
import app.ninesevennine.twofactorauthenticator.Algorithm
import app.ninesevennine.twofactorauthenticator.CodeType
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
import app.ninesevennine.twofactorauthenticator.rustInterface
import app.ninesevennine.twofactorauthenticator.utils.Clipboard
import app.ninesevennine.twofactorauthenticator.utils.Signals
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
    val scope = rememberCoroutineScope()

    val account = remember { rustInterface.features_edit_get() }

    var name by remember { mutableStateOf(account.name) }
    var issuer by remember { mutableStateOf(account.issuer) }
    var note by remember { mutableStateOf(account.note) }
    var secret by remember { mutableStateOf(account.secret) }
    var codeType by remember { mutableStateOf(CodeType.entries.getOrElse(account.codeType) { CodeType.HOTP }) }
    var algorithm by remember { mutableStateOf(Algorithm.entries.getOrElse(account.algorithm) { Algorithm.SHA1 }) }
    var counter by remember { mutableStateOf(account.counter.toString()) }
    var period by remember { mutableStateOf(account.period.toString()) }
    var digits by remember { mutableStateOf(account.digits.toString()) }
    var color by remember { mutableIntStateOf(account.color) }

    val iconResult = remember(issuer) {
        IconPacks.get(context, issuer)
    }

    var nameError by remember { mutableStateOf(false) }
    var secretError by remember { mutableStateOf(secret.isEmpty()) }
    var counterError by remember { mutableStateOf(false) }
    var periodError by remember { mutableStateOf(false) }
    var digitsError by remember { mutableStateOf(false) }

    var update by remember { mutableIntStateOf(0) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showDeletePrompt by remember { mutableStateOf(false) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    Time.Track()

    BackHandler {
        Signals.checkEditAccountSome = false
        rustInterface.management_edit_none()
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
                        AccountShort(
                            account.id,
                            name,
                            issuer,
                            codeType.ordinal,
                            counter.toLongOrNull()?.coerceAtLeast(0) ?: 0,
                            period.toLongOrNull()?.coerceAtLeast(10) ?: 10,
                            color
                        ),
                        update,
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

                        SectionHueSlider(color) {
                            rustInterface.features_edit_color_hue_set(it)
                            color = rustInterface.features_edit_color_get()
                        }

                        SectionSaturationSlider(color) {
                            rustInterface.features_edit_color_saturation_set(it)
                            color = rustInterface.features_edit_color_get()
                        }

                        SectionDualButtonSmall(
                            leftText = stringResource(R.string.screen_edit_section_color_copy),
                            rightText = stringResource(R.string.screen_edit_section_color_paste),
                            onLeft = {
                                Clipboard.copy(context, "color", rustInterface.features_edit_color_get_as_hex(), false)
                            },
                            onRight = {
                                val text = Clipboard.paste(context) ?: return@SectionDualButtonSmall
                                rustInterface.features_edit_color_set(text)
                                color = rustInterface.features_edit_color_get()
                            }
                        )

                        if (iconResult.color != null) {
                            SectionButtonSmall(
                                text = stringResource(R.string.screen_edit_section_color_preset),
                                onClick = {
                                    rustInterface.features_edit_color_set(iconResult.color.toArgb())
                                    color = rustInterface.features_edit_color_get()
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
                            nameError = !rustInterface.features_edit_name_set(it)
                            name = it
                        },
                        error = nameError
                    )

                    if (codeType != CodeType.STEAM) {
                        SectionTextBox(
                            title = stringResource(R.string.screen_edit_section_details_issuer),
                            value = issuer,
                            onValueChange = {
                                rustInterface.features_edit_issuer_set(it)
                                issuer = it
                            }
                        )
                    }

                    SectionTextBox(
                        title = stringResource(R.string.screen_edit_section_details_note),
                        value = note,
                        onValueChange = {
                            rustInterface.features_edit_note_set(it)
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
                        value = secret,
                        onValueChange = {
                            secretError = !rustInterface.features_edit_secret_set(it)
                            secret = it
                            if (!secretError)  {
                                update++
                            }
                        },
                        placeholder = "Base32",
                        error = secretError
                    )

                    SectionDropdownSingleChoice(
                        title = stringResource(R.string.screen_edit_section_parameters_type),
                        options = CodeType.entries.toTypedArray(),
                        selectedOption = codeType,
                        onOptionSelected = {
                            rustInterface.features_edit_type_set(it.ordinal)
                            codeType = it

                            if (it == CodeType.STEAM) {
                                rustInterface.features_edit_issuer_set("Steam")
                                issuer = "Steam"

                                rustInterface.features_edit_algorithm_set(Algorithm.SHA1.ordinal)
                                algorithm = Algorithm.SHA1

                                periodError = !rustInterface.features_edit_period_set("30")
                                period = "30"

                                digitsError = !rustInterface.features_edit_digits_set("5")
                                digits = "5"
                            }

                            update++
                        },
                        label = { it.displayName }
                    )

                    if (codeType != CodeType.STEAM) {
                        SectionDropdownSingleChoice(
                            title = stringResource(R.string.screen_edit_section_parameters_algorithm),
                            options = Algorithm.entries.toTypedArray(),
                            selectedOption = algorithm,
                            onOptionSelected = {
                                rustInterface.features_edit_algorithm_set(it.ordinal)
                                algorithm = it
                                update++
                            },
                            label = { it.displayName }
                        )

                        if (codeType == CodeType.HOTP) {
                            SectionTextBox(
                                title = stringResource(R.string.screen_edit_section_parameters_counter),
                                value = counter,
                                onValueChange = {
                                    counterError = !rustInterface.features_edit_counter_set(it)
                                    counter = it
                                    if (!counterError) {
                                        update++
                                    }
                                },
                                placeholder = "0+",
                                numberField = true,
                                error = counterError
                            )
                        } else {
                            SectionTextBox(
                                title = stringResource(R.string.screen_edit_section_parameters_period),
                                value = period,
                                onValueChange = {
                                    periodError = !rustInterface.features_edit_period_set(it)
                                    period = it
                                    if (!periodError) {
                                        update++
                                    }
                                },
                                placeholder = "10+",
                                numberField = true,
                                error = periodError
                            )
                        }

                        SectionTextBox(
                            title = stringResource(R.string.screen_edit_section_parameters_digits),
                            value = digits,
                            onValueChange = {
                                digitsError = !rustInterface.features_edit_digits_set(it)
                                digits = it
                                if (!digitsError) {
                                    update++
                                }
                            },
                            placeholder = "4-10",
                            numberField = true,
                            error = digitsError
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
                rustInterface.management_edit_none()
                navController.popBackStack(MainRoute, false)
            },
            onDone = {
                if (nameError || secretError || digitsError) {
                    return@EditHeader
                }

                if (codeType == CodeType.HOTP && counterError) {
                    return@EditHeader
                }

                if (codeType == CodeType.TOTP && periodError) {
                    return@EditHeader
                }

                scope.launch(Dispatchers.IO) {
                    rustInterface.management_import_edit_into_accounts()
                    rustInterface.management_accounts_save()

                    if (Signals.checkEditAccountSome) {
                        Signals.checkEditAccountSome = false
                        Signals.accountAdded = true
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
                    rustInterface.features_edit_delete(account.id)
                    rustInterface.management_accounts_save()

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