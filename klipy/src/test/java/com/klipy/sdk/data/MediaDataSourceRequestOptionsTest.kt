package com.klipy.sdk.data

import com.klipy.sdk.model.ContentFilter
import com.klipy.sdk.model.MediaFormat
import com.klipy.sdk.model.MediaRequestOptions
import com.klipy.sdk.model.ShareTriggerOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class MediaDataSourceRequestOptionsTest {

    private val apiCallHelper = ApiCallHelper()
    private val mapper = MediaItemMapperImpl()

    @Test
    fun `getCategories forwards locale overrides`() = runBlocking {
        val service = RecordingMediaService()
        val dataSource = dataSource(service)

        dataSource.getCategories(MediaRequestOptions(locale = "us"))

        assertEquals("us", service.lastCategoriesLocale)
    }

    @Test
    fun `search forwards the documented customer locale filter options`() = runBlocking {
        val service = RecordingMediaService()
        val dataSource = dataSource(service)

        dataSource.getMediaData(
            filter = "celebration",
            options = MediaRequestOptions(
                customerId = "user-42",
                locale = "uk",
                contentFilter = ContentFilter.HIGH,
                formatFilter = linkedSetOf(MediaFormat.GIF, MediaFormat.WEBP)
            )
        )

        assertEquals(
            SearchRequest(
                query = "celebration",
                page = 1,
                perPage = 24,
                customerId = "user-42",
                locale = "uk",
                contentFilter = "high",
                formatFilter = "gif,webp"
            ),
            service.lastSearchRequest
        )
    }

    @Test
    fun `share trigger forwards search query and custom customer id`() = runBlocking {
        val service = RecordingMediaService()
        val dataSource = dataSource(service)

        dataSource.triggerShare(
            slug = "party-time",
            options = ShareTriggerOptions(customerId = "user-7", searchQuery = "party")
        )

        assertEquals("party-time", service.lastSharedSlug)
        assertEquals(TriggerViewRequestDto(customerId = "user-7", query = "party"), service.lastShareRequest)
    }

    @Test
    fun `recent requests keep the default device id when no customer override is provided`() = runBlocking {
        val service = RecordingMediaService()
        val dataSource = dataSource(service)

        dataSource.getMediaData(filter = "recent", options = MediaRequestOptions())

        assertEquals(RecentRequest(customerId = "device-123", page = 1, perPage = 24), service.lastRecentRequest)
        assertNull(service.lastSearchRequest)
    }

    private fun dataSource(service: RecordingMediaService): MediaDataSourceImpl =
        MediaDataSourceImpl(
            apiCallHelper = apiCallHelper,
            mediaService = service,
            mediaItemMapper = mapper,
            deviceInfoProvider = object : DeviceInfoProvider {
                override fun getDeviceId(): String = "device-123"
                override fun getUserAgent(): String? = null
                override fun getCarrier(): String? = null
                override fun getNetworkOperator(): String? = null
            }
        )

    private class RecordingMediaService : MediaService {
        var lastCategoriesLocale: String? = null
        var lastSearchRequest: SearchRequest? = null
        var lastRecentRequest: RecentRequest? = null
        var lastSharedSlug: String? = null
        var lastShareRequest: TriggerViewRequestDto? = null

        override suspend fun getCategories(locale: String?): Response<CategoriesResponseDto> {
            lastCategoriesLocale = locale
            return Response.success(
                CategoriesResponseDto(
                    result = true,
                    data = CategoriesDataDto(categories = emptyList())
                )
            )
        }

        override suspend fun getRecent(
            customerId: String,
            page: Int,
            perPage: Int
        ): Response<MediaItemResponseDto> {
            lastRecentRequest = RecentRequest(customerId, page, perPage)
            return emptyMediaResponse()
        }

        override suspend fun getTrending(
            page: Int,
            perPage: Int,
            customerId: String,
            locale: String?,
            formatFilter: String?
        ): Response<MediaItemResponseDto> = emptyMediaResponse()

        override suspend fun search(
            query: String,
            page: Int,
            perPage: Int,
            customerId: String,
            locale: String?,
            contentFilter: String?,
            formatFilter: String?
        ): Response<MediaItemResponseDto> {
            lastSearchRequest = SearchRequest(
                query = query,
                page = page,
                perPage = perPage,
                customerId = customerId,
                locale = locale,
                contentFilter = contentFilter,
                formatFilter = formatFilter
            )
            return emptyMediaResponse()
        }

        override suspend fun getItems(ids: String, slugs: String): Response<MediaItemResponseDto> =
            emptyMediaResponse()

        override suspend fun triggerShare(
            slug: String,
            request: TriggerViewRequestDto
        ): Response<Any> {
            lastSharedSlug = slug
            lastShareRequest = request
            return Response.success(Any())
        }

        override suspend fun triggerView(
            slug: String,
            request: TriggerViewRequestDto
        ): Response<Any> = Response.success(Any())

        override suspend fun report(
            slug: String,
            request: ReportRequestDto
        ): Response<Any> = Response.success(Any())

        override suspend fun hideFromRecent(customerId: String, slug: String): Response<Any> =
            Response.success(Any())

        private fun emptyMediaResponse(): Response<MediaItemResponseDto> =
            Response.success(
                MediaItemResponseDto(
                    result = true,
                    data = DataDto(
                        data = emptyList(),
                        hasNext = false,
                        meta = MetaDto(itemMinWidth = 0, adMaxResizePercentage = 0)
                    )
                )
            )
    }

    private data class SearchRequest(
        val query: String,
        val page: Int,
        val perPage: Int,
        val customerId: String,
        val locale: String?,
        val contentFilter: String?,
        val formatFilter: String?
    )

    private data class RecentRequest(
        val customerId: String,
        val page: Int,
        val perPage: Int
    )
}
