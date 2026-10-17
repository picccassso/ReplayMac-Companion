package com.replaymac.companion.data.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.Base64
import java.io.ByteArrayOutputStream

/** Draws a demo clip's thumbnail as base64 JPEG, the form ReplayMac sends, or null if it can't. */
fun interface DemoThumbnailRenderer {
    fun render(clipId: String): String?
}

/** A 320x180 gradient, its hue picked by the clip id, with a play glyph in the middle. */
object GradientThumbnailRenderer : DemoThumbnailRenderer {
    private const val WIDTH = 320
    private const val HEIGHT = 180
    private const val JPEG_QUALITY = 80

    override fun render(clipId: String): String? = runCatching {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawGradient(canvas, hue = hueFor(clipId))
        drawPlayGlyph(canvas)
        val jpeg = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        bitmap.recycle()
        Base64.encodeToString(jpeg, Base64.NO_WRAP)
    }.getOrNull()

    /** Stable for a clip, so its thumbnail looks the same every time it's drawn. */
    private fun hueFor(clipId: String): Float = Math.floorMod(clipId.hashCode() * 47, 360).toFloat()

    private fun drawGradient(canvas: Canvas, hue: Float) {
        val light = Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.85f))
        val dark = Color.HSVToColor(floatArrayOf((hue + 40f) % 360f, 0.75f, 0.3f))
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), light, dark, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)
    }

    private fun drawPlayGlyph(canvas: Canvas) {
        val centerX = WIDTH / 2f
        val centerY = HEIGHT / 2f
        val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(80, 255, 255, 255) }
        canvas.drawCircle(centerX, centerY, 34f, disc)
        val triangle = Path().apply {
            moveTo(centerX - 10f, centerY - 16f)
            lineTo(centerX + 18f, centerY)
            lineTo(centerX - 10f, centerY + 16f)
            close()
        }
        canvas.drawPath(triangle, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
    }
}
