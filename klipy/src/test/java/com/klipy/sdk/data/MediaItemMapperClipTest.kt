package com.klipy.sdk.data

import com.klipy.sdk.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
            blurPreview = null,
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
        assertNotNull(result)
        requireNotNull(result)

        assertEquals("awesome-clip", result.id)
        assertEquals(MediaType.CLIP, result.mediaType)
        assertEquals("Awesome clip", result.title)
        assertNull(result.blurPreview)

        requireNotNull(result.lowQualityMetaData)
        requireNotNull(result.highQualityMetaData)

        assertEquals("https://cdn.example.com/clip/selector.gif", result.lowQualityMetaData!!.url)
        assertEquals(200, result.lowQualityMetaData!!.width)
        assertEquals(150, result.lowQualityMetaData!!.height)

        assertEquals("https://cdn.example.com/clip/preview.mp4", result.highQualityMetaData!!.url)
        assertEquals(400, result.highQualityMetaData!!.width)
        assertEquals(300, result.highQualityMetaData!!.height)
    }

    @Test
    fun `mapToDomain falls back to the preview asset when clip selector data is missing`() {
        val dto = MediaItemDto.ClipMediaItemDto(
            slug = "preview-only-clip",
            title = "Preview only clip",
            fileMeta = FileTypesDto(
                mp4 = FileMetaDataDto(
                    url = "https://cdn.example.com/clip/preview.mp4",
                    width = 400,
                    height = 300
                )
            ),
            file = ClipFileDto(
                gif = null,
                mp4 = "https://cdn.example.com/clip/preview.mp4",
                webp = null
            ),
            type = "clip"
        )

        val result = mapper.mapToDomain(dto)
        assertNotNull(result)
        requireNotNull(result)

        assertEquals("preview-only-clip", result.id)
        assertEquals("https://cdn.example.com/clip/preview.mp4", result.lowQualityMetaData?.url)
        assertEquals("https://cdn.example.com/clip/preview.mp4", result.highQualityMetaData?.url)
    }
}
