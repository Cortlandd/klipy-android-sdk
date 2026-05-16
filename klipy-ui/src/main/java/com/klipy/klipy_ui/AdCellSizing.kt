package com.klipy.klipy_ui

internal fun calculateAdCellHeightPx(
    containerWidthPx: Int,
    metaWidthPx: Int,
    metaHeightPx: Int,
    density: Float,
    minHeightDp: Int = 72,
    maxHeightDp: Int = 180
): Int {
    val safeContainerWidth = containerWidthPx.coerceAtLeast(1)
    val minHeightPx = dpToPx(minHeightDp, density)
    val maxHeightPx = dpToPx(maxHeightDp, density)
    if (metaWidthPx <= 0 || metaHeightPx <= 0) return minHeightPx

    val safeMetaWidth = metaWidthPx.coerceAtLeast(1)
    val safeMetaHeight = metaHeightPx.coerceAtLeast(1)
    val ratioHeight = (safeContainerWidth.toFloat() * safeMetaHeight / safeMetaWidth).toInt()
    return ratioHeight.coerceIn(minHeightPx, maxHeightPx)
}

private fun dpToPx(dp: Int, density: Float): Int = (dp * density).toInt()
