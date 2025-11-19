package com.klipy.klipy_demo.features.home

import com.klipy.klipy_ui.KlipyMediaAdapter
import com.klipy.sdk.model.MediaItem


data class HomeState(
    val isOpeningPicker: Boolean = false,
    val lastSelected: MediaItem? = null,
    val lastSearchTerm: String? = null
)

sealed interface HomeAction {
    data object OpenPickerClicked : HomeAction
    data class MediaSelected(val media: MediaItem) : HomeAction
    data object PickerDismissed : HomeAction
    data class SearchTermUpdated(val term: String) : HomeAction
}

sealed interface HomeEffect {
    data object OpenPicker : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
}