package com.klipy.sdk.data

import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaData
import com.klipy.sdk.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Tests for [MediaDataSourceSelectorImpl].
 */
class MediaDataSourceSelectorTest {

    private class RecordingDataSource : MediaDataSource {
        var resetCount = 0

        override suspend fun getCategories(): Result<List<Category>> {
            error("Not used in this test")
        }

        override suspend fun getMediaData(filter: String): Result<MediaData> {
            error("Not used in this test")
        }

        override suspend fun getItems(ids: List<String>, slugs: List<String>): Result<MediaData> {
            error("Not used in this test")
        }

        override suspend fun triggerShare(slug: String): Result<Any> {
            error("Not used in this test")
        }

        override suspend fun triggerView(slug: String): Result<Any> {
            error("Not used in this test")
        }

        override suspend fun report(slug: String, reason: String): Result<Any> {
            error("Not used in this test")
        }

        override suspend fun hideFromRecent(slug: String): Result<Any> {
            error("Not used in this test")
        }

        override fun reset() {
            resetCount++
        }
    }

    @Test
    fun `getDataSource returns the same instance per media type`() {
        val gif = RecordingDataSource()
        val sticker = RecordingDataSource()
        val clip = RecordingDataSource()
        val meme = RecordingDataSource()

        val selector = MediaDataSourceSelectorImpl(
            gifsDataSource = gif,
            stickersDataSource = sticker,
            clipsDataSource = clip,
            memesDataSource = meme
        )

        assertSame(gif, selector.getDataSource(MediaType.GIF))
        assertSame(sticker, selector.getDataSource(MediaType.STICKER))
        assertSame(clip, selector.getDataSource(MediaType.CLIP))
        assertSame(meme, selector.getDataSource(MediaType.MEME))
    }

    @Test
    fun `getDataSource resets datasource when type changes`() {
        val gif = RecordingDataSource()
        val sticker = RecordingDataSource()
        val clip = RecordingDataSource()
        val meme = RecordingDataSource()

        val selector = MediaDataSourceSelectorImpl(
            gifsDataSource = gif,
            stickersDataSource = sticker,
            clipsDataSource = clip,
            memesDataSource = meme
        )

        // First GIF call resets GIF
        selector.getDataSource(MediaType.GIF)
        // Same type again -> no extra reset call
        selector.getDataSource(MediaType.GIF)
        // Switch types
        selector.getDataSource(MediaType.STICKER)
        selector.getDataSource(MediaType.CLIP)
        selector.getDataSource(MediaType.MEME)

        assertEquals(1, gif.resetCount)
        assertEquals(1, sticker.resetCount)
        assertEquals(1, clip.resetCount)
        assertEquals(1, meme.resetCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getDataSource throws for AD media type`() {
        val selector = MediaDataSourceSelectorImpl(
            gifsDataSource = RecordingDataSource(),
            stickersDataSource = RecordingDataSource(),
            clipsDataSource = RecordingDataSource(),
            memesDataSource = RecordingDataSource()
        )

        selector.getDataSource(MediaType.AD)
    }
}
