package com.klipy.sdk.data

import com.klipy.sdk.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for mapping clip DTOs into [MediaItem]s.
 */
class MediaItemMapperClipTest {

    private val mapper = MediaItemMapperImpl()

    @Test
    fun `mapToDomain maps clip dto into clip media item`() {
        val dto = MediaItemDto.ClipMediaItemDto(
            slug = "awesome-clip",
            title = "Awesome clip",
            placeHolder = null,
            fileMeta = FileTypesDto(
                gif = FileMetaDataDto(
                    url = "https://cdn.example.com/clip/selector.gif",
                    width = 200,
                    height = 150
                ),
                mp4 = FileMetaDataDto(
                    url = "https://cdn.example.com/clip/preview.mp4",
                    width = 400,
                    height = 300
                )
            ),
            file = ClipFileDto(
                gif = "https://cdn.example.com/clip/selector.gif",
                mp4 = "https://cdn.example.com/clip/preview.mp4",
                webp = null
            ),
            type = "clip"
        )

        val result = mapper.mapToDomain(dto)

        assertEquals("awesome-clip", result.id)
        assertEquals(MediaType.CLIP, result.mediaType)
        assertEquals("Awesome clip", result.title)
        assertNull(result.placeHolder)

        requireNotNull(result.lowQualityMetaData)
        requireNotNull(result.highQualityMetaData)

        assertEquals("https://cdn.example.com/clip/selector.gif", result.lowQualityMetaData!!.url)
        assertEquals(200, result.lowQualityMetaData!!.width)
        assertEquals(150, result.lowQualityMetaData!!.height)

        assertEquals("https://cdn.example.com/clip/preview.mp4", result.highQualityMetaData!!.url)
        assertEquals(400, result.highQualityMetaData!!.width)
        assertEquals(300, result.highQualityMetaData!!.height)
    }
}
