package com.klipy.sdk.data

import com.klipy.sdk.model.MediaType
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for mapping ad DTOs into [MediaItem]s.
 */
class MediaItemMapperAdTest {

    private val mapper = MediaItemMapperImpl()

    @Test
    fun `mapToDomain maps ad dto into ad media item with generated id`() {
        val dto = MediaItemDto.AdMediaItemDto(
            width = 300,
            height = 250,
            content = "https://ads.example.com/creative.html",
            type = "ad"
        )

        val result = mapper.mapToDomain(dto)
        assertNotNull(result)
        requireNotNull(result)

        assertTrue("Id must start with ad-", result.id.startsWith("ad-"))
        assertEquals(MediaType.AD, result.mediaType)
        assertNull(result.title)
        assertNull(result.blurPreview)

        assertNotNull(result.lowQualityMetaData)
        assertNull(result.highQualityMetaData)

        requireNotNull(result.lowQualityMetaData)
        assertEquals("https://ads.example.com/creative.html", result.lowQualityMetaData!!.url)
        assertEquals(300, result.lowQualityMetaData!!.width)
        assertEquals(250, result.lowQualityMetaData!!.height)
    }

    @Test
    fun `mapToDomain returns null when ad dto is missing required metadata`() {
        val dto = MediaItemDto.AdMediaItemDto(
            width = 300,
            height = null,
            content = null,
            type = "ad"
        )

        assertNull(mapper.mapToDomain(dto))
    }
}
