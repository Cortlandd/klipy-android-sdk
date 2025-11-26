package com.klipy.klipy_ui

import android.os.Parcelable
import com.klipy.sdk.model.MediaType
import kotlinx.parcelize.Parcelize

@Parcelize
data class KlipyPickerConfig(
    val mediaTypes: List<MediaType> = listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP, MediaType.MEME),
    val columns: Int = 3,
    val showRecents: Boolean = false,
    val showTrending: Boolean = true,
    val initialMediaType: MediaType = MediaType.GIF,
) : Parcelable