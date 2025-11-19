package com.klipy.klipy_demo.features.home

import com.cortlandwalker.ghettoxide.Reducer
import com.klipy.sdk.model.singularName

class HomeReducer : Reducer<HomeState, HomeAction, HomeEffect>() {
    override suspend fun process(action: HomeAction) {
        when(action) {
            HomeAction.OpenPickerClicked -> {
                emit(HomeEffect.OpenPicker)
            }

            is HomeAction.MediaSelected -> {
                state { it.copy(lastSelected = action.media) }
            }

            is HomeAction.SearchTermUpdated -> {
                state { it.copy(lastSearchTerm = action.term) }
            }

            HomeAction.PickerDismissed -> {
                // No-op for now, but you could emit an effect or clear state if you want
            }
        }
    }

    override fun onLoadAction(): HomeAction? {
        return null
    }

}