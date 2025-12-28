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
    fun mapToDomain(data: MediaItemDto): MediaItem
}

internal class MediaItemMapperImpl : MediaItemMapper {

    override fun mapToDomain(data: MediaItemDto): MediaItem {
        return when (data) {
            is MediaItemDto.ClipMediaItemDto -> {
                val selectorMeta = data.file?.gif?.let { url ->
                    MetaData(
                        url = url,
                        width = data.fileMeta!!.gif!!.width!!,
                        height = data.fileMeta.gif.height!!
                    )
                }
                val previewMeta = data.file?.mp4?.let { url ->
                    MetaData(
                        url = url,
                        width = data.fileMeta!!.mp4!!.width!!,
                        height = data.fileMeta.mp4.height!!
                    )
                }

                MediaItem(
                    id = data.slug!!,
                    title = data.title,
                    blurPreview = data.blurPreview?.base64ToBitmap(),
                    lowQualityMetaData = selectorMeta,
                    highQualityMetaData = previewMeta,
                    mediaType = MediaType.CLIP
                )
            }

            is MediaItemDto.GeneralMediaItemDto -> {
                val normalizedType = data.type?.lowercase()

                val (lowFile, highFile) = data.file?.let { dims ->
                    val low = dims.md ?: dims.hd ?: dims.xs
                    val high = dims.hd ?: dims.md ?: dims.sm
                    low to high
                } ?: (null to null)

                val lowMetaDto = pickStaticSource(normalizedType, lowFile)
                val highMetaDto = pickStaticSource(normalizedType, highFile)

                MediaItem(
                    id = data.slug!!,
                    title = data.title,
                    blurPreview = data.blurPreview?.base64ToBitmap(),
                    lowQualityMetaData = lowMetaDto?.toDomain(),
                    highQualityMetaData = highMetaDto?.toDomain(),
                    mediaType = when (normalizedType) {
                        "gif" -> MediaType.GIF
                        "sticker" -> MediaType.STICKER
                        "meme", "static-meme", "static-memes" -> MediaType.MEME
                        else -> MediaType.GIF
                    }
                )
            }

            is MediaItemDto.AdMediaItemDto -> {
                val meta = MetaData(
                    url = data.content!!,
                    width = data.width!!,
                    height = data.height!!
                )
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

    private fun FileMetaDataDto.toDomain(): MetaData =
        MetaData(
            url = url!!,
            width = width!!,
            height = height!!
        )
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