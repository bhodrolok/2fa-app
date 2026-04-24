package app.ninesevennine.twofactorauthenticator.gui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.utils.Config
import app.ninesevennine.twofactorauthenticator.features.Locale
import app.ninesevennine.twofactorauthenticator.features.LocaleOption
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedRadioButton
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedRefreshButton
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.serialization.Serializable

@Serializable
object SelectLanguageRoute

private const val TAG = "SelectLanguage"

@Composable
fun SelectLanguage(navController: NavController) {
    val context = LocalContext.current

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    var showRefreshButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showRefreshButton = true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 512.dp).padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(topInset + 80.dp))

                RoundedRadioButton(
                    painter = painterResource(R.drawable.flag_europe),
                    text = LocaleOption.ENGLISH.displayName,
                    enabled = Locale.currentEffectiveLocale(context) == LocaleOption.ENGLISH
                ) {
                    Logger.i(TAG, "language set to english")

                    Locale.set(context, LocaleOption.ENGLISH)
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoundedRadioButton(
                    painter = painterResource(R.drawable.flag_spain),
                    text = LocaleOption.SPANISH.displayName,
                    enabled = Locale.currentEffectiveLocale(context) == LocaleOption.SPANISH
                ) {
                    Logger.i(TAG, "language set to spanish")

                    Locale.set(context, LocaleOption.SPANISH)
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoundedRadioButton(
                    painter = painterResource(R.drawable.flag_russia),
                    text = LocaleOption.RUSSIAN.displayName,
                    enabled = Locale.currentEffectiveLocale(context) == LocaleOption.RUSSIAN
                ) {
                    Logger.i(TAG, "language set to russian")

                    Locale.set(context, LocaleOption.RUSSIAN)
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoundedRadioButton(
                    painter = painterResource(R.drawable.flag_germany),
                    text = LocaleOption.GERMAN.displayName,
                    enabled = Locale.currentEffectiveLocale(context) == LocaleOption.GERMAN
                ) {
                    Logger.i(TAG, "language set to german")

                    Locale.set(context, LocaleOption.GERMAN)
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoundedRadioButton(
                    painter = painterResource(R.drawable.flag_netherlands),
                    text = LocaleOption.DUTCH.displayName,
                    enabled = Locale.currentEffectiveLocale(context) == LocaleOption.DUTCH
                ) {
                    Logger.i(TAG, "language set to dutch")

                    Locale.set(context, LocaleOption.DUTCH)
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoundedRadioButton(
                    painter = painterResource(R.drawable.flag_estonia),
                    text = LocaleOption.ESTONIAN.displayName,
                    enabled = Locale.currentEffectiveLocale(context) == LocaleOption.ESTONIAN
                ) {
                    Logger.i(TAG, "language set to estonian")

                    Locale.set(context, LocaleOption.ESTONIAN)
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = showRefreshButton && Locale.localeOption != LocaleOption.SYSTEM,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> -fullHeight / 4 },
                        animationSpec = spring(stiffness = 250f, dampingRatio = 0.85f)
                    ) + scaleIn(
                        initialScale = 0.9f,
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)),
                    exit = fadeOut(
                        animationSpec = tween(durationMillis = 300)
                    ) + scaleOut(
                        targetScale = 0.9f,
                        animationSpec = tween(durationMillis = 300)
                    )
                ) {
                    RoundedRefreshButton(
                        text = stringResource(R.string.common_button_use_system_default)
                    ) {
                        Logger.i(TAG, "language set to system")

                        Locale.set(context, LocaleOption.SYSTEM)
                        Config.save(context)
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }
        }

        StartButtonHeader(stringResource(R.string.screen_settings_appearance_language)) {
            navController.popBackStack()
        }
    }
}