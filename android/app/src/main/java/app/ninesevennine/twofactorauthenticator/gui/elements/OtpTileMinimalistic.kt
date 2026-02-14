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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.AccountTruncated
import app.ninesevennine.twofactorauthenticator.CodeType
import app.ninesevennine.twofactorauthenticator.RustInterface
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect.tiltShimmer
import app.ninesevennine.twofactorauthenticator.gui.Tints
import app.ninesevennine.twofactorauthenticator.gui.screens.EditRoute
import app.ninesevennine.twofactorauthenticator.utils.Clipboard
import app.ninesevennine.twofactorauthenticator.utils.Time
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun OtpTileMinimalistic(
    navController: NavController,
    account: AccountTruncated,
    updateCount: Int = 0,
    onCounterChange: (Long) -> Unit = {},
    isEditing: Boolean = false
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val colors = if (Theme.currentOption(context) == ThemeOption.LIGHT) Tints.light else Tints.dark
    val isUsingPitchBlack by remember { mutableStateOf(Theme.themeOption == ThemeOption.PITCH_BLACK) }

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
            RustInterface().getAccountCode(account.id, counter, isShowingNextCode)
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

    val shape = RoundedCornerShape(20.dp)

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

                            val code = RustInterface()
                                .getAccountCode(account.id, counter, isShowingNextCode)
                                .ifEmpty { "?".repeat(account.digits.coerceIn(4, 10)) }

                            Clipboard.copy(context, "authenticator-contents", code, true)
                        } else {
                            codeScopeJob = codeScope.launch {
                                try {
                                    delay(150)
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
                            Clipboard.copy(context, "authenticator-contents", rawCode, true)
                        }

                        if (Config.isUsingTapToRevealCodes) {
                            isCodeRevealed = !isCodeRevealed
                        }
                    }
                }
            )
            .clip(shape)
            .then(
                if (isUsingPitchBlack) {
                    Modifier.tiltShimmer(shape)
                } else {
                    Modifier
                        .then(
                            if (isEditing)
                                Modifier
                            else
                                Modifier.graphicsLayer(colorFilter = ColorFilter.tint(Color(account.color), BlendMode.Multiply))
                        )
                        .background(colors.light)
                        .border(1.dp, colors.medium, shape)
                }
            ),
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
                        color = if (isUsingPitchBlack) Theme.colors.primary else colors.dark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W700,
                        fontFamily = InterVariable,
                        overflow = TextOverflow.MiddleEllipsis,
                        maxLines = 1
                    )

                    if (account.name.isNotEmpty() && account.issuer.isNotEmpty()) {
                        Text(
                            text = account.name,
                            color = if (isUsingPitchBlack) Theme.colors.secondary else colors.dark,
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
                                color = if (isUsingPitchBlack) Theme.colors.primary else colors.medium,
                                style = Stroke(width = 2.75f)
                            )

                            val sweepAngle = -(remainingSeconds.toFloat() / account.period.toFloat()) * 360f
                            drawArc(
                                color = if (isUsingPitchBlack) Theme.colors.primary else colors.medium,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = true
                            )
                        }

                        if (isShowingNextCode) {
                            Spacer(Modifier.widthIn(2.dp))

                            Text(
                                text = "+1",
                                color = if (isUsingPitchBlack) Theme.colors.primary else colors.medium,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W900,
                                fontFamily = InterVariable
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
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

                                    val newCounter = RustInterface().incrementAccountCounterAndGet(account.id)
                                    counter = newCounter
                                    onCounterChange(newCounter)
                                }
                            ),
                        tint = if (isUsingPitchBlack) Theme.colors.primary else colors.medium
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

                                if (!isEditing) {
                                    RustInterface().copyAccountIntoEditAccount(account.id)
                                    navController.navigate(EditRoute)
                                }
                            }
                        ),
                    Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = if (isUsingPitchBlack) Theme.colors.primary else colors.medium
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(58.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayedCode,
                    modifier = Modifier.graphicsLayer(scaleX = codeScale, scaleY = codeScale),
                    color = if (isUsingPitchBlack) Theme.colors.primary else colors.medium,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = InterVariable,
                    maxLines = 1
                )
            }
        }
    }
}