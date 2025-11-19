package com.klipy.sdk.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.webkit.WebSettings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation
import retrofit2.Response as RetrofitResponse
import java.util.Locale

// --- API Call Helper ---

class ApiCallHelper {

    /**
     * Wraps a Retrofit [apiCall] into a [Result], handling HTTP errors and null bodies.
     */
    suspend fun <T> makeApiCall(
        apiCall: suspend () -> RetrofitResponse<T>
    ): Result<T> {
        return kotlin.runCatching {
            val response = apiCall()
            if (response.isSuccessful) {
                response.body() ?: throw EmptyResponseBodyException()
            } else {
                throw retrofit2.HttpException(response)
            }
        }
    }
}

// --- Advertising ID ---

interface AdvertisingInfoProvider {
    fun getAdvertisingId(): String?
}

class AdvertisingInfoProviderImpl(
    private val context: Context
) : AdvertisingInfoProvider {

    override fun getAdvertisingId(): String? {
        return try {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
            info.id
        } catch (e: Exception) {
            null
        }
    }
}

// --- Device info ---

interface DeviceInfoProvider {
    fun getDeviceId(): String
    fun getUserAgent(): String?
    fun getCarrier(): String?
    fun getNetworkOperator(): String?
}

class DeviceInfoProviderImpl(
    private val context: Context
) : DeviceInfoProvider {

    override fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    override fun getUserAgent(): String? {
        return WebSettings.getDefaultUserAgent(context)
    }

    override fun getCarrier(): String? {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.networkOperatorName
    }

    override fun getNetworkOperator(): String? {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        // MCC+MNC string
        return tm.networkOperator
    }
}

// --- Screen info ---

interface ScreenMeasurementsProvider {
    var device: Measurements
    var mediaSelectorContainer: Measurements
    fun getDensityScaleFactor(): Float
}

class ScreenMeasurementsProviderImpl(
    private val context: Context
) : ScreenMeasurementsProvider {

    override var device: Measurements = Measurements(0, 0)
    override var mediaSelectorContainer: Measurements = Measurements(0, 0)

    override fun getDensityScaleFactor(): Float {
        val metrics = context.resources.displayMetrics
        // 160 dpi is baseline
        return metrics.densityDpi / 160f
    }
}

data class Measurements(
    val width: Int,
    val height: Int
)

// --- Annotation ---

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AdsQueryParameters

// --- Interceptor adding ad / device query params when @AdsQueryParameters is present ---

class AdsQueryParametersInterceptor(
    private val deviceInfoProvider: DeviceInfoProvider,
    private val screenMeasurementsProvider: ScreenMeasurementsProvider,
    private val advertisingInfoProvider: AdvertisingInfoProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val invocation = originalRequest.tag(Invocation::class.java)

        val hasAnnotation =
            invocation?.method()?.getAnnotation(AdsQueryParameters::class.java) != null

        if (!hasAnnotation) {
            return chain.proceed(originalRequest)
        }

        val originalUrl = originalRequest.url
        val builder = originalUrl.newBuilder()
            // TODO: This may need to change?
            // Unique user id in the app; demo uses device ID.
            .addQueryParameter(CUSTOMER_ID, deviceInfoProvider.getDeviceId())
            .addQueryParameter(LOCALE, Locale.getDefault().language)
            .addQueryParameter(AD_MIN_WIDTH, "50")
            .addQueryParameter(AD_MAX_WIDTH, screenMeasurementsProvider.mediaSelectorContainer.width.toString())
            .addQueryParameter(AD_MIN_HEIGHT, "50")
            .addQueryParameter(AD_MAX_HEIGHT, "200")
            .apply {
                advertisingInfoProvider.getAdvertisingId()?.let { ifa ->
                    addQueryParameter(IFA, ifa)
                }
            }
            .addQueryParameter(APP_VERSION, "1.0")
            .addQueryParameter(OS, "Android")
            .addQueryParameter(OS_VERSION, Build.VERSION.RELEASE)
            .addQueryParameter(MANUFACTURER, Build.MANUFACTURER)
            .addQueryParameter(MODEL, Build.MODEL)
            .addQueryParameter(
                AD_DEVICE_WIDTH,
                screenMeasurementsProvider.device.width.toString()
            )
            .addQueryParameter(
                AD_DEVICE_HEIGHT,
                screenMeasurementsProvider.device.height.toString()
            )
            .addQueryParameter(
                AD_PXRATIO,
                screenMeasurementsProvider.getDensityScaleFactor().toString()
            )
            .addQueryParameter(AD_LANGUAGE, Locale.getDefault().language)
            .apply {
                deviceInfoProvider.getCarrier()?.let { carrier ->
                    addQueryParameter(AD_CARRIER, carrier)
                }
            }
            .apply {
                deviceInfoProvider.getNetworkOperator()?.let { op ->
                    addQueryParameter(AD_MCCMNC, op)
                }
            }
            // Demo values; if you have real user profile data, you can adapt these.
            .addQueryParameter(AD_YOB, "1980")
            .addQueryParameter(AD_GENDER, "M")

        val newUrl = builder.build()

        val newRequestBuilder = originalRequest.newBuilder()
            .url(newUrl)

        deviceInfoProvider.getUserAgent()?.let { ua ->
            newRequestBuilder.header(USER_AGENT, ua)
        }

        return chain.proceed(newRequestBuilder.build())
    }

    private companion object {
        const val CUSTOMER_ID = "customer_id"
        const val AD_MIN_WIDTH = "ad-min-width"
        const val AD_MAX_WIDTH = "ad-max-width"
        const val AD_MIN_HEIGHT = "ad-min-height"
        const val AD_MAX_HEIGHT = "ad-max-height"
        const val USER_AGENT = "User-Agent"
        const val APP_VERSION = "ad-app-version"
        const val OS = "ad-os"
        const val OS_VERSION = "ad-osv"
        const val MANUFACTURER = "ad-make"
        const val MODEL = "ad-model"
        const val IFA = "ad-ifa"
        const val LOCALE = "locale"
        const val AD_DEVICE_WIDTH = "ad-device-w"
        const val AD_DEVICE_HEIGHT = "ad-device-h"
        const val AD_YOB = "ad-yob"
        const val AD_GENDER = "ad-gender"
        const val AD_PXRATIO = "ad-pxratio"
        const val AD_CARRIER = "ad-carrier"
        const val AD_MCCMNC = "ad-mccmnc"
        const val AD_LANGUAGE = "ad-language"
    }
}