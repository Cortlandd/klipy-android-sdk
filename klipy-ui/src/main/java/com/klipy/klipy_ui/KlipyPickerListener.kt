package com.klipy.klipy_ui

import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType

interface KlipyPickerListener {
    fun onMediaSelected(item: MediaItem, searchTerm: String?)
    fun onDismissed(lastContentType: MediaType?)
    fun didSearchTerm(term: String)
}