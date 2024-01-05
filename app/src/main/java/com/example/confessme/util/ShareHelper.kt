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
import android.graphics.RectF
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

    private fun drawTextInBox(
        canvas: Canvas, text: String, x: Float, y: Float, width: Float, height: Float,
        backgroundColor: Int, textColor: Int, paint: Paint, textSize: Float, radius: Float
    ) {
        val rect = RectF(x, y, x + width, y + height)
        paint.color = backgroundColor
        canvas.drawRoundRect(rect, radius, radius, paint)

        paint.color = textColor
        paint.textSize = textSize
        canvas.drawText(text, x + 10f, y + height / 2f + textSize / 2f, paint)
    }

    private fun generateShareImageWithText(confessionText: String, answerText: String, answeredUserName: String): Bitmap {
        val maxWidth = 500
        val maxHeight = 500

        val introText = context.getString(R.string.confess_me_intro)
        val requestText = context.getString(R.string.confess_me_request)
        val usernameLabelText = context.getString(R.string.confess_me_username)
        val usernameText = "@$answeredUserName"

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
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = ContextCompat.getColor(context, R.color.confessmered)

        val whisperBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.whisper)
        val scaledWhisper = Bitmap.createScaledBitmap(whisperBitmap, 75, 75, true)

        val whisperX = (maxWidth - scaledWhisper.width) / 2f
        val whisperY = 20f 

        canvas.drawBitmap(scaledWhisper, whisperX, whisperY, paint)

        paint.textSize = 20f 
        val introTextX = (maxWidth - paint.measureText(introText)) / 2f
        val introTextY = whisperY + scaledWhisper.height + 20f 
        canvas.drawText(introText, introTextX, introTextY, paint)

        val requestTextX = (maxWidth - paint.measureText(requestText)) / 2f
        val requestTextY = introTextY + 20f 
        canvas.drawText(requestText, requestTextX, requestTextY, paint)

        paint.textSize = 16f 
        paint.color = Color.BLACK
        val usernameLabelTextX = (maxWidth - paint.measureText(usernameLabelText)) / 2f
        val usernameLabelTextY = requestTextY + 20f 
        canvas.drawText(usernameLabelText, usernameLabelTextX, usernameLabelTextY, paint)

        paint.textSize = 16f 
        val usernameTextX = (maxWidth - paint.measureText(usernameText)) / 2f
        val usernameTextY = usernameLabelTextY + paint.textSize
        canvas.drawText(usernameText, usernameTextX, usernameTextY, paint)

        val confession = confessionText
        val answer = answerText

        val confessBoxHeight = 80f 
        val confessBoxY = usernameTextY + 40f
        val answerBoxY = confessBoxY + confessBoxHeight + 10f

        val cornerRadius = 8f

        drawTextInBox(
            canvas, confession, 10f, confessBoxY, maxWidth.toFloat() - 20f, confessBoxHeight,
            ContextCompat.getColor(context, R.color.confessmeredblur), Color.WHITE, paint, 14f, cornerRadius
        )
        drawTextInBox(
            canvas, answer, 10f, answerBoxY, maxWidth.toFloat() - 20f, confessBoxHeight,
            ContextCompat.getColor(context, R.color.confessmered), Color.WHITE, paint, 14f, cornerRadius
        )

        return bitmap
    }

    private fun generateImage(answeredUserName: String): Bitmap {
        val maxWidth = 500
        val maxHeight = 500

        val introText = context.getString(R.string.confess_me_intro)
        val requestText = context.getString(R.string.confess_me_request)
        val usernameLabelText = context.getString(R.string.confess_me_username)
        val usernameText = "@$answeredUserName"

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
        paint.color = ContextCompat.getColor(context, R.color.confessmered)

        val introTextRect = Rect()
        paint.getTextBounds(introText, 0, introText.length, introTextRect)

        val requestTextRect = Rect()
        paint.getTextBounds(requestText, 0, requestText.length, requestTextRect)

        canvas.drawText(introText, (maxWidth - paint.measureText(introText)) / 2f, (maxHeight + introTextRect.height()) / 2f - requestTextRect.height(), paint)
        canvas.drawText(requestText, (maxWidth - paint.measureText(requestText)) / 2f, (maxHeight + introTextRect.height()) / 2f, paint)

        val whisperBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.whisper)
        val scaledWhisper = Bitmap.createScaledBitmap(whisperBitmap, 75, 75, true)

        val whisperX = (maxWidth - scaledWhisper.width) / 2f
        val whisperY = (maxHeight - (introTextRect.height() * 4)) / 2f - scaledWhisper.height - 10f
        canvas.drawBitmap(scaledWhisper, whisperX, whisperY, paint)

        paint.color = Color.BLACK

        paint.textSize = 20f
        val usernameLabelTextX = (maxWidth - paint.measureText(usernameLabelText)) / 2f
        val usernameLabelTextY = (maxHeight + introTextRect.height()) / 2f + requestTextRect.height() + 5f
        canvas.drawText(usernameLabelText, usernameLabelTextX, usernameLabelTextY, paint)
        paint.textSize = 25f
        val usernameTextX = (maxWidth - paint.measureText(usernameText)) / 2f
        val usernameTextY = usernameLabelTextY + paint.textSize + 5f
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

        val generatedBitmap = generateShareImageWithText(
            confessionText = "$emojiSpeech$emojiEar $answerFromUsername:\n" +
                    "$confessionText",
            answerText = "$emojiSpeech\n" +
                    "$answerText",
            answeredUserName = answeredUserName)
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

    fun shareImage(answeredUserName: String) {
        val generatedBitmap = generateImage(answeredUserName = answeredUserName)
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
