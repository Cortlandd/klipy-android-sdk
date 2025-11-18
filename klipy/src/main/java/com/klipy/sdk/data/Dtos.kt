package com.klipy.sdk.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

// DTOs that match Klipy’s HTTP API.

data class CategoriesResponseDto(
    @SerializedName("result")
    val result: Boolean? = null,
    @SerializedName("data")
    val data: List<String>? = null
)

data class MediaItemResponseDto(
    @SerializedName("result")
    val result: Boolean? = null,
    @SerializedName("data")
    val data: DataDto? = null
)

data class DataDto(
    @SerializedName("data")
    val data: List<MediaItemDto>? = null,
    @SerializedName("has_next")
    val hasNext: Boolean? = null,
    @SerializedName("meta")
    val meta: MetaDto? = null
)

data class MetaDto(
    @SerializedName("item_min_width")
    val itemMinWidth: Int? = null,
    @SerializedName("ad_max_resize_percent")
    val adMaxResizePercentage: Int? = null
)

// --- Media Item DTOs ---

data class FileMetaDataDto(
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("width")
    val width: Int? = null,
    @SerializedName("height")
    val height: Int? = null,
    @SerializedName("size")
    val size: Long? = null
)

data class FileTypesDto(
    @SerializedName("gif")
    val gif: FileMetaDataDto? = null,
    @SerializedName("webp")
    val webp: FileMetaDataDto? = null,
    @SerializedName("mp4")
    val mp4: FileMetaDataDto? = null
)

data class DimensionsDto(
    @SerializedName("hd")
    val hd: FileTypesDto? = null,
    @SerializedName("md")
    val md: FileTypesDto? = null,
    @SerializedName("sm")
    val sm: FileTypesDto? = null,
    @SerializedName("xs")
    val xs: FileTypesDto? = null
)

data class ClipFileDto(
    @SerializedName("gif")
    val gif: String? = null,
    @SerializedName("mp4")
    val mp4: String? = null,
    @SerializedName("webp")
    val webp: String? = null
)

/**
 * Polymorphic media item for parsing JSON.
 */
sealed interface MediaItemDto {
    data class GeneralMediaItemDto(
        @SerializedName("slug")
        val slug: String? = null,
        @SerializedName("title")
        val title: String? = null,
        @SerializedName("blur_preview")
        val placeHolder: String? = null,
        @SerializedName("file")
        val file: DimensionsDto? = null,
        @SerializedName("type")
        val type: String? = null
    ) : MediaItemDto

    data class ClipMediaItemDto(
        @SerializedName("slug")
        val slug: String? = null,
        @SerializedName("title")
        val title: String? = null,
        @SerializedName("blur_preview")
        val placeHolder: String? = null,
        @SerializedName("file_meta")
        val fileMeta: FileTypesDto? = null,
        @SerializedName("file")
        val file: ClipFileDto? = null,
        @SerializedName("type")
        val type: String? = null
    ) : MediaItemDto

    data class AdMediaItemDto(
        @SerializedName("width")
        val width: Int? = null,
        @SerializedName("height")
        val height: Int? = null,
        @SerializedName("content")
        val content: String? = null,
        @SerializedName("type")
        val type: String? = null
    ) : MediaItemDto
}

// --- Requests ---

data class TriggerViewRequestDto(
    @SerializedName("customer_id")
    val customerId: String
)

data class ReportRequestDto(
    @SerializedName("customer_id")
    val customerId: String,
    @SerializedName("reason")
    val reason: String
)

// --- Polymorphic deserializer ---

class MediaItemDtoDeserializer : JsonDeserializer<MediaItemDto> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): MediaItemDto {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type")?.asString

        return when (type) {
            "clip" -> context.deserialize(jsonObject, MediaItemDto.ClipMediaItemDto::class.java)
            "ad" -> context.deserialize(jsonObject, MediaItemDto.AdMediaItemDto::class.java)
            else -> context.deserialize(jsonObject, MediaItemDto.GeneralMediaItemDto::class.java)
        }
    }
}

/** Thrown when Retrofit returns a successful response but the body is null. */
class EmptyResponseBodyException : RuntimeException("Response body was null")