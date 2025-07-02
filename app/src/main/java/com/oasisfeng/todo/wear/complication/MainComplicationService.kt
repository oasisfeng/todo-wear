package com.oasisfeng.todo.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PhotoImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.oasisfeng.todo.wear.data.TodoItem
import com.oasisfeng.todo.wear.data.TodoRepository
import com.oasisfeng.todo.wear.presentation.MainActivity

class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        Toast.makeText(this, "getPreview: $type", Toast.LENGTH_SHORT).show()
//        if (type != ComplicationType.PHOTO_IMAGE) return null
        return createComplicationData(TodoRepository.getRecentTodos())
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        if (request.complicationType != ComplicationType.PHOTO_IMAGE) {
            return NoDataComplicationData()
        }
        val todos = TodoRepository.getRecentTodos()
        return createComplicationData(todos)
    }

    private fun createComplicationData(todos: List<TodoItem>): ComplicationData {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val drawable = TodoListDrawable(todos)
        val bitmap = drawable.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)

        return PhotoImageComplicationData.Builder(
            photoImage = Icon.createWithBitmap(bitmap),
            contentDescription = PlainComplicationText.Builder("Todo List").build()
        ).setTapAction(pendingIntent).build()
    }

    private class TodoListDrawable(private val todos: List<TodoItem>) : Drawable() {

        private val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        }
        private val completedTextPaint = Paint(textPaint).apply {
            color = Color.GRAY
            isStrikeThruText = true
        }
        private val bulletPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        override fun getIntrinsicWidth(): Int = 300
        override fun getIntrinsicHeight(): Int = 300

        override fun draw(canvas: Canvas) {
            val bounds = bounds
            val width = bounds.width()
            val height = bounds.height()
            val columnWidth = width / 2
            val itemHeight = 24
            val bulletRadius = 3f
            val textOffset = 12f

            var x = 0
            var y = itemHeight
            var column = 0

            for (todo in todos) {
                if (y > height) {
                    if (column == 0) {
                        column = 1
                        x = columnWidth
                        y = itemHeight
                    } else {
                        break // No more space
                    }
                }

                val paint = if (todo.completed) completedTextPaint else textPaint
                canvas.drawCircle(x + bulletRadius + 4, y - textPaint.textSize / 2 + 4, bulletRadius, bulletPaint)
                canvas.drawText(todo.title, x + textOffset + 4, y.toFloat(), paint)

                y += itemHeight
            }
        }

        override fun setAlpha(alpha: Int) {
            textPaint.alpha = alpha
            completedTextPaint.alpha = alpha
            bulletPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            textPaint.colorFilter = colorFilter
            completedTextPaint.colorFilter = colorFilter
            bulletPaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}