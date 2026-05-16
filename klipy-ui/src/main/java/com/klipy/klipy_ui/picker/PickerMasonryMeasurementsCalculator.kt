package com.klipy.klipy_ui.picker

import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.isAD
import kotlin.math.abs
import kotlin.math.min

internal typealias PickerMediaItemRow = List<PickerMediaItemUIModel>

internal data class PickerMediaItemUIModel(
    val mediaItem: MediaItem,
    var measuredWidth: Int,
    var measuredHeight: Int
)

internal object PickerMasonryMeasurementsCalculator {
    private const val ITEM_MIN_HEIGHT = 50
    private const val ITEM_MAX_HEIGHT = 180
    private const val MAX_ITEMS_PER_ROW = 4

    private var calculatedItems = mutableListOf<MediaItem>()
    private var calculatedResults = mutableListOf<PickerMediaItemRow>()
    private var lastContainerWidth = -1
    private var lastGap = -1
    private var lastItemMinWidth = -1
    private var lastAdMaxResizePercentage = Float.NaN

    var itemMinWidth: Int = 0
    var adMaxResizePercentage: Float = 0F

    fun reset() {
        calculatedItems.clear()
        calculatedResults.clear()
        lastContainerWidth = -1
        lastGap = -1
        lastItemMinWidth = -1
        lastAdMaxResizePercentage = Float.NaN
        itemMinWidth = 0
        adMaxResizePercentage = 0F
    }

    fun createRows(
        items: List<MediaItem>,
        containerWidth: Int,
        gap: Int
    ): List<PickerMediaItemRow> {
        val isNewList = isNewList(calculatedItems, items)
        val layoutConfigChanged =
            lastContainerWidth != containerWidth ||
                lastGap != gap ||
                lastItemMinWidth != itemMinWidth ||
                lastAdMaxResizePercentage != adMaxResizePercentage

        if (isNewList || layoutConfigChanged) {
            calculatedResults = calculateRows(items, containerWidth, gap).toMutableList()
        } else if (items.size != calculatedItems.size) {
            val lastCalculatedRow = calculatedResults.lastOrNull()
            if (lastCalculatedRow != null) {
                calculatedResults.remove(lastCalculatedRow)
            }
            val itemsToCalculate = items.subList(
                calculatedResults.flatten().size,
                items.size
            )
            val newRows = calculateRows(itemsToCalculate, containerWidth, gap)
            calculatedResults = (calculatedResults + newRows).toMutableList()
        }

        calculatedItems = items.toMutableList()
        lastContainerWidth = containerWidth
        lastGap = gap
        lastItemMinWidth = itemMinWidth
        lastAdMaxResizePercentage = adMaxResizePercentage
        return calculatedResults
    }

    private fun calculateRows(
        items: List<MediaItem>,
        containerWidth: Int,
        gap: Int
    ): List<PickerMediaItemRow> {
        val rows = mutableListOf<PickerMediaItemRow>()
        var currentIndex = 0

        while (currentIndex < items.size) {
            val possibleItemsInRow = items.subList(
                currentIndex,
                min(currentIndex + MAX_ITEMS_PER_ROW, items.size)
            )

            val adjustedRow = precalculateSingleRow(possibleItemsInRow, containerWidth, gap)
            rows.add(adjustedRow)
            currentIndex += adjustedRow.size
        }

        return rows
    }

    private fun precalculateSingleRow(
        items: List<MediaItem>,
        containerWidth: Int,
        gap: Int
    ): PickerMediaItemRow {
        var possibleItemsInRow = items
        var minimumChange = Int.MAX_VALUE
        var currentRow = mutableListOf<PickerMediaItemUIModel>()
        var itemsHeightInRow = 0

        var currentMinHeight = ITEM_MIN_HEIGHT
        var currentMaxHeight = ITEM_MAX_HEIGHT

        val adIndex = possibleItemsInRow.indexOfFirst { it.isAD() }
        if (adIndex > 1) {
            possibleItemsInRow = possibleItemsInRow.subList(0, 2)
        } else if (adIndex >= 0) {
            val adMeta = possibleItemsInRow[adIndex].lowQualityMetaData
            if (adMeta != null) {
                currentMinHeight = adMeta.height
                currentMaxHeight = adMeta.height
            }
        }

        for (height in currentMinHeight..currentMaxHeight) {
            val itemsInRow = mutableListOf<PickerMediaItemUIModel>()
            for (element in possibleItemsInRow) {
                val item = element.copy()
                val meta = item.lowQualityMetaData ?: continue
                itemsInRow.add(PickerMediaItemUIModel(item, 0, 0))
                val newWidth = if (item.isAD()) {
                    meta.width
                } else {
                    ((meta.width.toFloat() * height) / meta.height).toInt()
                }
                itemsInRow[itemsInRow.lastIndex] =
                    itemsInRow.last().copy(measuredWidth = newWidth)
                val totalWidth =
                    itemsInRow.sumOf { it.measuredWidth } + (itemsInRow.size - 1) * gap
                val change = containerWidth - totalWidth

                if (abs(change) < abs(minimumChange) || (currentRow.size == 1 && itemsInRow.size != 1)) {
                    minimumChange = change
                    currentRow = itemsInRow.toMutableList()
                    itemsHeightInRow = height
                }
            }
        }

        val nonAdItems = currentRow.filter { !it.mediaItem.isAD() }
        currentRow.forEachIndexed { index, item ->
            val addition = if (item.mediaItem.isAD()) 0 else minimumChange / nonAdItems.size.coerceAtLeast(1)
            currentRow[index].apply {
                measuredWidth = item.measuredWidth + addition
                measuredHeight = itemsHeightInRow
            }
        }

        if (adIndex >= 0 && nonAdItems.size != currentRow.size) {
            val itemsBelowMinWidth = nonAdItems.filter { it.measuredWidth < itemMinWidth }
            if (itemsBelowMinWidth.isNotEmpty()) {
                itemsBelowMinWidth.forEach {
                    it.measuredWidth = itemMinWidth
                }
                val newRowWidth = currentRow.sumOf { it.measuredWidth } + (currentRow.size - 1) * gap

                if (newRowWidth > containerWidth) {
                    val adItem = currentRow[adIndex]
                    val minAdWidth = (adItem.measuredWidth * (1F - adMaxResizePercentage)).toInt()
                    var resizedAdWidth = adItem.measuredWidth - (newRowWidth - containerWidth)
                    if (resizedAdWidth < minAdWidth) {
                        val adWidthDifference = minAdWidth - resizedAdWidth
                        itemsBelowMinWidth.forEach {
                            it.measuredWidth -= adWidthDifference / itemsBelowMinWidth.size.coerceAtLeast(1)
                        }
                        resizedAdWidth = minAdWidth
                    }

                    adItem.measuredHeight =
                        (adItem.measuredHeight * (resizedAdWidth / adItem.measuredWidth.toFloat())).toInt()
                    adItem.measuredWidth = resizedAdWidth

                    itemsBelowMinWidth.forEach {
                        it.measuredHeight = adItem.measuredHeight
                    }
                }
            }
        }

        return currentRow
    }

    private fun isNewList(existingItems: List<MediaItem>, newItems: List<MediaItem>): Boolean {
        return existingItems.isEmpty() ||
            newItems.size < existingItems.size ||
            existingItems.map { it.id } != newItems.map { it.id }.take(existingItems.size)
    }
}

internal fun PickerMediaItemRow.hasAd(): Boolean = any { it.mediaItem.isAD() }
