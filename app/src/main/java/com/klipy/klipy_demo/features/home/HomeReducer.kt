package com.klipy.klipy_demo.features.home

import com.cortlandwalker.ghettoxide.Reducer
import com.klipy.sdk.model.singularName

class HomeReducer : Reducer<HomeState, HomeAction, HomeEffect>() {
    override suspend fun process(action: HomeAction) {
        when(action) {
            is HomeAction.MediaSelected -> {
                state { it.copy(lastSelected = action.media, isOpeningPicker = false) }
                val label = action.media.title ?: action.media.mediaType.singularName()
                emit(HomeEffect.ShowMessage("Selected: $label"))
            }
            HomeAction.OpenPickerClicked -> {
                state { it.copy(isOpeningPicker = true) }
                emit(HomeEffect.OpenPicker)
            }
            HomeAction.PickerDismissed -> {
                state { it.copy(isOpeningPicker = false) }
            }
            is HomeAction.SearchTermUpdated -> {
                state { it.copy(lastSearchTerm = action.term) }
            }
        }
    }

    override fun onLoadAction(): HomeAction? {
        return null
    }

}