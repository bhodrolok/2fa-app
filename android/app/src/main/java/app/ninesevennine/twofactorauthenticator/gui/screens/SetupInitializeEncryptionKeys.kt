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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.Entry
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.ShimmerEffect.tiltShimmer
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButtonNew
import app.ninesevennine.twofactorauthenticator.gui.elements.StrengthIndicator
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object SetupInitializeEncryptionKeysRoute

private const val TAG = "SetupInitializeEncryptionKeys"

@Composable
fun SetupInitializeEncryptionKeys(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val targetEntropy = 500
    val entropyPoints = remember { mutableStateListOf<Pair<Int, Int>>() }

    val alphabet = "* .-,+/"
    var pseudoSeed by remember { mutableStateOf("·".repeat(400)) }

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
                        repeat(400) { append(alphabet.random()) }
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
                .widthIn(max = 512.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).fillMaxWidth()
            ) {
                Text(
                    text = "Visualizer",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .height(128.dp)
                        .fillMaxWidth()
                        .background(Theme.colors.surface, RoundedCornerShape(8.dp))
                        .then(
                            if (Theme.themeOption == ThemeOption.PITCH_BLACK) {
                                Modifier.tiltShimmer(shape = RoundedCornerShape(8.dp))
                            } else {
                                Modifier.border(
                                    1.dp,
                                    Theme.colors.border,
                                    RoundedCornerShape(8.dp)
                                )
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
                        fontWeight = FontWeight.W700,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        overflow = TextOverflow.Clip,
                        softWrap = true,
                        maxLines = 6
                    )
                }

                AnimatedVisibility(entropyPoints.isNotEmpty()) {
                    StrengthIndicator(
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp, end = 4.dp, bottom = 4.dp),
                        strength = entropyPoints.size.toFloat() / targetEntropy
                    )
                }
            }

            Text(
                text = "Initialize Encryption Keys",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = Theme.colors.primary,
                fontSize = 18.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W700
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Move your finger randomly across the screen. The longer you move it, the stronger the resulting encryption keys become. This visualizer simply illustrates the entropy generation process. It does not reveal sensitive data and cannot be used to derive your keys.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = Theme.colors.secondary,
                fontSize = 14.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Justify
            )

            AnimatedVisibility(entropyPoints.size >= targetEntropy) {
                RoundedButtonNew(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = "Continue"
                ) {
                    if (isLoading) return@RoundedButtonNew

                    isLoading = true

                    val entropyArray = IntArray(entropyPoints.size * 2).apply {
                        var index = 0
                        entropyPoints.forEach { (x, y) ->
                            this[index++] = x
                            this[index++] = y
                        }
                    }

                    scope.launch(Dispatchers.IO) {
                        Logger.i(TAG, "continue pressed")

                        Entry.rustInterface.generateClientSeedWithEntropy(entropyArray)

                        Logger.i(TAG, "generated client seed using entropy buffer of size ${entropyArray.size / 2}")

                        withContext(Dispatchers.Main) {
                            navController.navigate(SetupBiometricUnlockRoute)
                        }
                    }
                }
            }
        }
    }
}