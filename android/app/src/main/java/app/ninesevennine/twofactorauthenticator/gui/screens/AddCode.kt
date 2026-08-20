package app.ninesevennine.twofactorauthenticator.gui.screens

import android.content.Intent
import android.view.SoundEffectConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.features.QRCodeScanner
import app.ninesevennine.twofactorauthenticator.gui.InterVariable
import app.ninesevennine.twofactorauthenticator.rustInterface
import app.ninesevennine.twofactorauthenticator.utils.Signals
import app.ninesevennine.twofactorauthenticator.utils.ZXing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.json.JSONObject


@Serializable
object AddCodeRoute

@Composable
fun AddCode(navController: NavController) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }

    val cancelInteractionSource = remember { MutableInteractionSource() }
    val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
    val cancelScale by animateFloatAsState(if (isCancelPressed) 0.95f else 1f)

    val enterInteractionSource = remember { MutableInteractionSource() }
    val isEnterPressed by enterInteractionSource.collectIsPressedAsState()
    val enterScale by animateFloatAsState(if (isEnterPressed) 0.95f else 1f)

    val imageInteractionSource = remember { MutableInteractionSource() }
    val isImagePressed by imageInteractionSource.collectIsPressedAsState()
    val imageScale by animateFloatAsState(if (isImagePressed) 0.95f else 1f)

    val shape = RoundedCornerShape(26.dp)

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().let { if (it < 16.dp) 16.dp else it + 16.dp }

    val pickScope = rememberCoroutineScope()
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            pickScope.launch(Dispatchers.IO) {
                val otpUrl = when {
                    // QR images
                    context.contentResolver.getType(it)?.startsWith("image/") == true -> {
                        ZXing.decodeQRFromImage(it, context.contentResolver)
                    }

                    // .maFile
                    else -> {
                        runCatching {
                            context.contentResolver.openInputStream(it)?.use { stream ->
                                val jsonContent = stream.readBytes().decodeToString()
                                JSONObject(jsonContent).optString("uri")
                            }
                        }.getOrNull()?.takeIf { url -> url.isNotEmpty() }
                    }
                }

                if (otpUrl != null && rustInterface.features_otpauth_import_into_hostages(otpUrl)) {
                    withContext(Dispatchers.Main) {
                        Signals.accountAdded = true
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    QRCodeScanner.View(
        onQRCodeScanned = { uri ->
            if (isProcessing) return@View

            isProcessing = true

            scope.launch(Dispatchers.Main) {
                if (rustInterface.features_otpauth_import_into_hostages(uri)) {
                    Signals.accountAdded = true
                    navController.popBackStack()
                } else {
                    isProcessing = false
                }
            }

        }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = navBarPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer(scaleX = cancelScale, scaleY = cancelScale)
                    .size(48.dp)
                    .clip(shape)
                    .background(color = Color(0x99000000))
                    .clickable(
                        interactionSource = cancelInteractionSource,
                        indication = ripple(color = Color.White),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            view.playSoundEffect(SoundEffectConstants.CLICK)

                            navController.popBackStack()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.mi_close),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier
                    .graphicsLayer(scaleX = enterScale, scaleY = enterScale)
                    .height(48.dp)
                    .clip(shape)
                    .background(color = Color(0x99000000))
                    .clickable(
                        interactionSource = enterInteractionSource,
                        indication = ripple(color = Color.White),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            view.playSoundEffect(SoundEffectConstants.CLICK)

                            rustInterface.management_edit_default()
                            navController.navigate(EditRoute)
                        }
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.screen_add_code_enter_manually),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = InterVariable,
                    fontWeight = FontWeight.W700
                )
            }

            Box(
                modifier = Modifier
                    .graphicsLayer(scaleX = imageScale, scaleY = imageScale)
                    .size(48.dp)
                    .clip(shape)
                    .background(color = Color(0x99000000))
                    .clickable(
                        interactionSource = imageInteractionSource,
                        indication = ripple(color = Color.White),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            view.playSoundEffect(SoundEffectConstants.CLICK)

                            Signals.suppressLockOnExit = true
                            pickImage.launch(arrayOf("*/*"))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.mi_upload_file),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}