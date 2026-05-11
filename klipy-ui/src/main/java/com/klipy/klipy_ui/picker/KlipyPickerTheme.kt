package com.klipy.klipy_ui.picker

import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize

/**
 * Theme mode for the XML picker.
 */
enum class KlipyPickerThemeMode {
    AUTOMATIC,
    LIGHT,
    DARK
}

/**
 * Optional theme color overrides for [KlipyPickerDialogFragment].
 *
 * These colors are intentionally high-level so host apps can blend the picker
 * into their existing brand without needing to override every single view.
 */
@Parcelize
data class KlipyPickerColors(
    @ColorInt val backgroundColor: Int? = null,
    @ColorInt val surfaceColor: Int? = null,
    @ColorInt val primaryColor: Int? = null,
    @ColorInt val onSurfaceColor: Int? = null,
    @ColorInt val secondaryTextColor: Int? = null,
    @ColorInt val outlineColor: Int? = null,
    @ColorInt val searchFieldColor: Int? = null,
    @ColorInt val buttonColor: Int? = null,
    @ColorInt val onButtonColor: Int? = null
) : Parcelable
