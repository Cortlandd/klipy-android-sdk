package com.klipy.sdk.model

import android.graphics.Bitmap

/**
 * Supported media types.
 */
enum class MediaType(val title: String) {
    GIF("GIFs"),
    CLIP("Clips"),
    STICKER("Stickers"),
    AD("Ads")
}

/** Optional helper if you want singular names for UI labels. */
fun MediaType.singularName(): String = when (this) {
    MediaType.GIF -> "GIF"
    MediaType.STICKER -> "Sticker"
    MediaType.CLIP -> "Clip"
    MediaType.AD -> "Ad"
}

/**
 * Category metadata displayed in category pickers.
 * e.g. title = "love", url = preview image for that category.
 */
data class Category(
    val title: String,
    val url: String
)

/** Convenience: is this the "recent" pseudo-category? */
fun Category.isRecent(): Boolean = title.equals("recent", ignoreCase = true)

/**
 * A page of media data.
 * - [mediaItems] list will contain both media and ad items.
 * - [itemMinWidth] may be used to determine minimum column width in a grid.
 * - [adMaxResizePercentage] is the max up-scale factor for ads (0..1).
 */
data class MediaData(
    val mediaItems: List<MediaItem>,
    val itemMinWidth: Int,
    val adMaxResizePercentage: Float
) {
    companion object {
        val EMPTY = MediaData(emptyList(), 0, 0f)
    }
}

/**
 * A single media item that you can display or play in your app.
 *
 * - GIF/STICKER:
 *   - [lowQualityMetaData] is usually a small GIF/WebP.
 *   - [highQualityMetaData] is higher resolution when available.
 *
 * - CLIP:
 *   - [lowQualityMetaData] is used as "selector" (e.g. gif thumb).
 *   - [highQualityMetaData] is usually MP4/WebP playback.
 *
 * - AD:
 *   - [mediaType] = [MediaType.AD]
 *   - [lowQualityMetaData] only, representing the ad asset.
 */
data class MediaItem(
    val id: String,
    val title: String?,
    val placeHolder: Bitmap?,
    val lowQualityMetaData: MetaData?,
    val highQualityMetaData: MetaData?,
    val mediaType: MediaType
)

/** Simple metadata describing a single media file variant. */
data class MetaData(
    val url: String,
    val width: Int,
    val height: Int
)

fun MediaItem.isAD(): Boolean = mediaType == MediaType.AD