@file:OptIn(ExperimentalMaterial3Api::class)

package com.klipy.conversationdemo.features.adsdemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.klipy.conversationdemo.features.conversation.ui.MasonryLayout
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType

@Composable
fun AdsDemoScreen(
    state: AdsDemoState,
    reducer: AdsDemoReducer
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text("Ads Demo")
                        Text(
                            text = "Live Klipy masonry feed with ads mixed into content",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { reducer.postAction(AdsDemoAction.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AdsDemoControls(
                state = state,
                reducer = reducer
            )

            when {
                state.isLoading && state.mediaItems.isEmpty() -> {
                    LoadingState()
                }

                state.errorMessage != null && state.mediaItems.isEmpty() -> {
                    ErrorState(
                        message = state.errorMessage,
                        onRetryClicked = { reducer.postAction(AdsDemoAction.RetryClicked) }
                    )
                }

                state.mediaItems.isEmpty() -> {
                    EmptyState(
                        selectedFeed = state.selectedFeed,
                        onRetryClicked = { reducer.postAction(AdsDemoAction.RetryClicked) }
                    )
                }

                else -> {
                    MasonryLayout(
                        modifier = Modifier.fillMaxSize(),
                        items = state.mediaItems,
                        isLoading = state.isLoadingMore,
                        gap = 8.dp,
                        loadMore = { reducer.postAction(AdsDemoAction.LoadMore) },
                        onMediaItemClicked = { reducer.postAction(AdsDemoAction.MediaItemClicked(it)) },
                        onMediaItemLongClicked = { reducer.postAction(AdsDemoAction.MediaItemLongClicked(it)) },
                        onAdLoadFailed = {
                            reducer.postAction(AdsDemoAction.AdFailed(it.id))
                        }
                    )
                }
            }
        }
    }

    state.actionSheetMediaItem?.let { mediaItem ->
        MediaActionDialog(
            mediaItem = mediaItem,
            showHideFromRecent = state.selectedFeed == AdsDemoFeed.RECENT,
            onDismiss = { reducer.postAction(AdsDemoAction.DismissActionSheet) },
            onReportClicked = { reducer.postAction(AdsDemoAction.ReportClicked) },
            onHideFromRecentClicked = { reducer.postAction(AdsDemoAction.HideFromRecentClicked) }
        )
    }

    state.reportReasonMediaItem?.let {
        ReportReasonDialog(
            onDismiss = { reducer.postAction(AdsDemoAction.DismissReportDialog) },
            onReasonSelected = {
                reducer.postAction(AdsDemoAction.ReportReasonSelected(it))
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdsDemoControls(
    state: AdsDemoState,
    reducer: AdsDemoReducer
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.availableMediaTypes.forEach { mediaType ->
                FilterChip(
                    selected = mediaType == state.selectedMediaType,
                    onClick = {
                        reducer.postAction(AdsDemoAction.MediaTypeSelected(mediaType))
                    },
                    label = { Text(mediaType.title) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdsDemoFeed.entries.forEach { feed ->
                AssistChip(
                    onClick = { reducer.postAction(AdsDemoAction.FeedSelected(feed)) },
                    label = { Text(feed.label) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (feed == state.selectedFeed) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.searchDraft,
            onValueChange = {
                reducer.postAction(AdsDemoAction.SearchDraftChanged(it))
            },
            label = { Text("Search Klipy ads feed") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    reducer.postAction(AdsDemoAction.SearchSubmitted(state.searchDraft))
                }
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        reducer.postAction(AdsDemoAction.SearchSubmitted(state.searchDraft))
                    }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        Text(
            text = "Tap media to preview it. Long press for report and recents actions. The default GIF feed includes a seeded sample ad so you can benchmark inline rendering, and ad click-throughs break out instead of taking over the feed slot.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetryClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Couldn’t load the ads demo feed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onRetryClicked) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    selectedFeed: AdsDemoFeed,
    onRetryClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = when (selectedFeed) {
                        AdsDemoFeed.SEARCH -> "Search to load media and ad results"
                        AdsDemoFeed.RECENT -> "No recent items yet"
                        AdsDemoFeed.TRENDING -> "No trending items returned"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Button(onClick = onRetryClicked) {
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
private fun MediaActionDialog(
    mediaItem: MediaItem,
    showHideFromRecent: Boolean,
    onDismiss: () -> Unit,
    onReportClicked: () -> Unit,
    onHideFromRecentClicked: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(mediaItem.title ?: "Media actions")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose how to handle this Klipy item.")
                if (showHideFromRecent) {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onHideFromRecentClicked
                    ) {
                        Text("Hide from recents")
                    }
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onReportClicked
                ) {
                    Text("Report")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ReportReasonDialog(
    onDismiss: () -> Unit,
    onReasonSelected: (String) -> Unit
) {
    val reasons = listOf(
        "Inappropriate content",
        "Spam or misleading",
        "Irrelevant placement"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report this item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reasons.forEach { reason ->
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onReasonSelected(reason) }
                    ) {
                        Text(reason)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
