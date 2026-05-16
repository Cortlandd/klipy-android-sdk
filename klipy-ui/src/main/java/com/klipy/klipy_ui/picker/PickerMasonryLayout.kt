package com.klipy.klipy_ui.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.klipy.sdk.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun PickerMasonryLayout(
    modifier: Modifier,
    items: List<MediaItem>,
    isLoading: Boolean,
    gap: Dp,
    onLoadMore: () -> Unit,
    onMediaItemClicked: (MediaItem) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val widthDp = with(density) { constraints.maxWidth.toDp() }
        val rows = remember { mutableStateOf<List<PickerMediaItemRow>>(emptyList()) }
        val listState = rememberLazyListState()
        val reachedBottom by remember {
            derivedStateOf {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem?.index != 0 &&
                    lastVisibleItem?.index == listState.layoutInfo.totalItemsCount - 2
            }
        }

        LaunchedEffect(items, widthDp) {
            val newRows = withContext(Dispatchers.Default) {
                PickerMasonryMeasurementsCalculator.createRows(
                    items = items,
                    containerWidth = widthDp.value.toInt(),
                    gap = gap.value.toInt()
                )
            }
            rows.value = newRows
        }

        LaunchedEffect(reachedBottom, isLoading) {
            if (!isLoading && reachedBottom) {
                onLoadMore()
            }
        }

        if (widthDp != 0.dp) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                items(
                    items = rows.value,
                    key = { row -> row.firstOrNull()?.mediaItem?.id ?: "" }
                ) { row ->
                    PickerMediaContent(
                        data = row,
                        gap = gap,
                        onMediaItemClicked = onMediaItemClicked
                    )
                }
            }
        }
    }
}
