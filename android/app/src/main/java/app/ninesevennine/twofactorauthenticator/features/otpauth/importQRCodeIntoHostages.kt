package app.ninesevennine.twofactorauthenticator.features.otpauth

import android.graphics.Bitmap
import app.ninesevennine.twofactorauthenticator.rustInterface

fun importQRCodeIntoHostages(bitmap: Bitmap): Boolean {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)

    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    return rustInterface.features_otpauth_import_qrcode_into_hostages(width, height, pixels)
}