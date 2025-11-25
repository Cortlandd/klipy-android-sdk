package com.klipy.sdk.data

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Connection
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Tests for [AdDimensionSanitizerInterceptor].
 */
class AdDimensionSanitizerInterceptorTest {

    private class RecordingChain(initialUrl: HttpUrl) : Interceptor.Chain {
        private val originalRequest: Request = Request.Builder()
            .url(initialUrl)
            .build()

        lateinit var proceededRequest: Request

        override fun request(): Request = originalRequest

        override fun proceed(request: Request): Response {
            proceededRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }

        override fun call(): Call {
            return object : Call {
                override fun request(): Request = originalRequest
                override fun execute(): Response = proceed(originalRequest)
                override fun enqueue(responseCallback: Callback) {
                    responseCallback.onResponse(this, proceed(originalRequest))
                }
                override fun cancel() {}
                override fun isExecuted(): Boolean = false
                override fun isCanceled(): Boolean = false
                override fun timeout(): Timeout = Timeout.NONE
                override fun clone(): Call = this
            }
        }

        override fun connection(): Connection? = null

        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    @Test
    fun `interceptor applies fallback values when ad dimensions are missing or non positive`() {
        val url = "https://api.klipy.com/gifs/search?ad-min-width=0&ad-max-width=&ad-min-height=-1"
            .toHttpUrl()

        val chain = RecordingChain(url)
        val interceptor = AdDimensionSanitizerInterceptor()

        interceptor.intercept(chain)

        val newUrl = chain.proceededRequest.url

        assertEquals("50", newUrl.queryParameter("ad-min-width"))
        assertEquals("200", newUrl.queryParameter("ad-max-width"))
        assertEquals("50", newUrl.queryParameter("ad-min-height"))
        // not present originally -> should be set to default
        assertEquals("200", newUrl.queryParameter("ad-max-height"))
    }

    @Test
    fun `interceptor keeps existing positive ad dimension values`() {
        val url =
            "https://api.klipy.com/stickers/trending?ad-min-width=80&ad-max-width=250&ad-min-height=90&ad-max-height=300"
                .toHttpUrl()

        val chain = RecordingChain(url)
        val interceptor = AdDimensionSanitizerInterceptor()

        interceptor.intercept(chain)

        val newUrl = chain.proceededRequest.url

        assertEquals("80", newUrl.queryParameter("ad-min-width"))
        assertEquals("250", newUrl.queryParameter("ad-max-width"))
        assertEquals("90", newUrl.queryParameter("ad-min-height"))
        assertEquals("300", newUrl.queryParameter("ad-max-height"))
    }
}
