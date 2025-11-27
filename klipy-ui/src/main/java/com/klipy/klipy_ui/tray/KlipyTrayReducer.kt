package com.klipy.klipy_ui.tray

import com.cortlandwalker.ghettoxide.Reducer
import com.klipy.klipy_ui.KlipyUi
import com.klipy.sdk.KlipyRepository
import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Core tray reducer shared by XML and Compose.
 *
 * - Trending first (if enabled), otherwise Recents.
 * - Categories are optional/enhancement, not a gate.
 * - Paging is based on "empty page => no more".
 */
class KlipyTrayReducer(
    private val config: KlipyTrayConfig
) : Reducer<KlipyTrayState, KlipyTrayAction, KlipyTrayEffect>() {
    private val repo: KlipyRepository = KlipyUi.requireRepository()
    private var didStart = false
    private var selectedMediaType: MediaType? = null
    private var currentFilter: String = ""
    private var canLoadMore: Boolean = true
    private var isFetching: Boolean = false
    private var searchJob: Job? = null

    override fun onLoadAction(): KlipyTrayAction {
        return KlipyTrayAction.ScreenStarted
    }

    override suspend fun process(action: KlipyTrayAction) {
        when (action) {
            KlipyTrayAction.ScreenStarted -> {
                if (!didStart) {
                    didStart = true
                    loadInitial()
                }
            }

            KlipyTrayAction.RetryInitialLoad -> {
                didStart = false
                loadInitial()
            }

            is KlipyTrayAction.MediaTypeSelected -> {
                onMediaTypeSelected(action.type)
            }

            is KlipyTrayAction.CategorySelected -> {
                onCategorySelected(action.category)
            }

            is KlipyTrayAction.SearchInputChanged -> {
                onSearchInputChanged(action.query)
            }

            is KlipyTrayAction.MediaItemClicked -> {
                emit(KlipyTrayEffect.MediaChosen(action.item))
            }

            is KlipyTrayAction.LoadNextPage -> {
                if (!isFetching && canLoadMore) {
                    fetchMediaPage(reset = false)
                }
            }
        }
    }

    private fun initialFilter(): String = when {
        config.showTrending -> "trending"
        config.showRecents  -> "recent"
        else                -> ""
    }

    private fun loadInitial() {
        val mediaTypes = config.mediaTypes.ifEmpty {
            listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP, MediaType.MEME)
        }

        // Initial UI state: show tabs, nothing selected yet
        state {
            it.copy(
                isLoading = true,
                mediaTypes = mediaTypes,
                chosenMediaType = null,
                categories = emptyList(),
                chosenCategory = null,
                mediaItems = emptyList(),
                searchInput = "",
                lastSearchedInput = null
            )
        }

        scope.launch {
            try {
                val initialType = config.initialMediaType
                    .takeIf { mediaTypes.contains(it) }
                    ?: mediaTypes.firstOrNull()

                if (initialType == null) {
                    state { it.copy(isLoading = false) }
                    return@launch
                }

                // Make sure onMediaTypeSelected is not short-circuited
                selectedMediaType = null

                // Reflect the selected tab immediately in UI
                state { s ->
                    s.copy(
                        isLoading = true,
                        chosenMediaType = initialType,
                        categories = emptyList(),
                        chosenCategory = null,
                        mediaItems = emptyList(),
                        searchInput = "",
                        lastSearchedInput = null
                    )
                }

                // 🔑 Reuse the same logic that works when tapping GIF
                onMediaTypeSelected(initialType)
            } catch (t: Throwable) {
                state { it.copy(isLoading = false) }
                emit(
                    KlipyTrayEffect.ShowError(
                        t.message ?: "Failed to load Klipy content."
                    )
                )
            }
        }
    }

    private fun onMediaTypeSelected(type: MediaType) {
        if (type == selectedMediaType) return

        selectedMediaType = type
        currentFilter = initialFilter()
        canLoadMore = true
        searchJob?.cancel()

        state { s ->
            s.copy(
                isLoading = true,
                chosenMediaType = type,
                categories = emptyList(),
                chosenCategory = null,
                mediaItems = emptyList(),
                searchInput = "",
                lastSearchedInput = null
            )
        }

        scope.launch {
            val mediaResult = repo.getMedia(type, currentFilter)
            mediaResult
                .onSuccess { data ->
                    val items = data.mediaItems
                    state { s ->
                        s.copy(
                            isLoading = false,
                            mediaItems = items
                        )
                    }
                    canLoadMore = items.isNotEmpty()
                }
                .onFailure { error ->
                    state { it.copy(isLoading = false) }
                    emit(
                        KlipyTrayEffect.ShowError(
                            error.message ?: "Failed to load media."
                        )
                    )
                }

            if (config.showCategories) {
                val categoriesResult = repo.getCategories(type)
                categoriesResult
                    .onSuccess { categories ->
                        val trending = categories.firstOrNull {
                            it.title.equals("trending", ignoreCase = true)
                        }
                        state { s ->
                            s.copy(
                                categories = categories,
                                chosenCategory = trending
                            )
                        }
                    }
                    .onFailure { error ->
                        emit(
                            KlipyTrayEffect.ShowError(
                                error.message ?: "Failed to load categories."
                            )
                        )
                    }
            }
        }
    }

    private fun onCategorySelected(category: Category?) {
        searchJob?.cancel()
        currentFilter = category?.title ?: initialFilter()
        canLoadMore = true

        state { s ->
            s.copy(
                chosenCategory = category,
                searchInput = "",
                lastSearchedInput = category?.title,
                mediaItems = emptyList(),
                isLoading = true
            )
        }

        fetchMediaPage(reset = true)
    }

    private fun onSearchInputChanged(queryRaw: String) {
        if (!config.showSearch) return

        val query = queryRaw.trim()
        val mediaType = currentState.chosenMediaType
            ?: currentState.mediaTypes.firstOrNull()
            ?: MediaType.GIF

        // If blank => go back to trending/recent
        if (query.isEmpty()) {
            currentFilter = initialFilter()
            canLoadMore = true
            searchJob?.cancel()

            state {
                it.copy(
                    searchInput = "",
                    lastSearchedInput = "",
                    isLoading = true,
                    mediaItems = emptyList()
                )
            }

            fetchMediaPage(reset = true)
            return
        }

        currentFilter = query
        canLoadMore = true
        searchJob?.cancel()

        state {
            it.copy(
                searchInput = query,
                lastSearchedInput = query,
                isLoading = true,
                mediaItems = emptyList()
            )
        }

        // simple non-debounced search; host can add debounce externally if needed
        scope.launch {
            val result = repo.getMedia(mediaType, query)
            result
                .onSuccess { data ->
                    state {
                        it.copy(
                            isLoading = false,
                            mediaItems = data.mediaItems
                        )
                    }
                }
                .onFailure { throwable ->
                    state { it.copy(isLoading = false) }
                    emit(
                        KlipyTrayEffect.ShowError(
                            throwable.message ?: "Failed to load Klipy results"
                        )
                    )
                }
        }
    }

    private fun fetchMediaPage(reset: Boolean) {
        val type = selectedMediaType ?: return

        if (!reset && (!canLoadMore || isFetching)) return

        if (reset) {
            canLoadMore = true
            repo.reset(type)
        }

        isFetching = true

        scope.launch {
            try {
                val result = repo.getMedia(type, currentFilter)
                result
                    .onSuccess { data ->
                        val items = data.mediaItems
                        if (items.isEmpty()) {
                            canLoadMore = false
                        }

                        state { s ->
                            val newItems =
                                if (reset) items
                                else s.mediaItems + items

                            s.copy(
                                isLoading = false,
                                mediaItems = newItems
                            )
                        }
                    }
                    .onFailure { error ->
                        state { it.copy(isLoading = false) }
                        emit(
                            KlipyTrayEffect.ShowError(
                                error.message ?: "Failed to load media."
                            )
                        )
                    }
            } finally {
                isFetching = false
            }
        }
    }
}

// Handy if you ever want to debounce externally:
private fun kotlinx.coroutines.CoroutineScope.debounceSearch(
    delayMillis: Long = 400L,
    block: () -> Unit
): Job = launch {
    delay(delayMillis)
    block()
}
