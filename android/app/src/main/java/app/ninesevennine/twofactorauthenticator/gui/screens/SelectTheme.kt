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
import app.ninesevennine.twofactorauthenticator.features.Config
import app.ninesevennine.twofactorauthenticator.features.Theme
import app.ninesevennine.twofactorauthenticator.features.ThemeOption
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedRadioButton
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedRefreshButton
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.serialization.Serializable

@Serializable
object SelectThemeRoute

private const val TAG = "SelectTheme"

@Composable
fun SelectTheme(navController: NavController) {
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
                    painter = painterResource(R.drawable.mi_light_mode),
                    tint = Theme.colors.primary,
                    text = stringResource(R.string.theme_option_light),
                    enabled = Theme.currentOption(context) == ThemeOption.LIGHT
                ) {
                    Logger.i(TAG, "theme set to light")

                    Theme.set(context, ThemeOption.LIGHT)
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoundedRadioButton(
                    painter = painterResource(R.drawable.mi_dark_mode),
                    tint = Theme.colors.primary,
                    text = stringResource(R.string.theme_option_dark),
                    enabled = Theme.currentOption(context) == ThemeOption.DARK
                ) {
                    Logger.i(TAG, "theme set to dark")

                    Theme.set(context, ThemeOption.DARK)
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoundedRadioButton(
                    painter = painterResource(R.drawable.mi_contrast),
                    tint = Theme.colors.primary,
                    text = stringResource(R.string.theme_option_pitch_black),
                    enabled = Theme.currentOption(context) == ThemeOption.PITCH_BLACK
                ) {
                    Logger.i(TAG, "theme set to pitch black")

                    Theme.set(context, ThemeOption.PITCH_BLACK)
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = showRefreshButton && Theme.themeOption != ThemeOption.SYSTEM,
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
                        Logger.i(TAG, "theme set to system")

                        Theme.set(context, ThemeOption.SYSTEM)
                        Config.save(context)
                    }
                }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }
        }

        StartButtonHeader(stringResource(R.string.screen_settings_appearance_theme)) {
            navController.popBackStack()
        }
    }
}