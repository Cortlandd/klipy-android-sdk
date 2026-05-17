package com.klipy.klipy_demo.features.home

import android.graphics.Color
import com.klipy.klipy_ui.picker.KlipyPickerColors
import com.klipy.klipy_ui.picker.KlipyPickerConfig
import com.klipy.klipy_ui.picker.KlipyPickerThemeMode
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType

private val DemoMediaTypes = listOf(
    MediaType.GIF,
    MediaType.STICKER,
    MediaType.CLIP,
    MediaType.MEME
)

enum class DemoPickerDefaultFeed {
    TRENDING,
    RECENTS,
    EMPTY
}

data class PickerDemoSettings(
    val columns: Int = 3,
    val themeMode: KlipyPickerThemeMode = KlipyPickerThemeMode.AUTOMATIC,
    val defaultFeed: DemoPickerDefaultFeed = DemoPickerDefaultFeed.TRENDING,
    val useCustomColors: Boolean = false,
    val showSearch: Boolean = true,
    val showConfirmationScreen: Boolean = false,
    val itemSpacingDp: Int = 1,
    val mediaTypes: List<MediaType> = DemoMediaTypes
) {
    fun withToggledMediaType(type: MediaType): PickerDemoSettings {
        val nextSelection = if (mediaTypes.contains(type)) {
            mediaTypes - type
        } else {
            DemoMediaTypes.filter { it == type || mediaTypes.contains(it) }
        }

        return copy(mediaTypes = nextSelection.ifEmpty { mediaTypes })
    }

    fun toPickerConfig(isDarkMode: Boolean): KlipyPickerConfig {
        val resolvedThemeMode = when (themeMode) {
            KlipyPickerThemeMode.AUTOMATIC -> {
                if (isDarkMode) KlipyPickerThemeMode.DARK else KlipyPickerThemeMode.LIGHT
            }
            else -> themeMode
        }

        return KlipyPickerConfig(
            mediaTypes = mediaTypes.ifEmpty { DemoMediaTypes },
            columns = columns,
            showTrending = defaultFeed == DemoPickerDefaultFeed.TRENDING,
            showRecents = defaultFeed == DemoPickerDefaultFeed.RECENTS,
            showSearch = showSearch,
            showConfirmationScreen = showConfirmationScreen,
            itemSpacingDp = itemSpacingDp,
            initialMediaType = mediaTypes.firstOrNull() ?: MediaType.GIF,
            themeMode = themeMode,
            colors = if (useCustomColors) resolvedThemeMode.demoPickerColors() else null
        )
    }
}

private fun KlipyPickerThemeMode.demoPickerColors(): KlipyPickerColors {
    return when (this) {
        KlipyPickerThemeMode.DARK -> KlipyPickerColors(
            backgroundColor = Color.parseColor("#FF15110A"),
            surfaceColor = Color.parseColor("#FF1E160A"),
            primaryColor = Color.parseColor("#FFF7C948"),
            onSurfaceColor = Color.parseColor("#FFF9F6EF"),
            secondaryTextColor = Color.parseColor("#FFD1D5DB"),
            outlineColor = Color.parseColor("#FF4B3A17"),
            searchFieldColor = Color.parseColor("#FF221A0E"),
            buttonColor = Color.parseColor("#FFF7C948"),
            onButtonColor = Color.parseColor("#FF111827")
        )
        else -> KlipyPickerColors(
            backgroundColor = Color.parseColor("#FFFFFBEB"),
            surfaceColor = Color.parseColor("#FFFFFFFF"),
            primaryColor = Color.parseColor("#FFD97706"),
            onSurfaceColor = Color.parseColor("#FF1F2937"),
            secondaryTextColor = Color.parseColor("#FF6B7280"),
            outlineColor = Color.parseColor("#FFF3D28A"),
            searchFieldColor = Color.parseColor("#FFFFF4CC"),
            buttonColor = Color.parseColor("#FF111827"),
            onButtonColor = Color.parseColor("#FFFFFFFF")
        )
    }
}


data class HomeState(
    val isOpeningPicker: Boolean = false,
    val lastSelected: MediaItem? = null,
    val lastSearchTerm: String? = null,
    val showSettings: Boolean = false,
    val pickerSettings: PickerDemoSettings = PickerDemoSettings()
)

sealed interface HomeAction {
    data object OpenPickerClicked : HomeAction
    data object OpenSettingsClicked : HomeAction
    data object SettingsDismissed : HomeAction
    data class MediaSelected(val media: MediaItem) : HomeAction
    data object PickerDismissed : HomeAction
    data class SearchTermUpdated(val term: String) : HomeAction
    data class ThemeModeChanged(val mode: KlipyPickerThemeMode) : HomeAction
    data class ColumnsChanged(val columns: Int) : HomeAction
    data class DefaultFeedChanged(val feed: DemoPickerDefaultFeed) : HomeAction
    data class CustomColorsChanged(val enabled: Boolean) : HomeAction
    data class SearchVisibilityChanged(val enabled: Boolean) : HomeAction
    data class ConfirmationScreenChanged(val enabled: Boolean) : HomeAction
    data class ItemSpacingChanged(val spacingDp: Int) : HomeAction
    data class MediaTypeToggled(val type: MediaType) : HomeAction
}

sealed interface HomeEffect {
    data object OpenPicker : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
}
