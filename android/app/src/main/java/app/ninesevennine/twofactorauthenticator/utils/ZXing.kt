package app.ninesevennine.twofactorauthenticator.utils

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

object ZXing {
    private const val TAG = "ZXing"

    fun decodeQRFromImage(uri: Uri, contentResolver: ContentResolver): String? {
        try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }

            val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null

            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.recycle()

            return runCatching {
                val source = RGBLuminanceSource(width, height, pixels)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                val reader = MultiFormatReader().apply {
                    setHints(
                        mapOf(
                            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                            DecodeHintType.TRY_HARDER to true
                        )
                    )
                }

                reader.decodeWithState(binaryBitmap)?.text
            }.getOrNull()
        } catch (e: Exception) {
            Log.i(TAG, "failed to decode QR from image", e)
            return null
        }
    }
}