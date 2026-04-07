package app.ninesevennine.twofactorauthenticator.gui.screens

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
import app.ninesevennine.twofactorauthenticator.gui.elements.RoundedRadioButton
import app.ninesevennine.twofactorauthenticator.gui.elements.StartButtonHeader
import app.ninesevennine.twofactorauthenticator.utils.Logger
import kotlinx.serialization.Serializable

@Serializable
object SelectOtpCardStyleRoute

private const val TAG = "SelectOtpCardStyle"

enum class OtpCardStyle(val value: Int) {
    NORMAL(0),
    COMPACT(1),
    GRID(2);
}

@Composable
fun SelectOtpCardStyle(navController: NavController) {
    val context = LocalContext.current

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

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
                    painter = painterResource(R.drawable.crop_16_9_with_circle),
                    tint = Theme.colors.primary,
                    text = stringResource(R.string.otp_card_style_normal),
                    enabled = Config.UsingOtpCardStyle == OtpCardStyle.NORMAL.value
                ) {
                    Logger.i(TAG, "otp card style set to normal")

                    Config.UsingOtpCardStyle = OtpCardStyle.NORMAL.value
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoundedRadioButton(
                    painter = painterResource(R.drawable.mi_crop_16_9),
                    tint = Theme.colors.primary,
                    text = stringResource(R.string.otp_card_style_compact),
                    enabled = Config.UsingOtpCardStyle == OtpCardStyle.COMPACT.value
                ) {
                    Logger.i(TAG, "otp card style set to compact")

                    Config.UsingOtpCardStyle = OtpCardStyle.COMPACT.value
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoundedRadioButton(
                    painter = painterResource(R.drawable.mi_tile_small),
                    tint = Theme.colors.primary,
                    text = stringResource(R.string.otp_card_style_grid),
                    enabled = Config.UsingOtpCardStyle == OtpCardStyle.GRID.value
                ) {
                    Logger.i(TAG, "otp card style set to grid")

                    Config.UsingOtpCardStyle = OtpCardStyle.GRID.value
                    Config.save(context)
                }

                Spacer(modifier = Modifier.height(bottomInset + 64.dp))
            }
        }

        StartButtonHeader(stringResource(R.string.screen_settings_appearance_card_style)) {
            navController.popBackStack()
        }
    }
}