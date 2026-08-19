package app.ninesevennine.twofactorauthenticator.gui.elements

import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.AccountShort
import app.ninesevennine.twofactorauthenticator.CodeType
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.IconPacks
import app.ninesevennine.twofactorauthenticator.features.IconPacksStyle
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.OtpCard
import app.ninesevennine.twofactorauthenticator.gui.screens.EditRoute
import app.ninesevennine.twofactorauthenticator.rustInterface
import app.ninesevennine.twofactorauthenticator.utils.Clipboard
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.utils.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OtpCardNormal(
    navController: NavController,
    account: AccountShort,
    updateCount: Int = 0,
    onCounterChange: (Long) -> Unit = {},
    onPalettePressed: () -> Unit = {},
    isEditing: Boolean = false
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val scope = rememberCoroutineScope()

    val themeOption = Theme.currentOption(context)
    val isPitchBlack = Theme.themeOption == ThemeOption.PITCH_BLACK

    val colors = remember(themeOption, account.color) {
        when (themeOption) {
            ThemeOption.LIGHT -> OtpCard.lightFromColor(Color(account.color))
            ThemeOption.DARK -> OtpCard.darkFromColor(Color(account.color))
            else -> OtpCard.pitchBlack
        }
    }

    val accent = if (isPitchBlack) colors.text else colors.surface
    val onAccent = if (isPitchBlack) colors.text else colors.background

    var isCodeRevealed by remember { mutableStateOf(!Config.isUsingTapToRevealCodes) }

    val isHotp = account.codeType == CodeType.HOTP.ordinal
    var hotpCounter by remember(account.counter) { mutableLongStateOf(account.counter) }

    val counter = if (isHotp) hotpCounter else Time.epochSeconds / account.period
    val remainingSeconds = if (isHotp) 0L else account.period - (Time.epochSeconds % account.period)

    var showNextForCounter by remember { mutableLongStateOf(-1) }
    val isShowingNextCode = showNextForCounter == counter

    val rawCode = remember(counter, isShowingNextCode, isCodeRevealed, updateCount) {
        rustInterface.features_otp_generate(account.id, counter, isShowingNextCode, isCodeRevealed)
    }

    val displayedCode = remember(rawCode) {
        if (rawCode.length > 5) {
            rawCode.chunked(rawCode.length / 2).joinToString(" ")
        } else {
            rawCode
        }
    }

    val iconBitmap = remember(account.issuer) {
        IconPacks.get(context, account.issuer).bitmap?.asImageBitmap()
    }

    val iconPacksStyle = Config.UsingIconPacksStyle

    val fallbackInitial = remember(account.issuer, account.name) {
        when {
            account.issuer.isNotEmpty() -> account.issuer.first().uppercase()
            account.name.isNotEmpty() -> account.name.first().uppercase()
            else -> "?"
        }
    }

    val codeInteractionSource = remember { MutableInteractionSource() }
    val isCodePressed by codeInteractionSource.collectIsPressedAsState()
    val codeScale by animateFloatAsState(if (isCodePressed) 0.95f else 1f)
    var codeScopeJob by remember { mutableStateOf<Job?>(null) }

    val periodInteractionSource = remember { MutableInteractionSource() }
    val isPeriodPressed by periodInteractionSource.collectIsPressedAsState()
    val periodScale by animateFloatAsState(if (isPeriodPressed) 0.85f else 1f)

    val counterInteractionSource = remember { MutableInteractionSource() }
    val isCounterPressed by counterInteractionSource.collectIsPressedAsState()
    val counterScale by animateFloatAsState(if (isCounterPressed) 0.85f else 1f)

    var counterCumulativeRotation by remember { mutableFloatStateOf(0f) }
    val counterIconRotation by animateFloatAsState(
        targetValue = counterCumulativeRotation,
        animationSpec = tween(durationMillis = 500)
    )

    val editInteractionSource = remember { MutableInteractionSource() }
    val isEditPressed by editInteractionSource.collectIsPressedAsState()
    val editScale by animateFloatAsState(if (isEditPressed) 0.85f else 1f)

    val shape = remember { RoundedCornerShape(24.dp) }
    val codeShape = remember { RoundedCornerShape(18.dp) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(144.dp)
            .clickable(
                interactionSource = codeInteractionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    view.playSoundEffect(SoundEffectConstants.CLICK)

                    if (Config.isUsingDoubleTapToCopyCodes) {
                        if (codeScopeJob?.isActive == true) {
                            codeScopeJob?.cancel()

                            val code = rustInterface.features_otp_generate(account.id, counter, isShowingNextCode, true)

                            Clipboard.copy(context, "OTP", code, Config.isUsingSecureClipboard)
                        } else {
                            codeScopeJob = scope.launch {
                                try {
                                    delay(200.milliseconds)
                                    if (Config.isUsingTapToRevealCodes) {
                                        isCodeRevealed = !isCodeRevealed
                                    }
                                } catch (_: CancellationException) {
                                    // job canceled.
                                }
                            }
                        }
                    } else {
                        if (isCodeRevealed) {
                            Clipboard.copy(context, "OTP", rawCode, Config.isUsingSecureClipboard)
                        }

                        if (Config.isUsingTapToRevealCodes) {
                            isCodeRevealed = !isCodeRevealed
                        }
                    }
                }
            )
            .clip(shape)
            .background(colors.background)
            .border(1.dp, colors.border, shape),
        Alignment.Center
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                Alignment.Center
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (iconBitmap != null) {
                            Canvas(Modifier.size(52.dp)) {
                                when (iconPacksStyle) {
                                    IconPacksStyle.TEXTURED.value -> {
                                        drawImage(image = iconBitmap)
                                    }

                                    IconPacksStyle.TINTED.value -> {
                                        drawImage(
                                            image = iconBitmap,
                                            colorFilter = ColorFilter.tint(accent, BlendMode.Modulate)
                                        )
                                    }

                                    else -> {}
                                }
                            }
                        } else {
                            Canvas(Modifier.size(52.dp)) {
                                drawCircle(color = colors.surface)
                            }

                            Text(
                                text = fallbackInitial,
                                color = onAccent,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.W700,
                                fontFamily = InterVariable,
                                overflow = TextOverflow.MiddleEllipsis,
                                maxLines = 1
                            )
                        }
                    }

                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp),
                        Arrangement.Center
                    ) {
                        Text(
                            text = account.issuer.ifEmpty { account.name },
                            color = colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W700,
                            fontFamily = InterVariable,
                            overflow = TextOverflow.MiddleEllipsis,
                            maxLines = 1
                        )

                        if (account.name.isNotEmpty() && account.issuer.isNotEmpty()) {
                            Text(
                                text = account.name,
                                color = colors.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W500,
                                fontFamily = InterVariable,
                                overflow = TextOverflow.MiddleEllipsis,
                                maxLines = 1
                            )
                        }
                    }

                    if (!isHotp) {
                        Row(
                            Modifier
                                .graphicsLayer(scaleX = periodScale, scaleY = periodScale)
                                .clickable(
                                    interactionSource = periodInteractionSource,
                                    indication = null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        view.playSoundEffect(SoundEffectConstants.CLICK)

                                        showNextForCounter = if (showNextForCounter == counter) -1 else counter
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Canvas(Modifier.size(26.dp)) {
                                drawCircle(color = accent, style = Stroke(width = 3f))

                                drawArc(
                                    color = accent,
                                    startAngle = -90f,
                                    sweepAngle = -(remainingSeconds.toFloat() / account.period.toFloat()) * 360f,
                                    useCenter = true
                                )
                            }

                            if (isShowingNextCode) {
                                Spacer(Modifier.widthIn(2.dp))

                                Text(
                                    text = "+1",
                                    color = accent,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.W900,
                                    fontFamily = InterVariable
                                )
                            }
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.mi_refresh),
                            contentDescription = null,
                            modifier = Modifier
                                .graphicsLayer(scaleX = counterScale, scaleY = counterScale, rotationZ = counterIconRotation)
                                .size(30.dp)
                                .clickable(
                                    interactionSource = counterInteractionSource,
                                    indication = null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        view.playSoundEffect(SoundEffectConstants.CLICK)

                                        counterCumulativeRotation += 360f

                                        hotpCounter = rustInterface.features_otp_counter_increment(account.id)
                                        onCounterChange(hotpCounter)

                                        if (!isEditing) {
                                            scope.launch(Dispatchers.IO) {
                                                rustInterface.management_accounts_save()
                                            }
                                        }
                                    }
                                ),
                            tint = accent
                        )
                    }

                    if (isEditing) {
                        if (!isPitchBlack) {
                            Spacer(Modifier.widthIn(8.dp))

                            Box(
                                Modifier
                                    .graphicsLayer(scaleX = editScale, scaleY = editScale)
                                    .size(32.dp)
                                    .clickable(
                                        interactionSource = editInteractionSource,
                                        indication = null,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            view.playSoundEffect(SoundEffectConstants.CLICK)

                                            onPalettePressed()
                                        }
                                    ),
                                Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.mi_palette),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = colors.surface
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.widthIn(8.dp))

                        Box(
                            Modifier
                                .graphicsLayer(scaleX = editScale, scaleY = editScale)
                                .size(32.dp)
                                .clickable(
                                    interactionSource = editInteractionSource,
                                    indication = null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        view.playSoundEffect(SoundEffectConstants.CLICK)

                                        rustInterface.management_import_account_into_edit(account.id)
                                        navController.navigate(EditRoute)
                                    }
                                ),
                            Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.mi_edit),
                                contentDescription = null,
                                modifier = Modifier.size(29.dp),
                                tint = accent
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(codeShape)
                    .background(colors.surface),
                Alignment.Center
            ) {
                Text(
                    text = displayedCode,
                    modifier = Modifier.graphicsLayer(scaleX = codeScale, scaleY = codeScale),
                    color = onAccent,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = InterVariable,
                    maxLines = 1
                )
            }
        }
    }
}