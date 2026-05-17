package com.klipy.klipy_demo.features.home

import com.cortlandwalker.ghettoxide.Reducer
import com.klipy.sdk.model.singularName

class HomeReducer : Reducer<HomeState, HomeAction, HomeEffect>() {
    override suspend fun process(action: HomeAction) {
        when(action) {
            HomeAction.OpenPickerClicked -> {
                emit(HomeEffect.OpenPicker)
            }

            HomeAction.OpenSettingsClicked -> {
                state { it.copy(showSettings = true) }
            }

            HomeAction.SettingsDismissed -> {
                state { it.copy(showSettings = false) }
            }

            is HomeAction.MediaSelected -> {
                state { it.copy(lastSelected = action.media) }
            }

            is HomeAction.SearchTermUpdated -> {
                state { it.copy(lastSearchTerm = action.term) }
            }

            is HomeAction.ThemeModeChanged -> {
                state { it.copy(pickerSettings = it.pickerSettings.copy(themeMode = action.mode)) }
            }

            is HomeAction.ColumnsChanged -> {
                state { it.copy(pickerSettings = it.pickerSettings.copy(columns = action.columns)) }
            }

            is HomeAction.DefaultFeedChanged -> {
                state { it.copy(pickerSettings = it.pickerSettings.copy(defaultFeed = action.feed)) }
            }

            is HomeAction.CustomColorsChanged -> {
                state { it.copy(pickerSettings = it.pickerSettings.copy(useCustomColors = action.enabled)) }
            }

            is HomeAction.SearchVisibilityChanged -> {
                state { it.copy(pickerSettings = it.pickerSettings.copy(showSearch = action.enabled)) }
            }

            is HomeAction.ConfirmationScreenChanged -> {
                state { it.copy(pickerSettings = it.pickerSettings.copy(showConfirmationScreen = action.enabled)) }
            }

            is HomeAction.ItemSpacingChanged -> {
                state { it.copy(pickerSettings = it.pickerSettings.copy(itemSpacingDp = action.spacingDp)) }
            }

            is HomeAction.MediaTypeToggled -> {
                state {
                    it.copy(
                        pickerSettings = it.pickerSettings.withToggledMediaType(action.type)
                    )
                }
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
