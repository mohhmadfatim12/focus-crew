package com.focuscrew.app.ui.apps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppIcon(
    drawable: Drawable?,
    appName: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(drawable) {
        drawable?.let { d ->
            if (d is BitmapDrawable && d.bitmap != null) {
                d.bitmap
            } else {
                val width = if (d.intrinsicWidth > 0) d.intrinsicWidth else 96
                val height = if (d.intrinsicHeight > 0) d.intrinsicHeight else 96
                val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(b)
                d.setBounds(0, 0, canvas.width, canvas.height)
                d.draw(canvas)
                b
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "$appName icon",
            modifier = modifier
                .size(38.dp)
                .clip(RoundedCornerShape(9.dp))
        )
    } else {
        // Fallback letter avatar
        Box(
            modifier = modifier
                .size(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
