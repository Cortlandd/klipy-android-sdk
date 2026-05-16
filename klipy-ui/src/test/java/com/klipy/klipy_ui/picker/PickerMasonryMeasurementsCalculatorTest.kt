package com.klipy.klipy_ui.picker

import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.MetaData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PickerMasonryMeasurementsCalculatorTest {

    @Before
    fun setUp() {
        PickerMasonryMeasurementsCalculator.reset()
    }

    @Test
    fun `ad after the first two items is deferred to a later row`() {
        val rows = PickerMasonryMeasurementsCalculator.createRows(
            items = listOf(
                mediaItem("gif-1", MediaType.GIF, 120, 120),
                mediaItem("gif-2", MediaType.GIF, 120, 120),
                mediaItem("ad-1", MediaType.AD, 320, 50),
                mediaItem("gif-3", MediaType.GIF, 120, 120)
            ),
            containerWidth = 360,
            gap = 1
        )

        assertEquals(listOf("gif-1", "gif-2"), rows.first().map { it.mediaItem.id })
        assertTrue(rows.drop(1).flatten().any { it.mediaItem.id == "ad-1" })
    }

    @Test
    fun `ad in the first two positions stays in the same row`() {
        val rows = PickerMasonryMeasurementsCalculator.createRows(
            items = listOf(
                mediaItem("gif-1", MediaType.GIF, 120, 120),
                mediaItem("ad-1", MediaType.AD, 320, 50),
                mediaItem("gif-2", MediaType.GIF, 120, 120)
            ),
            containerWidth = 600,
            gap = 1
        )

        assertTrue(rows.first().any { it.mediaItem.id == "ad-1" })
    }

    @Test
    fun `row sizing respects api item minimum width`() {
        PickerMasonryMeasurementsCalculator.itemMinWidth = 140
        PickerMasonryMeasurementsCalculator.adMaxResizePercentage = 0F

        val rows = PickerMasonryMeasurementsCalculator.createRows(
            items = listOf(
                mediaItem("gif-1", MediaType.GIF, 80, 120),
                mediaItem("ad-1", MediaType.AD, 320, 50)
            ),
            containerWidth = 480,
            gap = 1
        )

        val firstGif = rows.first().first { it.mediaItem.id == "gif-1" }
        assertTrue(firstGif.measuredWidth >= 140)
    }

    private fun mediaItem(
        id: String,
        type: MediaType,
        width: Int,
        height: Int
    ): MediaItem {
        return MediaItem(
            id = id,
            title = id,
            blurPreview = null,
            lowQualityMetaData = MetaData(
                url = "https://example.com/$id",
                width = width,
                height = height
            ),
            highQualityMetaData = null,
            mediaType = type
        )
    }
}
