package com.klipy.klipy_ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AdCellSizingTest {

    @Test
    fun `ad height uses the ad aspect ratio when it is in bounds`() {
        val height = calculateAdCellHeightPx(
            containerWidthPx = 1080,
            metaWidthPx = 320,
            metaHeightPx = 50,
            density = 2f
        )

        assertEquals(168, height)
    }

    @Test
    fun `ad height is clamped to minimum height`() {
        val height = calculateAdCellHeightPx(
            containerWidthPx = 200,
            metaWidthPx = 1200,
            metaHeightPx = 20,
            density = 2f
        )

        assertEquals(144, height)
    }

    @Test
    fun `ad height is clamped to maximum height`() {
        val height = calculateAdCellHeightPx(
            containerWidthPx = 1600,
            metaWidthPx = 320,
            metaHeightPx = 400,
            density = 2f
        )

        assertEquals(360, height)
    }

    @Test
    fun `ad height guards against invalid metadata dimensions`() {
        val height = calculateAdCellHeightPx(
            containerWidthPx = 300,
            metaWidthPx = 0,
            metaHeightPx = 0,
            density = 2f
        )

        assertEquals(144, height)
    }
}
