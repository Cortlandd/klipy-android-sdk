package com.klipy.sdk.data

import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaRequestOptions
import com.klipy.sdk.model.MediaData
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.ShareTriggerOptions
import com.klipy.sdk.model.contentFilterValue
import com.klipy.sdk.model.formatFilterValue

/**
 * Internal abstraction: a data source for a single media type (GIFs, Stickers, Clips).
 */
internal interface MediaDataSource {
    suspend fun getCategories(options: MediaRequestOptions): Result<List<Category>>
    suspend fun getMediaData(filter: String, options: MediaRequestOptions): Result<MediaData>
    suspend fun getItems(ids: List<String>, slugs: List<String>): Result<MediaData>
    suspend fun triggerShare(slug: String, options: ShareTriggerOptions): Result<Any>
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

    private val categoriesByLocale = mutableMapOf<String?, List<Category>>()
    private val defaultCustomerId: String = deviceInfoProvider.getDeviceId()

    private var currentPage: Int = INITIAL_PAGE
    private var currentRequestKey: PagingRequestKey? = null
    private var canRequestMoreData: Boolean = true

    override suspend fun getCategories(options: MediaRequestOptions): Result<List<Category>> {
        val locale = options.locale
        categoriesByLocale[locale]?.let { return Result.success(it) }

        return apiCallHelper
            .makeApiCall { mediaService.getCategories(locale) }
            .mapCatching { result ->
                val list = result.data.categories.toMutableList()
                val mapped = list.map { category ->
                    Category(
                        title = category.category,
                        query = category.query,
                        previewUrl = category.previewUrl
                    )
                }
                categoriesByLocale[locale] = mapped
                mapped
            }
    }

    override suspend fun getMediaData(
        filter: String,
        options: MediaRequestOptions
    ): Result<MediaData> {
        if (filter.isEmpty()) return Result.success(MediaData.EMPTY)

        val resolvedOptions = options.resolve(defaultCustomerId)
        val requestKey = PagingRequestKey(
            filter = filter,
            customerId = resolvedOptions.customerId,
            locale = resolvedOptions.locale,
            contentFilter = resolvedOptions.contentFilter,
            formatFilter = resolvedOptions.formatFilter
        )

        if (requestKey != currentRequestKey) {
            currentPage = INITIAL_PAGE
            canRequestMoreData = true
            currentRequestKey = requestKey
        }
        currentPage++

        if (!canRequestMoreData) {
            return Result.success(MediaData.EMPTY)
        }

        return apiCallHelper
            .makeApiCall {
                when (filter) {
                    RECENT -> mediaService.getRecent(
                        customerId = resolvedOptions.customerId,
                        page = currentPage,
                        perPage = PER_PAGE
                    )

                    TRENDING -> mediaService.getTrending(
                        page = currentPage,
                        perPage = PER_PAGE,
                        customerId = resolvedOptions.customerId,
                        locale = resolvedOptions.locale,
                        formatFilter = resolvedOptions.formatFilter
                    )

                    else -> mediaService.search(
                        query = filter,
                        page = currentPage,
                        perPage = PER_PAGE,
                        customerId = resolvedOptions.customerId,
                        locale = resolvedOptions.locale,
                        contentFilter = resolvedOptions.contentFilter,
                        formatFilter = resolvedOptions.formatFilter
                    )
                }
            }
            .mapCatching { response ->
                val data = response.data
                canRequestMoreData = data?.hasNext == true

                MediaData(
                    mediaItems = data?.data?.mapNotNull { dto ->
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
                    mediaItems = data?.data?.mapNotNull(mediaItemMapper::mapToDomain) ?: emptyList(),
                    itemMinWidth = data?.meta?.itemMinWidth ?: 0,
                    adMaxResizePercentage = (data?.meta?.adMaxResizePercentage ?: 0) / 100f
                )
            }
    }

    override fun reset() {
        currentPage = INITIAL_PAGE
        currentRequestKey = null
        canRequestMoreData = true
    }

    override suspend fun triggerShare(slug: String, options: ShareTriggerOptions): Result<Any> =
        apiCallHelper.makeApiCall {
            val resolvedOptions = options.resolve(defaultCustomerId)
            mediaService.triggerShare(
                slug,
                TriggerViewRequestDto(
                    customerId = resolvedOptions.customerId,
                    query = resolvedOptions.searchQuery
                )
            )
        }

    override suspend fun triggerView(slug: String): Result<Any> =
        apiCallHelper.makeApiCall {
            mediaService.triggerView(slug, TriggerViewRequestDto(defaultCustomerId))
        }

    override suspend fun report(slug: String, reason: String): Result<Any> =
        apiCallHelper.makeApiCall {
            mediaService.report(slug, ReportRequestDto(defaultCustomerId, reason))
        }

    override suspend fun hideFromRecent(slug: String): Result<Any> =
        apiCallHelper.makeApiCall {
            mediaService.hideFromRecent(defaultCustomerId, slug)
        }

    private fun String.toCategoryUrl(): String =
        "https://api.klipy.com/assets/images/category/${this}.png"

    private companion object {
        const val INITIAL_PAGE = 0
        const val PER_PAGE = 24
        const val RECENT = "recent"
        const val TRENDING = "trending"
    }
}

private data class ResolvedMediaRequestOptions(
    val customerId: String,
    val locale: String?,
    val contentFilter: String?,
    val formatFilter: String?
)

private fun MediaRequestOptions.resolve(defaultCustomerId: String): ResolvedMediaRequestOptions =
    ResolvedMediaRequestOptions(
        customerId = customerId ?: defaultCustomerId,
        locale = locale,
        contentFilter = contentFilterValue(),
        formatFilter = formatFilterValue()
    )

private data class ResolvedShareTriggerOptions(
    val customerId: String,
    val searchQuery: String?
)

private fun ShareTriggerOptions.resolve(defaultCustomerId: String): ResolvedShareTriggerOptions =
    ResolvedShareTriggerOptions(
        customerId = customerId ?: defaultCustomerId,
        searchQuery = searchQuery
    )

private data class PagingRequestKey(
    val filter: String,
    val customerId: String,
    val locale: String?,
    val contentFilter: String?,
    val formatFilter: String?
)

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
