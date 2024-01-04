package com.example.confessme.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.confessme.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ShareHelper(private val context: Context) {

    private fun generateImage(answeredUserName: String): Bitmap {
        val maxWidth = 500
        val maxHeight = 500

        val introText = context.getString(R.string.confess_me_intro)
        val requestText = context.getString(R.string.confess_me_request)
        val usernameText = context.getString(R.string.confess_me_username) + "@$answeredUserName"

        val bitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val shader = LinearGradient(
            0f, 0f, 0f, maxHeight.toFloat(),
            intArrayOf(Color.WHITE, ContextCompat.getColor(context, R.color.confessmered)),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader

        canvas.drawRect(0f, 0f, maxWidth.toFloat(), maxHeight.toFloat(), paint)

        paint.shader = null
        paint.textSize = 30f
        paint.typeface = Typeface.DEFAULT_BOLD

        val introTextRect = Rect()
        paint.getTextBounds(introText, 0, introText.length, introTextRect)

        val requestTextRect = Rect()
        paint.getTextBounds(requestText, 0, requestText.length, requestTextRect)

        val usernameTextRect = Rect()
        paint.getTextBounds(usernameText, 0, usernameText.length, usernameTextRect)

        val introTextX = (maxWidth - introTextRect.width()) / 2f
        val introTextY = (maxHeight - (introTextRect.height() * 4)) / 2f + introTextRect.height()

        val requestTextX = (maxWidth - requestTextRect.width()) / 2f
        val requestTextY = introTextY + introTextRect.height() + 20f

        val whisperBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.whisper)
        val scaledWhisper = Bitmap.createScaledBitmap(whisperBitmap, 75, 75, true)

        val whisperX = (maxWidth - scaledWhisper.width) / 2f
        val whisperY = introTextY - scaledWhisper.height - 40f
        canvas.drawBitmap(scaledWhisper, whisperX, whisperY, paint)

        paint.color = ContextCompat.getColor(context, R.color.confessmered)
        canvas.drawText(introText, introTextX, introTextY, paint)
        canvas.drawText(requestText, requestTextX, requestTextY, paint)

        paint.shader = null
        paint.textSize = 25f
        paint.typeface = Typeface.DEFAULT_BOLD
        val usernameTextX = (maxWidth - usernameTextRect.width()) / 2f
        val usernameTextY = requestTextY + requestTextRect.height() + 20f
        paint.color = Color.BLACK
        canvas.drawText(usernameText, usernameTextX, usernameTextY, paint)

        return bitmap
    }

    private fun saveBitmapToStorage(bitmap: Bitmap): Uri? {
        val imagesFolder = File(context.cacheDir, "images")
        imagesFolder.mkdirs()

        val file = File(imagesFolder, "shared_image.png")
        try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )
    }

    fun shareTextAndImage(confessionText: String, answerText: String,
                                  answerFromUsername: String, answeredUserName: String) {
        val emojiSpeech = "\uD83D\uDDE3"
        val emojiEar = "\uD83D\uDC42"

        val generatedBitmap = generateImage(answeredUserName)
        val imageUri = saveBitmapToStorage(generatedBitmap)

        val shareMessage = "$emojiSpeech$emojiEar $answerFromUsername:\n$confessionText\n\n$emojiSpeech\n$answerText"

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/*"
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)

        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.nerede_payla_acaks_n))
        if (shareIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
        }
    }

    fun shareImage(username: String) {
        val generatedBitmap = generateImage(username)
        val imageUri = saveBitmapToStorage(generatedBitmap)

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/*"
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)

        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.nerede_payla_acaks_n))
        if (shareIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
        }
    }
}
