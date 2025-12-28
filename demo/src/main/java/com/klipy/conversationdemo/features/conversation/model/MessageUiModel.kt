package com.klipy.conversationdemo.features.conversation.model

import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.MetaData
import java.util.UUID

sealed class MessageUiModel(
    open val id: String,
    open val isFromCurrentUser: Boolean
)

data class TextMessage(
    override val id: String = UUID.randomUUID().toString(),
    override val isFromCurrentUser: Boolean,
    val text: String
) : MessageUiModel(id, isFromCurrentUser)

data class GifMessage(
    override val id: String = UUID.randomUUID().toString(),
    override val isFromCurrentUser: Boolean,
    val url: String,
    val width: Int,
    val height: Int
) : MessageUiModel(id, isFromCurrentUser)

data class ClipMessage(
    override val id: String = UUID.randomUUID().toString(),
    override val isFromCurrentUser: Boolean,
    val url: String,
    val width: Int,
    val height: Int
) : MessageUiModel(id, isFromCurrentUser)

fun MessageUiModel.toMediaItemOrNull(): MediaItem? = when (this) {
    is GifMessage -> {
        MediaItem(
            id = id,
            title = null,
            blurPreview = null,
            lowQualityMetaData = MetaData(
                url = url,
                width = width,
                height = height
            ),
            // For GIF we usually only care about the “gif” variant;
            // you can keep highQualityMetaData null or duplicate.
            highQualityMetaData = null,
            mediaType = MediaType.GIF
        )
    }

    is ClipMessage -> {
        MediaItem(
            id = id,
            title = null,
            blurPreview = null,
            // Use thumb as low quality
            lowQualityMetaData = MetaData(
                url = url,
                width = width,
                height = height
            ),
            // Use same URL for playback; if you have a separate MP4 URL, plug it here.
            highQualityMetaData = MetaData(
                url = url,
                width = width,
                height = height
            ),
            mediaType = MediaType.CLIP
        )
    }

    else -> null
}
