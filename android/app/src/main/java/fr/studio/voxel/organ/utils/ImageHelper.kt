package fr.studio.voxel.organ.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

object ImageHelper {

    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                val maxDimension = 1024
                val width = bitmap.width
                val height = bitmap.height
                val newBitmap = if (width > maxDimension || height > maxDimension) {
                    val ratio = width.toFloat() / height.toFloat()
                    val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                    val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                    Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                } else {
                    bitmap
                }

                val outputStream = ByteArrayOutputStream()
                newBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val bytes = outputStream.toByteArray()
                
                if (newBitmap != bitmap) {
                    newBitmap.recycle()
                }
                bitmap.recycle()

                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val mimeType = "image/jpeg"
                "data:$mimeType;base64,$base64"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ImageHelper", "Error converting and compressing Uri to Base64", e)
            null
        }
    }
}
