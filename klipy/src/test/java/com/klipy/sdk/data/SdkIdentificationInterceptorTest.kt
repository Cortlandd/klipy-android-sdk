package com.klipy.sdk.data

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class SdkIdentificationInterceptorTest {

    private class RecordingChain(initialRequest: Request) : Interceptor.Chain {
        private val originalRequest: Request = initialRequest

        lateinit var proceededRequest: Request

        override fun request(): Request = originalRequest

        override fun proceed(request: Request): Response {
            proceededRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody(null))
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
    fun `interceptor adds the sdk user agent when no header is present`() {
        val interceptor = SdkIdentificationInterceptor(
            deviceInfoProvider = deviceInfoProvider(userAgent = "Mozilla/5.0 Android WebView")
        )
        val chain = RecordingChain(
            Request.Builder()
                .url("https://api.klipy.com/api/v1/key/gifs/search")
                .build()
        )

        interceptor.intercept(chain)

        assertEquals(
            "klipy-android-sdk (Android; community SDK) Mozilla/5.0 Android WebView",
            chain.proceededRequest.header("User-Agent")
        )
    }

    @Test
    fun `interceptor keeps an existing user agent header`() {
        val interceptor = SdkIdentificationInterceptor(
            deviceInfoProvider = deviceInfoProvider(userAgent = "Mozilla/5.0 Android WebView")
        )
        val chain = RecordingChain(
            Request.Builder()
                .url("https://api.klipy.com/api/v1/key/gifs/search")
                .header("User-Agent", "custom-client/1.0")
                .build()
        )

        interceptor.intercept(chain)

        assertEquals("custom-client/1.0", chain.proceededRequest.header("User-Agent"))
    }

    @Test
    fun `interceptor falls back to the sdk identifier when device user agent is unavailable`() {
        val interceptor = SdkIdentificationInterceptor(
            deviceInfoProvider = deviceInfoProvider(userAgent = null)
        )
        val chain = RecordingChain(
            Request.Builder()
                .url("https://api.klipy.com/api/v1/key/gifs/search")
                .build()
        )

        interceptor.intercept(chain)

        assertEquals(
            "klipy-android-sdk (Android; community SDK)",
            chain.proceededRequest.header("User-Agent")
        )
    }

    private fun deviceInfoProvider(userAgent: String?): DeviceInfoProvider =
        object : DeviceInfoProvider {
            override fun getDeviceId(): String = "device-123"
            override fun getUserAgent(): String? = userAgent
            override fun getCarrier(): String? = null
            override fun getNetworkOperator(): String? = null
        }
}
