package com.klipy.klipy_ui.picker

import android.os.Parcelable
import com.klipy.sdk.model.MediaType
import kotlinx.parcelize.Parcelize

/**
 * Configuration for [KlipyPickerDialogFragment].
 *
 * @property mediaTypes Media tabs to show (GIF / STICKER / CLIP / MEME).
 * @property columns Number of columns to use in the grid.
 * @property showRecents Whether to include "recent" content as a source.
 * @property showTrending Whether to include "trending" content as a source.
 *                        If both [showRecents] and [showTrending] are true,
 *                        trending is used as the initial feed.
 * @property showSearch Whether to show the search field in the picker shell.
 * @property showConfirmationScreen Whether to confirm non-ad media selection before dismissing.
 * @property itemSpacingDp Spacing in dp between visible media items.
 * @property initialMediaType Which tab is initially selected when the picker opens.
 * @property themeMode Visual theme for the picker sheet.
 * @property colors Optional high-level color overrides layered on top of [themeMode].
 *
 * This class is `Parcelable` so it can be passed in a fragment `Bundle`.
 */
@Parcelize
data class KlipyPickerConfig(
    val mediaTypes: List<MediaType> = listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP, MediaType.MEME),
    val columns: Int = 3,
    val showRecents: Boolean = false,
    val showTrending: Boolean = true,
    val showSearch: Boolean = true,
    val showConfirmationScreen: Boolean = false,
    val itemSpacingDp: Int = 1,
    val initialMediaType: MediaType = MediaType.GIF,
    val themeMode: KlipyPickerThemeMode = KlipyPickerThemeMode.AUTOMATIC,
    val colors: KlipyPickerColors? = null,
) : Parcelable
