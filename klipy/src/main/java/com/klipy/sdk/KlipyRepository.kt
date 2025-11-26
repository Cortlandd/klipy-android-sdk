package com.klipy.sdk

import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaData
import com.klipy.sdk.model.MediaType


/**
 * Main Klipy data access interface.
 */
interface KlipyRepository {

    /** Returns the media types that Klipy supports (GIF, STICKER, CLIP). */
    suspend fun getAvailableMediaTypes(): List<MediaType>

    /** Fetch available categories for a given media type (GIFs, Stickers, Clips). */
    suspend fun getCategories(mediaType: MediaType): Result<List<Category>>

    /** GET {GIF|Sticker|Clip|Meme} - Trending API */
    suspend fun getTrending(
        mediaType: MediaType
    ): Result<MediaData>

    /** GET {GIF|Sticker|Clip|Meme} - Search API */
    suspend fun search(
        mediaType: MediaType,
        query: String
    ): Result<MediaData>

    /** GET {GIF|Sticker|Clip|Meme} - Recent Items API [per user] */
    suspend fun getRecent(
        mediaType: MediaType
    ): Result<MediaData>

    /** GET {GIF|Sticker|Clip|Meme} - Items API (one or more items by id). */
    suspend fun getItems(
        mediaType: MediaType,
        ids: List<String>,
        slugs: List<String>
    ): Result<MediaData>

    /**
     * Fetch media items for a given type and filter.
     *
     * Filter values:
     * - "recent"   → user's recent media
     * - "trending" → trending items
     * - any other string → used as a search query
     *
     * Pagination is handled internally; each call with the same filter loads the next page
     * until the backend says there is no more data.
     */
    suspend fun getMedia(mediaType: MediaType, filter: String): Result<MediaData>

    /** Notify Klipy that a share occurred for analytics and recommendation tuning. */
    suspend fun triggerShare(mediaType: MediaType, slug: String): Result<Unit>

    /** Notify Klipy that a view occurred for analytics and recommendation tuning. */
    suspend fun triggerView(mediaType: MediaType, slug: String): Result<Unit>

    /** Report a piece of content, with a free-form reason string. */
    suspend fun report(mediaType: MediaType, slug: String, reason: String): Result<Unit>

    /** Hide a media item from the user's "recent" list. */
    suspend fun hideFromRecent(mediaType: MediaType, slug: String): Result<Unit>

    /**
     * Reset pagination for the given media type.
     * Call this when the user switches filters or you want to refresh from page 1.
     */
    fun reset(mediaType: MediaType)
}