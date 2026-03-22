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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
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
import app.ninesevennine.twofactorauthenticator.AccountTruncated
import app.ninesevennine.twofactorauthenticator.CodeType
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.AccountManagement
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.OtpCard
import app.ninesevennine.twofactorauthenticator.gui.screens.EditRoute
import app.ninesevennine.twofactorauthenticator.gui.screens.OtpCardStyle
import app.ninesevennine.twofactorauthenticator.utils.Clipboard
import app.ninesevennine.twofactorauthenticator.utils.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun OtpCardMinimalistic(
    navController: NavController,
    account: AccountTruncated,
    updateCount: Int = 0,
    onCounterChange: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val scope = rememberCoroutineScope()

    val colors = when (Theme.currentOption(context)) {
        ThemeOption.LIGHT -> OtpCard.lightFromColor(Color(account.color))
        ThemeOption.DARK -> OtpCard.darkFromColor(Color(account.color))
        else -> OtpCard.pitchBlack
    }

    var isCodeRevealed by remember { mutableStateOf(!Config.isUsingTapToRevealCodes) }

    var counter by remember { mutableLongStateOf(account.counter) }
    var remainingSeconds by remember { mutableLongStateOf(account.period) }

    var showNextForCounter by remember { mutableLongStateOf(-1) }
    val isShowingNextCode = showNextForCounter == counter

    if (account.codeType != CodeType.HOTP) {
        LaunchedEffect(Time.epochSeconds, account.period) {
            counter = Time.epochSeconds / account.period
            remainingSeconds = account.period - (Time.epochSeconds % account.period)
        }
    } else {
        LaunchedEffect(account.counter) {
            counter = account.counter
        }
    }

    val rawCode = remember(counter, isShowingNextCode, isCodeRevealed, updateCount) {
        if (isCodeRevealed) {
            Entry.rustInterface.getAccountCode(account.id, counter, isShowingNextCode)
                .ifEmpty { "?".repeat(account.digits) }
        } else {
            "•".repeat(account.digits)
        }
    }

    val displayedCode = remember(rawCode) {
        if (rawCode.length > 5) {
            rawCode.chunked(rawCode.length / 2).joinToString(" ")
        } else {
            rawCode
        }
    }

    val codeInteractionSource = remember { MutableInteractionSource() }
    val isCodePressed by codeInteractionSource.collectIsPressedAsState()
    val codeScale by animateFloatAsState(if (isCodePressed) 0.95f else 1f)
    val codeScope = rememberCoroutineScope()
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

    val shape = RoundedCornerShape(16.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clickable(
                interactionSource = codeInteractionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    view.playSoundEffect(SoundEffectConstants.CLICK)

                    if (Config.isUsingDoubleTapToCopyCodes) {
                        if (codeScopeJob?.isActive == true) {
                            codeScopeJob?.cancel()

                            val code = Entry
                                .rustInterface
                                .getAccountCode(account.id, counter, isShowingNextCode)
                                .ifEmpty { "?".repeat(account.digits.coerceIn(4, 10)) }

                            Clipboard.copy(context, "OTP", code, Config.isUsingSecureClipboard)
                        } else {
                            codeScopeJob = codeScope.launch {
                                try {
                                    delay(200)
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
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp).height(34.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp),
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

                Spacer(Modifier.widthIn(6.dp))

                if (account.codeType == CodeType.TOTP || account.codeType == CodeType.STEAM) {
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
                        Canvas(Modifier.size(24.dp)) {
                            drawCircle(
                                color = if (Theme.themeOption == ThemeOption.PITCH_BLACK) colors.text else colors.surface,
                                style = Stroke(width = 2.75f)
                            )

                            val sweepAngle = -(remainingSeconds.toFloat() / account.period.toFloat()) * 360f
                            drawArc(
                                color = if (Theme.themeOption == ThemeOption.PITCH_BLACK) colors.text else colors.surface,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = true
                            )
                        }

                        if (isShowingNextCode) {
                            Spacer(Modifier.widthIn(2.dp))

                            Text(
                                text = "+1",
                                color = if (Theme.themeOption == ThemeOption.PITCH_BLACK) colors.text else colors.surface,
                                fontSize = 16.sp,
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
                            .size(28.dp)
                            .clickable(
                                interactionSource = counterInteractionSource,
                                indication = null,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)

                                    counterCumulativeRotation += 360f

                                    val newCounter = Entry.rustInterface.incrementAccountCounterAndGet(account.id)
                                    counter = newCounter
                                    onCounterChange(newCounter)

                                    scope.launch(Dispatchers.IO) {
                                        AccountManagement.saveAccountsUsingSeed(context)

                                        if (Config.isUsingAutomaticBackups) {
                                            AccountManagement.saveAccountsUsingAutomaticBackups(context)
                                        }
                                    }
                                }
                            ),
                        tint = if (Theme.themeOption == ThemeOption.PITCH_BLACK) colors.text else colors.surface
                    )
                }

                Spacer(Modifier.widthIn(6.dp))

                Box(
                    Modifier
                        .graphicsLayer(scaleX = editScale, scaleY = editScale)
                        .size(26.dp)
                        .clickable(
                            interactionSource = editInteractionSource,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                view.playSoundEffect(SoundEffectConstants.CLICK)

                                Entry.rustInterface.copyAccountIntoEditAccount(account.id)
                                navController.navigate(EditRoute)
                            }
                        ),
                    Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mi_edit),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = if (Theme.themeOption == ThemeOption.PITCH_BLACK) colors.text else colors.surface
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(58.dp).padding(start = if (Config.UsingOtpCardStyle == OtpCardStyle.COMPACT.value) 8.dp else 0.dp),
                horizontalArrangement = if (Config.UsingOtpCardStyle == OtpCardStyle.COMPACT.value) Arrangement.Start else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayedCode,
                    modifier = Modifier.graphicsLayer(scaleX = codeScale, scaleY = codeScale),
                    color = if (Theme.themeOption == ThemeOption.PITCH_BLACK) colors.text else colors.surface,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = InterVariable,
                    maxLines = 1
                )
            }
        }
    }
}