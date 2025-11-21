package com.klipy.conversationdemo.features.mediaitempreview

import com.cortlandwalker.ghettoxide.Reducer

class MediaItemPreviewReducer : Reducer<MediaItemPreviewState, MediaItemPreviewAction, MediaItemPreviewEffect>() {
    override suspend fun process(action: MediaItemPreviewAction) {
        when (action) {
            MediaItemPreviewAction.DismissClicked -> {
                emit(MediaItemPreviewEffect.Close)
            }
            MediaItemPreviewAction.ShareClicked -> {

            }
        }
    }

    override fun onLoadAction(): MediaItemPreviewAction? {
        return null
    }

}