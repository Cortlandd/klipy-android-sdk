@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.klipy.klipy_ui.picker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import co.kikliko.android.ads_sdk.GIFWebView
import co.kikliko.android.ads_sdk.KlipyContent
import coil.compose.rememberAsyncImagePainter
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.isAD

@Composable
internal fun PickerMediaContent(
    data: PickerMediaItemRow,
    gap: Dp,
    onMediaItemClicked: (MediaItem) -> Unit,
    onMediaItemLongClicked: (MediaItem) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(data.first().measuredHeight.dp)
            .zIndex(if (data.hasAd()) 0F else 1F),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        data.forEach {
            val mediaItem = it.mediaItem
            val itemWidth = it.measuredWidth.dp
            if (mediaItem.isAD()) {
                PickerAdMediaItem(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .zIndex(0F),
                    content = mediaItem.lowQualityMetaData?.url,
                    width = it.measuredWidth,
                    height = it.measuredHeight
                )
            } else if (mediaItem.mediaType == MediaType.CLIP) {
                PickerClipMediaItem(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .combinedClickable(
                            onClick = { onMediaItemClicked(mediaItem) },
                            onLongClick = { onMediaItemLongClicked(mediaItem) }
                        )
                        .zIndex(1F),
                    mediaItem = mediaItem
                )
            } else {
                PickerGifImage(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .combinedClickable(
                            onClick = { onMediaItemClicked(mediaItem) },
                            onLongClick = { onMediaItemLongClicked(mediaItem) }
                        )
                        .zIndex(1F),
                    key = mediaItem,
                    url = mediaItem.lowQualityMetaData?.url,
                    contentScale = ContentScale.Crop,
                    placeholder = rememberAsyncImagePainter(mediaItem.blurPreview),
                    error = rememberAsyncImagePainter(mediaItem.blurPreview)
                )
            }
        }
    }
}

@Composable
private fun PickerAdMediaItem(
    modifier: Modifier = Modifier,
    content: String?,
    width: Int,
    height: Int
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            GIFWebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = {
            it.loadContent(
                KlipyContent(
                    isWebView = false,
                    content = content.orEmpty(),
                    width = width,
                    height = height
                )
            )
        },
        onRelease = {
            it.removeAllViews()
            it.destroy()
        }
    )
}

@Composable
private fun PickerClipMediaItem(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem
) {
    Box(modifier = modifier) {
        PickerGifImage(
            modifier = Modifier.fillMaxSize(),
            key = mediaItem,
            url = mediaItem.lowQualityMetaData?.url,
            contentScale = ContentScale.Crop,
            placeholder = rememberAsyncImagePainter(mediaItem.blurPreview),
            error = rememberAsyncImagePainter(mediaItem.blurPreview)
        )
        Icon(
            modifier = Modifier
                .padding(5.dp)
                .size(16.dp)
                .align(Alignment.TopStart),
            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = "",
            tint = Color.White
        )
        mediaItem.title?.let {
            val brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black)
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush)
                    .align(Alignment.BottomStart)
                    .padding(5.dp),
                text = it,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
