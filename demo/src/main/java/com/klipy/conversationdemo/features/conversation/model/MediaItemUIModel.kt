package com.klipy.conversationdemo.features.conversation.model

import androidx.compose.runtime.Stable
import com.klipy.sdk.model.MediaItem

@Stable
data class MediaItemUIModel(
    val mediaItem: MediaItem,
    var measuredWidth: Int,
    var measuredHeight: Int
)