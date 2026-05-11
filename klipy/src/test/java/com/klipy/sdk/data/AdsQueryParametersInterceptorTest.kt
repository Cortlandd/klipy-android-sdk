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
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Invocation
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

class AdsQueryParametersInterceptorTest {

    private interface TestService {
        @AdsQueryParameters
        fun annotated()

        fun plain()
    }

    private class RecordingChain(
        initialUrl: HttpUrl,
        invocation: Invocation?
    ) : Interceptor.Chain {
        private val originalRequest: Request = Request.Builder()
            .url(initialUrl)
            .apply {
                if (invocation != null) {
                    tag(Invocation::class.java, invocation)
                }
            }
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
    fun `annotated requests include ad iframe parameter`() {
        val interceptor = AdsQueryParametersInterceptor(
            deviceInfoProvider = deviceInfoProvider(),
            screenMeasurementsProvider = screenMeasurementsProvider(),
            advertisingInfoProvider = advertisingInfoProvider()
        )
        val chain = RecordingChain(
            initialUrl = "https://api.klipy.com/api/v1/key/gifs/search?page=1".toHttpUrl(),
            invocation = Invocation.of(method("annotated"), emptyList<Any>())
        )

        interceptor.intercept(chain)

        assertEquals("1", chain.proceededRequest.url.queryParameter("ad-iframe"))
    }

    @Test
    fun `plain requests do not receive ad iframe parameter`() {
        val interceptor = AdsQueryParametersInterceptor(
            deviceInfoProvider = deviceInfoProvider(),
            screenMeasurementsProvider = screenMeasurementsProvider(),
            advertisingInfoProvider = advertisingInfoProvider()
        )
        val chain = RecordingChain(
            initialUrl = "https://api.klipy.com/api/v1/key/gifs/items?ids=1".toHttpUrl(),
            invocation = Invocation.of(method("plain"), emptyList<Any>())
        )

        interceptor.intercept(chain)

        assertNull(chain.proceededRequest.url.queryParameter("ad-iframe"))
    }

    private fun method(name: String): Method = TestService::class.java.getDeclaredMethod(name)

    private fun deviceInfoProvider() = object : DeviceInfoProvider {
        override fun getDeviceId(): String = "device-id"
        override fun getCarrier(): String? = "carrier"
        override fun getNetworkOperator(): String? = "310260"
        override fun getUserAgent(): String? = null
    }

    private fun screenMeasurementsProvider() = object : ScreenMeasurementsProvider {
        override var device: Measurements = Measurements(width = 1080, height = 1920)
        override var mediaSelectorContainer: Measurements =
            Measurements(width = 720, height = 480)

        override fun getDensityScaleFactor(): Float = 3f
    }

    private fun advertisingInfoProvider() = object : AdvertisingInfoProvider {
        override fun getAdvertisingId(): String? = "ifa-id"
    }
}
