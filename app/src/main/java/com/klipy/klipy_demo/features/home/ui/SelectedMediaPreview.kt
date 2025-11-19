package com.klipy.klipy_demo.features.home.ui

import android.widget.ImageView
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.klipy.sdk.model.MediaItem
import com.bumptech.glide.Glide

@Composable
fun SelectedMediaPreview(item: MediaItem) {
    val context = LocalContext.current

    val meta = item.highQualityMetaData ?: item.lowQualityMetaData
    val url = meta?.url ?: return

    AndroidView(
        modifier = Modifier
            .size(180.dp)
            .clip(RoundedCornerShape(16.dp)),
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            Glide.with(context)
                .load(url)
                .into(imageView)
        }
    )
}