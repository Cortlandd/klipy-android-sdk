package com.klipy.sdk.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for small model helpers like [MediaItem.isAD].
 */
class MediaModelExtensionsTest {

    @Test
    fun `isAD returns true only for ad media type`() {
        val meta = MetaData(
            url = "https://cdn.example.com/ad",
            width = 300,
            height = 250
        )

        val adItem = MediaItem(
            id = "ad-1",
            title = null,
            blurPreview = null,
            lowQualityMetaData = meta,
            highQualityMetaData = null,
            mediaType = MediaType.AD
        )

        val gifItem = MediaItem(
            id = "gif-1",
            title = "Gif",
            blurPreview = null,
            lowQualityMetaData = meta,
            highQualityMetaData = null,
            mediaType = MediaType.GIF
        )

        assertTrue(adItem.isAD())
        assertFalse(gifItem.isAD())
    }
}
