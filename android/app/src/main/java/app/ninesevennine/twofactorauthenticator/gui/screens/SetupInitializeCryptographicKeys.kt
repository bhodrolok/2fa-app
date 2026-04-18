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
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedButton
import app.ninesevennine.twofactorauthenticator.gui.elements.StrengthIndicator
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object SetupInitializeCryptographicKeysRoute

private const val TAG = "SetupInitializeCryptographicKeys"

@Composable
fun SetupInitializeCryptographicKeys(navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val targetEntropy = 500
    val entropyPoints = remember { mutableStateListOf<Pair<Int, Int>>() }

    val alphabet = "* .-,+/"
    var pseudoPool by remember { mutableStateOf("·".repeat(400)) }

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

                    pseudoPool = buildString {
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
                    text = stringResource(R.string.screen_initialize_cryptographic_keys_pseudo_pool),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    color = Theme.colors.primary,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .height(128.dp)
                        .fillMaxWidth()
                        .background(Theme.colors.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, Theme.colors.border, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pseudoPool,
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
                text = stringResource(R.string.screen_initialize_cryptographic_keys_title),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = Theme.colors.primary,
                fontSize = 18.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.W700
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.screen_initialize_cryptographic_keys_description),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = Theme.colors.primary,
                fontSize = 14.sp,
                fontFamily = InterVariable,
                fontWeight = FontWeight.Normal
            )

            AnimatedVisibility(entropyPoints.size >= targetEntropy) {
                RoundedButton(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(R.string.common_button_continue)
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
                        Logger.i(TAG, "continue pressed")

                        Entry.rustInterface.generateClientSecretWithEntropy(entropyArray)

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