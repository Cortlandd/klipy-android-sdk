package com.klipy.conversationdemo.features.adsdemo

import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType

data class AdsDemoState(
    val availableMediaTypes: List<MediaType> = emptyList(),
    val selectedMediaType: MediaType? = null,
    val selectedFeed: AdsDemoFeed = AdsDemoFeed.TRENDING,
    val searchDraft: String = "",
    val activeSearchQuery: String? = null,
    val mediaItems: List<MediaItem> = emptyList(),
    val failedAdIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val actionSheetMediaItem: MediaItem? = null,
    val reportReasonMediaItem: MediaItem? = null
) {
    fun selectedFilterKey(): String? = when (selectedFeed) {
        AdsDemoFeed.TRENDING -> "trending"
        AdsDemoFeed.RECENT -> "recent"
        AdsDemoFeed.SEARCH -> activeSearchQuery?.takeIf { it.isNotBlank() }
    }
}

sealed interface AdsDemoAction {
    data object ScreenStarted : AdsDemoAction
    data object BackClicked : AdsDemoAction
    data class MediaTypeSelected(val mediaType: MediaType) : AdsDemoAction
    data class FeedSelected(val feed: AdsDemoFeed) : AdsDemoAction
    data class SearchDraftChanged(val value: String) : AdsDemoAction
    data class SearchSubmitted(val query: String) : AdsDemoAction
    data object RetryClicked : AdsDemoAction
    data object LoadMore : AdsDemoAction
    data class MediaItemClicked(val item: MediaItem) : AdsDemoAction
    data class MediaItemLongClicked(val item: MediaItem) : AdsDemoAction
    data class AdFailed(val itemId: String) : AdsDemoAction
    data object DismissActionSheet : AdsDemoAction
    data object ReportClicked : AdsDemoAction
    data object DismissReportDialog : AdsDemoAction
    data class ReportReasonSelected(val reason: String) : AdsDemoAction
    data object HideFromRecentClicked : AdsDemoAction
}

enum class AdsDemoFeed(val label: String) {
    TRENDING("Trending"),
    RECENT("Recent"),
    SEARCH("Search")
}

sealed interface AdsDemoEffect {
    data object Back : AdsDemoEffect
    data class OpenMediaPreview(val item: MediaItem) : AdsDemoEffect
    data class ShowMessage(val message: String) : AdsDemoEffect
}
