package app.ninesevennine.twofactorauthenticator.features

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.ninesevennine.twofactorauthenticator.R
import app.ninesevennine.twofactorauthenticator.ui.InterVariable
import app.ninesevennine.twofactorauthenticator.rustInterface
import java.util.concurrent.Executors

object QRCodeScanner {
    private const val TAG = "QRCodeScanner"

    @Composable
    fun View(
        onOtpAuthImported: () -> Unit
    ) {
        val context = LocalContext.current

        var hasCameraPermission by remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> hasCameraPermission = granted }

        LaunchedEffect(Unit) {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                PackageManager.PERMISSION_GRANTED -> hasCameraPermission = true
                else -> permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                CameraPreview(onOtpAuthImported)
            } else {
                Text(
                    text = stringResource(R.string.feature_qr_code_scanner_camera_permission),
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = InterVariable,
                )
            }
        }
    }

    @Composable
    private fun CameraPreview(onOtpAuthImported: () -> Unit) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

        val previewView = remember {
            PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FIT_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            }
        }

        val scanBoxFraction = 0.7f

        val imageAnalyzer = remember {
            ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder().setResolutionStrategy(
                        ResolutionStrategy(
                            Size(960, 960),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    ).build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setImageQueueDepth(1)
                .setOutputImageRotationEnabled(true)
                .build()
        }

        val analyzer = remember { RustQRAnalyzer(scanBoxFraction, onOtpAuthImported) }

        var cameraAvailable by remember { mutableStateOf(true) }

        DisposableEffect(lifecycleOwner) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val selector = CameraSelector.DEFAULT_BACK_CAMERA
                    if (!cameraProvider.hasCamera(selector)) {
                        cameraAvailable = false
                        return@addListener
                    }

                    val preview = Preview.Builder().build().apply {
                        surfaceProvider = previewView.surfaceProvider
                    }

                    imageAnalyzer.setAnalyzer(cameraExecutor, analyzer)
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageAnalyzer
                    )

                    cameraAvailable = true
                } catch (e: Exception) {
                    Log.e(TAG, "failed adding listener", e)
                    cameraAvailable = false
                }
            }, ContextCompat.getMainExecutor(context))

            onDispose {
                try {
                    imageAnalyzer.clearAnalyzer()
                    cameraExecutor.shutdown()
                } catch (_: Exception) { }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (cameraAvailable) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            val boxSize = size.width.coerceAtMost(size.height) * scanBoxFraction
                            val left = (size.width - boxSize) / 2
                            val top = (size.height - boxSize) / 2

                            drawRect(Color(0x88000000))

                            drawIntoCanvas { canvas ->
                                val paint = Paint().apply { blendMode = BlendMode.Clear }
                                canvas.drawRect(Rect(left, top, left + boxSize, top + boxSize), paint)
                            }

                            drawRect(
                                color = Color.White,
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
                                style = Stroke(width = 4f)
                            )
                        }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.feature_qr_code_scanner_camera_unavailable),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W700,
                        fontFamily = InterVariable
                    )
                }
            }
        }
    }

    private class RustQRAnalyzer(
        private val scanBoxFraction: Float,
        private val onOtpAuthImported: () -> Unit
    ) : ImageAnalysis.Analyzer {

        private var imported = false
        private var croppedRow = ByteArray(0)
        private var pixels = IntArray(0)

        override fun analyze(image: ImageProxy) {
            if (imported) {
                image.close()
                return
            }

            val plane = image.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride

            val boxSize = (image.width.coerceAtMost(image.height) * scanBoxFraction).toInt()
            val left = (image.width - boxSize) / 2
            val top = (image.height - boxSize) / 2

            if (croppedRow.size != boxSize) {
                croppedRow = ByteArray(boxSize)
                pixels = IntArray(boxSize * boxSize)
            }

            for (y in 0 until boxSize) {
                buffer.position((top + y) * rowStride + left)
                buffer.get(croppedRow, 0, boxSize)

                val offset = y * boxSize
                for (x in 0 until boxSize) {
                    val v = croppedRow[x].toInt() and 0xFF
                    pixels[offset + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
                }
            }

            if (rustInterface.features_otpauth_import_qrcode_into_hostages(boxSize, boxSize, pixels)) {
                imported = true
                onOtpAuthImported()
            }

            croppedRow.fill(0)
            pixels.fill(0)

            image.close()
        }
    }
}