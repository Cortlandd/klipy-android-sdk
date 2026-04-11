package com.klipy.sdk.model

/**
 * Documented content safety levels supported by Klipy search endpoints.
 */
enum class ContentFilter {
    OFF,
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Media format filters supported by Klipy's GIF and sticker endpoints.
 */
enum class MediaFormat(internal val apiValue: String) {
    GIF("gif"),
    WEBP("webp"),
    JPG("jpg"),
    MP4("mp4"),
    WEBM("webm")
}

/**
 * Optional request customizations for Klipy content APIs.
 *
 * When [customerId] is omitted, the SDK falls back to the device identifier it already uses
 * for recents and ad personalization. When [locale] is omitted for ad-enabled feeds, the
 * interceptor continues to provide the current device locale.
 */
data class MediaRequestOptions(
    val customerId: String? = null,
    val locale: String? = null,
    val contentFilter: ContentFilter? = null,
    val formatFilter: Set<MediaFormat> = emptySet()
)

/**
 * Optional analytics fields for Klipy share events.
 */
data class ShareTriggerOptions(
    val customerId: String? = null,
    val searchQuery: String? = null
)

internal fun MediaRequestOptions.contentFilterValue(): String? = contentFilter?.name?.lowercase()

internal fun MediaRequestOptions.formatFilterValue(): String? =
    formatFilter.takeIf { it.isNotEmpty() }?.joinToString(",") { it.apiValue }
