package com.klipy.sdk.data

import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaData
import com.klipy.sdk.model.MediaType

/**
 * Internal abstraction: a data source for a single media type (GIFs, Stickers, Clips).
 */
internal interface MediaDataSource {
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getMediaData(filter: String): Result<MediaData>
    suspend fun getItems(ids: List<String>, slugs: List<String>): Result<MediaData>
    suspend fun triggerShare(slug: String): Result<Any>
    suspend fun triggerView(slug: String): Result<Any>
    suspend fun report(slug: String, reason: String): Result<Any>
    suspend fun hideFromRecent(slug: String): Result<Any>
    fun reset()
}

/**
 * Shared implementation for GIFs, Stickers, Clips backed by a [MediaService].
 *
 * Handles:
 * - categories caching
 * - simple paging per filter (auto-increments page on each call)
 * - mapping DTOs → domain models
 */
internal class MediaDataSourceImpl(
    private val apiCallHelper: ApiCallHelper,
    private val mediaService: MediaService,
    private val mediaItemMapper: MediaItemMapper,
    deviceInfoProvider: DeviceInfoProvider
) : MediaDataSource {

    private var categories = emptyList<Category>()
    private val customerId: String = deviceInfoProvider.getDeviceId()

    private var currentPage: Int = INITIAL_PAGE
    private var currentFilter: String = ""
    private var canRequestMoreData: Boolean = true

    override suspend fun getCategories(): Result<List<Category>> {
        if (categories.isNotEmpty()) return Result.success(categories)

        return apiCallHelper
            .makeApiCall { mediaService.getCategories() }
            .mapCatching { result ->
                val list = result.data.categories.toMutableList()
                val mapped = list.map { category ->
                    Category(
                        title = category.category,
                        query = category.query,
                        previewUrl = category.previewUrl
                    )
                }
                categories = mapped
                mapped
            }
    }

    override suspend fun getMediaData(filter: String): Result<MediaData> {
        if (filter.isEmpty()) return Result.success(MediaData.EMPTY)

        if (filter != currentFilter) {
            // Filter changed; reset paging.
            currentPage = INITIAL_PAGE
            canRequestMoreData = true
        }
        currentFilter = filter
        currentPage++

        if (!canRequestMoreData) {
            return Result.success(MediaData.EMPTY)
        }

        return apiCallHelper
            .makeApiCall {
                when (filter) {
                    RECENT -> mediaService.getRecent(
                        customerId = customerId,
                        page = currentPage,
                        perPage = PER_PAGE
                    )

                    TRENDING -> mediaService.getTrending(
                        page = currentPage,
                        perPage = PER_PAGE,
                        customerId = customerId
                    )

                    else -> mediaService.search(
                        query = filter,
                        page = currentPage,
                        perPage = PER_PAGE
                    )
                }
            }
            .mapCatching { response ->
                val data = response.data
                canRequestMoreData = data?.hasNext == true

                MediaData(
                    mediaItems = data?.data?.map { dto ->
                        mediaItemMapper.mapToDomain(dto)
                    } ?: emptyList(),
                    itemMinWidth = data?.meta?.itemMinWidth ?: 0,
                    adMaxResizePercentage = (data?.meta?.adMaxResizePercentage ?: 0) / 100f
                )
            }
            .onFailure {
                canRequestMoreData = false
            }
    }

    override suspend fun getItems(
        ids: List<String>,
        slugs: List<String>
    ): Result<MediaData> {
        if (ids.isEmpty() && slugs.isEmpty()) return Result.success(MediaData.EMPTY)

        return apiCallHelper
            .makeApiCall {
                mediaService.getItems(
                    ids = ids.joinToString(","),
                    slugs = slugs.joinToString(",")
                )
            }
            .mapCatching { response ->
                val data = response.data
                MediaData(
                    mediaItems = data?.data?.map(mediaItemMapper::mapToDomain) ?: emptyList(),
                    itemMinWidth = data?.meta?.itemMinWidth ?: 0,
                    adMaxResizePercentage = (data?.meta?.adMaxResizePercentage ?: 0) / 100f
                )
            }
    }

    override fun reset() {
        currentPage = INITIAL_PAGE
        currentFilter = ""
        canRequestMoreData = true
    }

    override suspend fun triggerShare(slug: String): Result<Any> =
        apiCallHelper.makeApiCall {
            mediaService.triggerShare(slug, TriggerViewRequestDto(customerId))
        }

    override suspend fun triggerView(slug: String): Result<Any> =
        apiCallHelper.makeApiCall {
            mediaService.triggerView(slug, TriggerViewRequestDto(customerId))
        }

    override suspend fun report(slug: String, reason: String): Result<Any> =
        apiCallHelper.makeApiCall {
            mediaService.report(slug, ReportRequestDto(customerId, reason))
        }

    override suspend fun hideFromRecent(slug: String): Result<Any> =
        apiCallHelper.makeApiCall {
            mediaService.hideFromRecent(customerId, slug)
        }

    private fun String.toCategoryUrl(): String =
        "https://api.klipy.com/assets/images/category/${this}.png"

    private companion object {
        const val INITIAL_PAGE = 0
        const val PER_PAGE = 50
        const val RECENT = "recent"
        const val TRENDING = "trending"
    }
}

/**
 * Chooses the correct [MediaDataSource] for a given [MediaType] and
 * resets paging when the type changes.
 */
internal interface MediaDataSourceSelector {
    fun getDataSource(mediaType: MediaType): MediaDataSource
}

internal class MediaDataSourceSelectorImpl(
    private val gifsDataSource: MediaDataSource,
    private val stickersDataSource: MediaDataSource,
    private val clipsDataSource: MediaDataSource,
    private val memesDataSource: MediaDataSource
) : MediaDataSourceSelector {

    private var lastMediaType: MediaType? = null

    override fun getDataSource(mediaType: MediaType): MediaDataSource {
        val ds = when (mediaType) {
            MediaType.GIF -> gifsDataSource
            MediaType.STICKER -> stickersDataSource
            MediaType.CLIP -> clipsDataSource
            MediaType.MEME -> memesDataSource
            MediaType.AD -> throw IllegalArgumentException("No datasource for AD type")
        }

        if (mediaType != lastMediaType) {
            lastMediaType = mediaType
            ds.reset()
        }

        return ds
    }
}