package com.klipy.sdk.data

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Ensures ad dimension query parameters are valid (>= 1) so the API
 * does not return 422 for things like `ad-max-width = 0`.
 */
internal class AdDimensionSanitizerInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val originalUrl = original.url

        val urlBuilder = originalUrl.newBuilder()

        fun fixIntParam(name: String, fallback: Int) {
            val raw = originalUrl.queryParameter(name)
            val value = raw?.toIntOrNull() ?: 0
            if (value <= 0) {
                urlBuilder.setQueryParameter(name, fallback.toString())
            }
        }

        // Clamp all ad dims to sane defaults if missing/zero
        fixIntParam("ad-min-width", 50)
        fixIntParam("ad-max-width", 200)
        fixIntParam("ad-min-height", 50)
        fixIntParam("ad-max-height", 200)

        val newRequest = original.newBuilder()
            .url(urlBuilder.build())
            .build()

        return chain.proceed(newRequest)
    }
}