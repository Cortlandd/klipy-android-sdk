package com.klipy.sdk.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * General service interface – GIF, CLIP, STICKER services all share this structure.
 */
interface MediaService {
    suspend fun getCategories(): Response<CategoriesResponseDto>

    suspend fun getRecent(
        @Path("customer_id") customerId: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    suspend fun getTrending(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    suspend fun triggerShare(
        @Path("slug") slug: String,
        @Body request: TriggerViewRequestDto
    ): Response<Any>

    suspend fun triggerView(
        @Path("slug") slug: String,
        @Body request: TriggerViewRequestDto
    ): Response<Any>

    suspend fun report(
        @Path("slug") slug: String,
        @Body request: ReportRequestDto
    ): Response<Any>

    suspend fun hideFromRecent(
        @Path("customerId") customerId: String,
        @Query("slug") slug: String
    ): Response<Any>
}

/**
 * GIF API.
 */
interface GifService : MediaService {

    @GET("gifs/categories")
    override suspend fun getCategories(): Response<CategoriesResponseDto>

    @GET("gifs/recent/{customer_id}")
    @AdsQueryParameters
    override suspend fun getRecent(
        @Path("customer_id") customerId: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    @GET("gifs/trending")
    @AdsQueryParameters
    override suspend fun getTrending(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    @GET("gifs/search")
    @AdsQueryParameters
    override suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    @POST("gifs/share/{slug}")
    override suspend fun triggerShare(
        @Path("slug") slug: String,
        @Body request: TriggerViewRequestDto
    ): Response<Any>

    @POST("gifs/view/{slug}")
    override suspend fun triggerView(
        @Path("slug") slug: String,
        @Body request: TriggerViewRequestDto
    ): Response<Any>

    @POST("gifs/report/{slug}")
    override suspend fun report(
        @Path("slug") slug: String,
        @Body request: ReportRequestDto
    ): Response<Any>

    @DELETE("gifs/recent/{customerId}")
    override suspend fun hideFromRecent(
        @Path("customerId") customerId: String,
        @Query("slug") slug: String
    ): Response<Any>
}

/**
 * Stickers API.
 */
interface StickersService : MediaService {

    @GET("stickers/categories")
    override suspend fun getCategories(): Response<CategoriesResponseDto>

    @GET("stickers/recent/{customer_id}")
    @AdsQueryParameters
    override suspend fun getRecent(
        @Path("customer_id") customerId: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    @GET("stickers/trending")
    @AdsQueryParameters
    override suspend fun getTrending(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    @GET("stickers/search")
    @AdsQueryParameters
    override suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    @POST("stickers/share/{slug}")
    override suspend fun triggerShare(
        @Path("slug") slug: String,
        @Body request: TriggerViewRequestDto
    ): Response<Any>

    @POST("stickers/view/{slug}")
    override suspend fun triggerView(
        @Path("slug") slug: String,
        @Body request: TriggerViewRequestDto
    ): Response<Any>

    @POST("stickers/report/{slug}")
    override suspend fun report(
        @Path("slug") slug: String,
        @Body request: ReportRequestDto
    ): Response<Any>

    @DELETE("stickers/recent/{customerId}")
    override suspend fun hideFromRecent(
        @Path("customerId") customerId: String,
        @Query("slug") slug: String
    ): Response<Any>
}

/**
 * Clips API.
 */
interface ClipsService : MediaService {

    @GET("clips/categories")
    override suspend fun getCategories(): Response<CategoriesResponseDto>

    @GET("clips/recent/{customer_id}")
    @AdsQueryParameters
    override suspend fun getRecent(
        @Path("customer_id") customerId: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    @GET("clips/trending")
    @AdsQueryParameters
    override suspend fun getTrending(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    @GET("clips/search")
    @AdsQueryParameters
    override suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<MediaItemResponseDto>

    @POST("clips/share/{slug}")
    override suspend fun triggerShare(
        @Path("slug") slug: String,
        @Body request: TriggerViewRequestDto
    ): Response<Any>

    @POST("clips/view/{slug}")
    override suspend fun triggerView(
        @Path("slug") slug: String,
        @Body request: TriggerViewRequestDto
    ): Response<Any>

    @POST("clips/report/{slug}")
    override suspend fun report(
        @Path("slug") slug: String,
        @Body request: ReportRequestDto
    ): Response<Any>

    @DELETE("clips/recent/{customerId}")
    override suspend fun hideFromRecent(
        @Path("customerId") customerId: String,
        @Query("slug") slug: String
    ): Response<Any>
}