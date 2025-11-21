package com.klipy.conversationdemo.features.mediaitempreview

import com.klipy.sdk.model.MediaItem

data class MediaItemPreviewState(
    val selectedMediaItem: MediaItem? = null
)

sealed interface MediaItemPreviewAction {
    data object DismissClicked : MediaItemPreviewAction
    data object ShareClicked : MediaItemPreviewAction
}

sealed interface MediaItemPreviewEffect {
    data object Close : MediaItemPreviewEffect
    data class Share(val url: String) : MediaItemPreviewEffect
}
