package com.klipy.sdk

import android.content.Context
import com.google.gson.GsonBuilder
import com.klipy.sdk.data.*
import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaData
import com.klipy.sdk.model.MediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Entry point for creating a Klipy client / repository.
 */
object KlipySdk {

    /**
     * Create a new [KlipyRepository] instance.
     *
     * @param context Android context; will be converted to applicationContext internally.
     * @param secretKey Your Klipy API key (secret key segment in the base URL).
     * @param baseApiUrl Base API URL up to but not including the secret key.
     *        Default: "https://api.klipy.com/api/v1/"
     * @param enableLogging If true, configures OkHttp's HttpLoggingInterceptor with BASIC level.
     */
    @JvmStatic
    fun create(
        context: Context,
        secretKey: String,
        baseApiUrl: String = "https://api.klipy.com/api/v1/",
        enableLogging: Boolean = false
    ): KlipyRepository {
        val appContext = context.applicationContext

        val advertisingInfoProvider = AdvertisingInfoProviderImpl(appContext)
        val deviceInfoProvider = DeviceInfoProviderImpl(appContext)
        val screenMeasurementsProvider = ScreenMeasurementsProviderImpl(appContext)

        val adsInterceptor = AdsQueryParametersInterceptor(
            deviceInfoProvider = deviceInfoProvider,
            screenMeasurementsProvider = screenMeasurementsProvider,
            advertisingInfoProvider = advertisingInfoProvider
        )

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(adsInterceptor)
            .addInterceptor(AdDimensionSanitizerInterceptor())
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)

        if (enableLogging) {
            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }

        val client = clientBuilder.build()

        val gson = GsonBuilder()
            .registerTypeAdapter(MediaItemDto::class.java, MediaItemDtoDeserializer())
            .create()

        val normalizedBase = if (baseApiUrl.endsWith("/")) baseApiUrl else "$baseApiUrl/"
        val baseWithKey = normalizedBase + secretKey.trimEnd('/') + "/"

        val retrofit = Retrofit.Builder()
            .baseUrl(baseWithKey)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val gifService = retrofit.create(GifService::class.java)
        val stickersService = retrofit.create(StickersService::class.java)
        val clipsService = retrofit.create(ClipsService::class.java)

        val apiCallHelper = ApiCallHelper()
        val mapper = MediaItemMapperImpl()

        val gifsDataSource = MediaDataSourceImpl(apiCallHelper, gifService, mapper, deviceInfoProvider)
        val stickersDataSource = MediaDataSourceImpl(apiCallHelper, stickersService, mapper, deviceInfoProvider)
        val clipsDataSource = MediaDataSourceImpl(apiCallHelper, clipsService, mapper, deviceInfoProvider)

        val selector = MediaDataSourceSelectorImpl(
            gifsDataSource = gifsDataSource,
            stickersDataSource = stickersDataSource,
            clipsDataSource = clipsDataSource
        )

        return KlipyRepositoryImpl(selector)
    }
}

/**
 * Concrete implementation of [KlipyRepository], delegating to [MediaDataSourceSelector].
 */
private class KlipyRepositoryImpl(
    private val mediaDataSourceSelector: MediaDataSourceSelector
) : KlipyRepository {

    override suspend fun getAvailableMediaTypes(): List<MediaType> =
        listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP)

    override suspend fun getCategories(mediaType: MediaType): Result<List<Category>> {
        return mediaDataSourceSelector.getDataSource(mediaType).getCategories()
    }

    override suspend fun getMedia(
        mediaType: MediaType,
        filter: String
    ): Result<MediaData> {
        return mediaDataSourceSelector.getDataSource(mediaType).getMediaData(filter)
    }

    override suspend fun triggerShare(mediaType: MediaType, slug: String): Result<Unit> {
        return mediaDataSourceSelector.getDataSource(mediaType)
            .triggerShare(slug)
            .map { Unit }
    }

    override suspend fun triggerView(mediaType: MediaType, slug: String): Result<Unit> {
        return mediaDataSourceSelector.getDataSource(mediaType)
            .triggerView(slug)
            .map { Unit }
    }

    override suspend fun report(
        mediaType: MediaType,
        slug: String,
        reason: String
    ): Result<Unit> {
        return mediaDataSourceSelector.getDataSource(mediaType)
            .report(slug, reason)
            .map { Unit }
    }

    override suspend fun hideFromRecent(
        mediaType: MediaType,
        slug: String
    ): Result<Unit> {
        return mediaDataSourceSelector.getDataSource(mediaType)
            .hideFromRecent(slug)
            .map { Unit }
    }

    override fun reset(mediaType: MediaType) {
        mediaDataSourceSelector.getDataSource(mediaType).reset()
    }
}