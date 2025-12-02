package com.atmiya.innovation.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QRCodeGenerator {
    fun generateQRCode(content: String, width: Int = 512, height: Int = 512): ImageBitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
