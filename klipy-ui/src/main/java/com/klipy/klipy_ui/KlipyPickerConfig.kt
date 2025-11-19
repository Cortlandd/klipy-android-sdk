package com.klipy.klipy_ui

import com.klipy.sdk.model.MediaType

data class KlipyPickerConfig(
    val mediaTypes: List<MediaType> = listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP),
    val columns: Int = 3,
    val showRecents: Boolean = true,
    val showTrending: Boolean = true,
    val initialMediaType: MediaType = MediaType.GIF,
)