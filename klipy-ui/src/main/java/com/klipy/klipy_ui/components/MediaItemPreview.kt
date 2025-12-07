package com.klipy.klipy_ui.components

import android.R
import android.net.Uri
import android.widget.ImageView
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType

@Composable
fun MediaItemPreview(item: MediaItem) {
    val context = LocalContext.current
    val meta = item.highQualityMetaData ?: item.lowQualityMetaData
    val url = meta?.url ?: return

    when (item.mediaType) {
        MediaType.CLIP -> {
            // Simple looping video with play/pause toggle
            var isPlaying by remember { mutableStateOf(true) }
            var videoView by remember { mutableStateOf<VideoView?>(null) }

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    modifier = Modifier.matchParentSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            videoView = this
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                if (isPlaying) start() else pause()
                            }
                        }
                    },
                    update = { view ->
                        videoView = view
                        if (view.tag != url) {
                            view.tag = url
                            view.setVideoURI(Uri.parse(url))
                            if (isPlaying) {
                                view.start()
                            } else {
                                view.pause()
                            }
                        } else {
                            // Sync play/pause on recomposition
                            if (isPlaying && !view.isPlaying) {
                                view.start()
                            } else if (!isPlaying && view.isPlaying) {
                                view.pause()
                            }
                        }
                    }
                )

                // 🔴 Centered play / pause overlay
                IconButton(
                    onClick = {
                        isPlaying = !isPlaying
                        videoView?.let { v ->
                            if (isPlaying) v.start() else v.pause()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    val icon = if (isPlaying) painterResource(R.drawable.ic_media_pause) else painterResource(R.drawable.ic_media_play)
                    Icon(
                        painter = icon,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White
                    )
                }
            }
        }
        else -> {
            AndroidView(
                modifier = Modifier
                    .size(240.dp)
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
    }
}