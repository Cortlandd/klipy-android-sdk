package com.klipy.sdk.data

import com.klipy.sdk.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for mapping general media items (GIF + STICKER) into domain [MediaItem]s.
 */
class MediaItemMapperGifStickerTest {

    private val mapper = MediaItemMapperImpl()

    @Test
    fun `mapToDomain maps general gif dto into gif media item with hd and md urls`() {
        val dto = MediaItemDto.GeneralMediaItemDto(
            slug = "funny-cat",
            title = "Funny cat",
            blurPreview = null,
            file = DimensionsDto(
                hd = FileTypesDto(
                    gif = FileMetaDataDto(
                        url = "https://cdn.example.com/gif/hd.gif",
                        width = 400,
                        height = 300
                    )
                ),
                md = FileTypesDto(
                    gif = FileMetaDataDto(
                        url = "https://cdn.example.com/gif/md.gif",
                        width = 200,
                        height = 150
                    )
                )
            ),
            type = "gif"
        )

        val result = mapper.mapToDomain(dto)

        assertEquals("funny-cat", result.id)
        assertEquals(MediaType.GIF, result.mediaType)
        assertEquals("Funny cat", result.title)
        assertNull(result.blurPreview)

        requireNotNull(result.lowQualityMetaData)
        requireNotNull(result.highQualityMetaData)

        assertEquals("https://cdn.example.com/gif/md.gif", result.lowQualityMetaData!!.url)
        assertEquals(200, result.lowQualityMetaData!!.width)
        assertEquals(150, result.lowQualityMetaData!!.height)

        assertEquals("https://cdn.example.com/gif/hd.gif", result.highQualityMetaData!!.url)
        assertEquals(400, result.highQualityMetaData!!.width)
        assertEquals(300, result.highQualityMetaData!!.height)
    }

    @Test
    fun `mapToDomain maps general non gif dto into sticker media item`() {
        val dto = MediaItemDto.GeneralMediaItemDto(
            slug = "party-hat",
            title = "Party hat",
            blurPreview = null,
            file = DimensionsDto(
                hd = FileTypesDto(
                    gif = FileMetaDataDto(
                        url = "https://cdn.example.com/sticker/hd.webp",
                        width = 512,
                        height = 512
                    )
                )
            ),
            type = "sticker"
        )

        val result = mapper.mapToDomain(dto)

        assertEquals("party-hat", result.id)
        assertEquals(MediaType.STICKER, result.mediaType)
        assertEquals("Party hat", result.title)
        assertNull(result.blurPreview)

        requireNotNull(result.lowQualityMetaData)
        requireNotNull(result.highQualityMetaData)

        // Only HD provided – mapper should fall back to HD for both low + high
        assertEquals("https://cdn.example.com/sticker/hd.webp", result.lowQualityMetaData!!.url)
        assertEquals("https://cdn.example.com/sticker/hd.webp", result.highQualityMetaData!!.url)
        assertEquals(512, result.lowQualityMetaData!!.width)
        assertEquals(512, result.highQualityMetaData!!.width)
    }
}
