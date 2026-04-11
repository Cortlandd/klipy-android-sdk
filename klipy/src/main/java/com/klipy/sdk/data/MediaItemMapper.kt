package com.klipy.sdk.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.MetaData
import java.util.UUID

/**
 * Maps network DTOs into SDK domain models.
 */
internal interface MediaItemMapper {
    fun mapToDomain(data: MediaItemDto): MediaItem?
}

internal class MediaItemMapperImpl : MediaItemMapper {

    override fun mapToDomain(data: MediaItemDto): MediaItem? {
        return when (data) {
            is MediaItemDto.ClipMediaItemDto -> {
                val slug = data.slug?.takeIf { it.isNotBlank() } ?: return null
                val selectorMeta = data.fileMeta?.gif?.toDomain(data.file?.gif)
                val previewMeta = data.fileMeta?.mp4?.toDomain(data.file?.mp4)
                val lowMeta = selectorMeta ?: previewMeta
                val highMeta = previewMeta ?: selectorMeta

                if (lowMeta == null && highMeta == null) return null

                MediaItem(
                    id = slug,
                    title = data.title,
                    blurPreview = data.blurPreview?.base64ToBitmap(),
                    lowQualityMetaData = lowMeta,
                    highQualityMetaData = highMeta,
                    mediaType = MediaType.CLIP,
                    tags = data.tags.orEmpty()
                )
            }

            is MediaItemDto.GeneralMediaItemDto -> {
                val slug = data.slug?.takeIf { it.isNotBlank() } ?: return null
                val normalizedType = data.type?.lowercase()

                val (lowFile, highFile) = data.file?.let { dims ->
                    val low = dims.md ?: dims.hd ?: dims.xs
                    val high = dims.hd ?: dims.md ?: dims.sm
                    low to high
                } ?: (null to null)

                val lowMetaDto = pickStaticSource(normalizedType, lowFile)
                val highMetaDto = pickStaticSource(normalizedType, highFile)
                val lowMeta = lowMetaDto?.toDomain()
                val highMeta = highMetaDto?.toDomain()
                val resolvedLowMeta = lowMeta ?: highMeta
                val resolvedHighMeta = highMeta ?: lowMeta

                if (resolvedLowMeta == null && resolvedHighMeta == null) return null

                MediaItem(
                    id = slug,
                    title = data.title,
                    blurPreview = data.blurPreview?.base64ToBitmap(),
                    lowQualityMetaData = resolvedLowMeta,
                    highQualityMetaData = resolvedHighMeta,
                    mediaType = when (normalizedType) {
                        "gif" -> MediaType.GIF
                        "sticker" -> MediaType.STICKER
                        "meme", "static-meme", "static-memes" -> MediaType.MEME
                        else -> MediaType.GIF
                    },
                    tags = data.tags.orEmpty()
                )
            }

            is MediaItemDto.AdMediaItemDto -> {
                val meta = createMetaData(
                    url = data.content,
                    width = data.width,
                    height = data.height
                ) ?: return null

                MediaItem(
                    id = "ad-${UUID.randomUUID()}",
                    title = null,
                    blurPreview = null,
                    lowQualityMetaData = meta,
                    highQualityMetaData = null,
                    mediaType = MediaType.AD
                )
            }
        }
    }

    private fun pickStaticSource(
        type: String?,
        file: FileTypesDto?
    ): FileMetaDataDto? {
        if (file == null) return null

        return when (type) {
            "meme", "static-meme", "static-memes" -> file.png ?: file.jpg ?: file.webp ?: file.gif
            "sticker" -> file.gif ?: file.png ?: file.jpg ?: file.webp
            "gif" -> file.gif ?: file.webp
            else -> file.gif ?: file.webp ?: file.png ?: file.jpg
        }
    }

    private fun FileMetaDataDto.toDomain(overrideUrl: String? = null): MetaData? =
        createMetaData(
            url = overrideUrl ?: url,
            width = width,
            height = height
        )

    private fun createMetaData(
        url: String?,
        width: Int?,
        height: Int?
    ): MetaData? {
        val resolvedUrl = url?.takeIf { it.isNotBlank() } ?: return null
        val resolvedWidth = width?.takeIf { it > 0 } ?: return null
        val resolvedHeight = height?.takeIf { it > 0 } ?: return null

        return MetaData(
            url = resolvedUrl,
            width = resolvedWidth,
            height = resolvedHeight
        )
    }
}

/**
 * Convert a BASE64 image string (with data URI prefix) to [Bitmap].
 */
internal fun String.base64ToBitmap(): Bitmap? {
    return try {
        val commaIndex = indexOf(',')
        val base64Part = if (commaIndex >= 0) substring(commaIndex + 1) else this
        val decoded: ByteArray = Base64.decode(base64Part, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
    } catch (_: Exception) {
        null
    }
}
