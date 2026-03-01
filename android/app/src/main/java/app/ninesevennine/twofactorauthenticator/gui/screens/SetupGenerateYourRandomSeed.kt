package app.ninesevennine.twofactorauthenticator.gui.screens

import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect.tiltShimmer
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.spinnerText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object SetupGenerateYourRandomSeedRoute

@Composable
fun SetupGenerateYourRandomSeed(navController: NavController) {
    val targetEntropy = 500
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val entropyPoints = remember { mutableStateListOf<Pair<Int, Int>>() }

    val entropyColor = when {
        entropyPoints.size <= 300 -> Color.Red
        entropyPoints.size >= targetEntropy -> Color.Green
        else -> {
            val fraction = (entropyPoints.size - 300).toFloat() / (targetEntropy - 300)
            Color(red = 1f - fraction, green = fraction, blue = 0f)
        }
    }

    val alphabet = "* .-,+/"
    var pseudoSeed by remember { mutableStateOf("·".repeat(500)) }

    BackHandler(enabled = isLoading) {
        // intercept the system back button when loading.
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    entropyPoints.add(change.position.x.toInt() to change.position.y.toInt())

                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    view.playSoundEffect(SoundEffectConstants.CLICK)

                    pseudoSeed = buildString {
                        repeat(500) { append(alphabet.random()) }
                    }

                    change.consume()
                }
            }
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.setup_generate_your_random_seed_pseudo),
                        color = Theme.colors.primary,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )

                    Text(
                        text = "${entropyPoints.size} / $targetEntropy",
                        color = entropyColor,
                        fontSize = 14.sp,
                        fontFamily = InterVariable,
                        fontWeight = FontWeight.W700
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .height(152.dp)
                        .fillMaxWidth()
                        .background(Theme.colors.surface)
                        .then(
                            if(Theme.themeOption == ThemeOption.PITCH_BLACK) {
                                Modifier.tiltShimmer(shape = RoundedCornerShape(16.dp))
                            } else {
                                Modifier.border(1.dp, Theme.colors.border, RoundedCornerShape(16.dp))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pseudoSeed,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = Theme.colors.primary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.W500,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        overflow = TextOverflow.Clip,
                        softWrap = true,
                        maxLines = 8
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.setup_generate_your_random_seed_title),
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 20.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W700
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.setup_generate_your_random_seed_info),
                modifier = Modifier.fillMaxWidth(),
                color = Theme.colors.primary,
                fontSize = 14.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W500
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(entropyPoints.size >= targetEntropy) {
                RoundedButton(
                    text = if (isLoading) spinnerText() else stringResource(R.string.common_button_continue)
                ) {
                    if (isLoading) return@RoundedButton

                    isLoading = true

                    val entropyArray = IntArray(entropyPoints.size * 2).apply {
                        var index = 0
                        entropyPoints.forEach { (x, y) ->
                            this[index++] = x
                            this[index++] = y
                        }
                    }

                    scope.launch(Dispatchers.IO) {
                        Entry.rustInterface.generateClientSeedWithEntropy(entropyArray)

                        withContext(Dispatchers.Main) {
                            navController.navigate(SetupBiometricUnlockRoute)
                        }
                    }
                }
            }
        }
    }
}