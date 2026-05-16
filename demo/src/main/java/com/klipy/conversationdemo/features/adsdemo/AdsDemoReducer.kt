package com.klipy.conversationdemo.features.adsdemo

import com.cortlandwalker.ghettoxide.Reducer
import com.klipy.conversationdemo.features.conversation.model.MasonryMeasurementsCalculator
import com.klipy.klipy_ui.KlipyUi
import com.klipy.sdk.KlipyRepository
import com.klipy.sdk.model.MediaData
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.MetaData
import com.klipy.sdk.model.isAD

class AdsDemoReducer(
    private val repository: KlipyRepository = KlipyUi.requireRepository()
) : Reducer<AdsDemoState, AdsDemoAction, AdsDemoEffect>() {

    override fun onLoadAction(): AdsDemoAction = AdsDemoAction.ScreenStarted

    override suspend fun process(action: AdsDemoAction) {
        when (action) {
            AdsDemoAction.ScreenStarted -> loadInitial()
            AdsDemoAction.BackClicked -> emit(AdsDemoEffect.Back)

            is AdsDemoAction.MediaTypeSelected -> onMediaTypeSelected(action.mediaType)
            is AdsDemoAction.FeedSelected -> onFeedSelected(action.feed)
            is AdsDemoAction.SearchDraftChanged -> {
                updateState { it.copy(searchDraft = action.value) }
            }

            is AdsDemoAction.SearchSubmitted -> onSearchSubmitted(action.query)
            AdsDemoAction.RetryClicked -> refreshCurrentFeed()
            AdsDemoAction.LoadMore -> loadMore()

            is AdsDemoAction.MediaItemClicked -> onMediaItemClicked(action.item)
            is AdsDemoAction.MediaItemLongClicked -> onMediaItemLongClicked(action.item)
            is AdsDemoAction.AdFailed -> onAdFailed(action.itemId)
            AdsDemoAction.DismissActionSheet -> {
                updateState { it.copy(actionSheetMediaItem = null) }
            }

            AdsDemoAction.ReportClicked -> onReportClicked()
            AdsDemoAction.DismissReportDialog -> {
                updateState { it.copy(reportReasonMediaItem = null) }
            }

            is AdsDemoAction.ReportReasonSelected -> onReportReasonSelected(action.reason)
            AdsDemoAction.HideFromRecentClicked -> onHideFromRecentClicked()
        }
    }

    private suspend fun loadInitial() {
        if (currentState.availableMediaTypes.isNotEmpty()) return

        updateState { it.copy(isLoading = true, errorMessage = null) }

        val availableMediaTypes = repository.getAvailableMediaTypes()
            .filterNot { it == MediaType.AD }

        val selectedMediaType = availableMediaTypes.firstOrNull { it == MediaType.GIF }
            ?: availableMediaTypes.firstOrNull()
            ?: MediaType.GIF

        updateState {
            it.copy(
                availableMediaTypes = availableMediaTypes,
                selectedMediaType = selectedMediaType,
                selectedFeed = AdsDemoFeed.TRENDING
            )
        }

        refreshCurrentFeed()
    }

    private suspend fun onMediaTypeSelected(mediaType: MediaType) {
        if (mediaType == currentState.selectedMediaType) return

        updateState {
            it.copy(
                selectedMediaType = mediaType,
                mediaItems = emptyList(),
                errorMessage = null
            )
        }
        refreshCurrentFeed()
    }

    private suspend fun onFeedSelected(feed: AdsDemoFeed) {
        if (feed == currentState.selectedFeed) return

        updateState {
            it.copy(
                selectedFeed = feed,
                activeSearchQuery = if (feed == AdsDemoFeed.SEARCH) it.activeSearchQuery else null,
                mediaItems = emptyList(),
                errorMessage = null
            )
        }
        refreshCurrentFeed()
    }

    private suspend fun onSearchSubmitted(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            emit(AdsDemoEffect.ShowMessage("Enter a search term first."))
            return
        }

        updateState {
            it.copy(
                selectedFeed = AdsDemoFeed.SEARCH,
                activeSearchQuery = trimmed,
                mediaItems = emptyList(),
                errorMessage = null
            )
        }
        refreshCurrentFeed()
    }

    private suspend fun loadMore() {
        if (currentState.isLoading || currentState.isLoadingMore || currentState.errorMessage != null) return

        val selectedMediaType = currentState.selectedMediaType ?: return
        val filter = currentState.selectedFilterKey() ?: return

        updateState { it.copy(isLoadingMore = true) }

        repository.getMedia(selectedMediaType, filter)
            .onSuccess { data ->
                applyMediaData(data, append = true)
            }
            .onFailure { error ->
                updateState { it.copy(isLoadingMore = false) }
                emit(
                    AdsDemoEffect.ShowMessage(
                        error.message ?: "Couldn't load more Klipy content."
                    )
                )
            }
    }

    private suspend fun onMediaItemClicked(item: MediaItem) {
        if (item.isAD()) return

        val selectedMediaType = currentState.selectedMediaType ?: item.mediaType
        repository.triggerView(selectedMediaType, item.id)
        emit(AdsDemoEffect.OpenMediaPreview(item))
    }

    private suspend fun onMediaItemLongClicked(item: MediaItem) {
        if (item.isAD()) return
        updateState { it.copy(actionSheetMediaItem = item) }
    }

    private suspend fun onAdFailed(itemId: String) {
        updateState { current ->
            if (itemId in current.failedAdIds) return@updateState current

            current.copy(
                failedAdIds = current.failedAdIds + itemId,
                mediaItems = current.mediaItems
                    .filterNot { it.id == itemId }
                    .let { items ->
                        if (items.all { it.isAD() }) emptyList() else items
                    }
            )
        }
    }

    private suspend fun onReportClicked() {
        val mediaItem = currentState.actionSheetMediaItem ?: return
        updateState {
            it.copy(
                actionSheetMediaItem = null,
                reportReasonMediaItem = mediaItem
            )
        }
    }

    private suspend fun onReportReasonSelected(reason: String) {
        val mediaItem = currentState.reportReasonMediaItem ?: return
        val selectedMediaType = currentState.selectedMediaType ?: mediaItem.mediaType

        updateState { it.copy(reportReasonMediaItem = null) }

        repository.report(selectedMediaType, mediaItem.id, reason)
            .onSuccess {
                emit(AdsDemoEffect.ShowMessage("Reported to Klipy."))
            }
            .onFailure { error ->
                emit(
                    AdsDemoEffect.ShowMessage(
                        error.message ?: "Couldn't report this item."
                    )
                )
            }
    }

    private suspend fun onHideFromRecentClicked() {
        val mediaItem = currentState.actionSheetMediaItem ?: return
        val selectedMediaType = currentState.selectedMediaType ?: mediaItem.mediaType

        updateState { it.copy(actionSheetMediaItem = null) }

        repository.hideFromRecent(selectedMediaType, mediaItem.id)
            .onSuccess {
                updateState { current ->
                    val updatedItems = current.mediaItems
                        .filterNot { it.id == mediaItem.id }
                        .let { items ->
                            if (items.all { it.isAD() }) emptyList() else items
                        }

                    current.copy(mediaItems = updatedItems)
                }
                emit(AdsDemoEffect.ShowMessage("Removed from recents."))
            }
            .onFailure { error ->
                emit(
                    AdsDemoEffect.ShowMessage(
                        error.message ?: "Couldn't hide this item from recents."
                    )
                )
            }
    }

    private suspend fun refreshCurrentFeed() {
        val selectedMediaType = currentState.selectedMediaType ?: return
        val filter = currentState.selectedFilterKey()

        if (filter == null) {
            updateState {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    mediaItems = emptyList(),
                    errorMessage = if (it.selectedFeed == AdsDemoFeed.SEARCH) {
                        "Search for something to load live Klipy content."
                    } else {
                        null
                    }
                )
            }
            return
        }

        repository.reset(selectedMediaType)
        updateState {
            it.copy(
                isLoading = true,
                isLoadingMore = false,
                mediaItems = emptyList(),
                errorMessage = null
            )
        }

        repository.getMedia(selectedMediaType, filter)
            .onSuccess { data ->
                applyMediaData(data, append = false)
            }
            .onFailure { error ->
                updateState {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Couldn't load Klipy content."
                    )
                }
            }
    }

    private suspend fun applyMediaData(
        data: MediaData,
        append: Boolean
    ) {
        MasonryMeasurementsCalculator.itemMinWidth = data.itemMinWidth
        MasonryMeasurementsCalculator.adMaxResizePercentage = data.adMaxResizePercentage

        updateState { current ->
            val merged = if (append) current.mediaItems + data.mediaItems else data.mediaItems
            val normalizedItems = maybeInjectDefaultDemoAd(
                mediaType = current.selectedMediaType,
                items = merged,
                failedAdIds = current.failedAdIds
            )
            current.copy(
                mediaItems = normalizedItems
                    .filterNot { it.id in current.failedAdIds }
                    .distinctBy { it.id to it.mediaType },
                isLoading = false,
                isLoadingMore = false,
                errorMessage = null
            )
        }
    }

    private fun maybeInjectDefaultDemoAd(
        mediaType: MediaType?,
        items: List<MediaItem>,
        failedAdIds: Set<String>
    ): List<MediaItem> {
        if (mediaType != MediaType.GIF) return items
        if (DEFAULT_DEMO_AD_ID in failedAdIds) return items.filterNot { it.id == DEFAULT_DEMO_AD_ID }

        val contentItems = items.filterNot { it.id == DEFAULT_DEMO_AD_ID }
        val insertIndex = minOf(2, contentItems.size)

        return buildList {
            addAll(contentItems.take(insertIndex))
            add(DEFAULT_DEMO_AD_ITEM)
            addAll(contentItems.drop(insertIndex))
        }
    }

    private suspend fun updateState(transform: (AdsDemoState) -> AdsDemoState) {
        state { current -> transform(current) }
    }

    private companion object {
        const val DEFAULT_DEMO_AD_ID = "demo-inline-ad"

        val DEFAULT_DEMO_AD_ITEM = MediaItem(
            id = DEFAULT_DEMO_AD_ID,
            title = "Demo inline ad",
            blurPreview = null,
            lowQualityMetaData = MetaData(
                url = "https://klipy.com/advertisement/api-us-east4/66c29474-5e60-4dd5-a476-7f430f7f7e90",
                width = 320,
                height = 50
            ),
            highQualityMetaData = null,
            mediaType = MediaType.AD
        )
    }
}
