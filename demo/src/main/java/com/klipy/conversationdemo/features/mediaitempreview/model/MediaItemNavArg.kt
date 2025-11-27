package com.klipy.conversationdemo.features.mediaitempreview.model

import android.os.Parcelable
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.MetaData
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItemNavArg(
    val id: String,
    val title: String?,
    val mediaType: MediaType,
    val lowUrl: String?,
    val lowWidth: Int,
    val lowHeight: Int,
    val highUrl: String?,
    val highWidth: Int,
    val highHeight: Int
) : Parcelable {

    fun toMediaItem(): MediaItem {
        val low = lowUrl?.let { url ->
            MetaData(
                url = url,
                width = lowWidth,
                height = lowHeight
            )
        }

        val high = highUrl?.let { url ->
            MetaData(
                url = url,
                width = highWidth,
                height = highHeight
            )
        }

        return MediaItem(
            id = id,
            title = title,
            placeHolder = null,
            lowQualityMetaData = low,
            highQualityMetaData = high,
            mediaType = mediaType
        )
    }

    companion object {
        fun from(item: MediaItem): MediaItemNavArg {
            val low = item.lowQualityMetaData
            val high = item.highQualityMetaData

            return MediaItemNavArg(
                id = item.id,
                title = item.title,
                mediaType = item.mediaType,
                lowUrl = low?.url,
                lowWidth = low?.width ?: 0,
                lowHeight = low?.height ?: 0,
                highUrl = high?.url,
                highWidth = high?.width ?: 0,
                highHeight = high?.height ?: 0
            )
        }
    }
}