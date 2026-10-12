package com.naury.chageun.core.uitesting

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import java.io.File

/**
 * 프로필 사진 대신 쓰는 그림. 실제 사람 사진을 저장소에 두지 않으려고 하늘색 배경에 사람 윤곽을 그린다.
 * 동그라미로 잘렸을 때 가운데가 맞는지 보이도록 정사각형으로 만든다.
 */
fun writeSampleProfilePhoto(file: File, size: Int = 192): File {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val s = size.toFloat()
    val background = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, s, SKY_TOP, SKY_BOTTOM, Shader.TileMode.CLAMP)
    }
    canvas.drawRect(0f, 0f, s, s, background)
    val person = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PERSON }
    canvas.drawCircle(s / 2, s * HEAD_Y, s * HEAD_RADIUS, person)
    canvas.drawOval(s * SHOULDER_LEFT, s * SHOULDER_TOP, s * (1 - SHOULDER_LEFT), s * SHOULDER_BOTTOM, person)
    file.parentFile?.mkdirs()
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    return file
}

private const val SKY_TOP = 0xFF8EC5FC.toInt()
private const val SKY_BOTTOM = 0xFFE0C3FC.toInt()
private const val PERSON = 0xFFFDF6EC.toInt()
private const val HEAD_Y = 0.4f
private const val HEAD_RADIUS = 0.18f
private const val SHOULDER_LEFT = 0.18f
private const val SHOULDER_TOP = 0.62f
private const val SHOULDER_BOTTOM = 1.2f
private const val JPEG_QUALITY = 90
