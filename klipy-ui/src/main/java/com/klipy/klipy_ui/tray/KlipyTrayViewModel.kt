package com.klipy.klipy_ui.tray

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.klipy.klipy_ui.KlipyUi
import com.klipy.sdk.KlipyRepository
import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Native AndroidX implementation of the tray logic (no external architecture dependencies).
 *
 * This is intentionally small and explicit:
 * - State lives in a [StateFlow]
 * - One-off effects are emitted via a [Channel]
 * - UI dispatches [KlipyTrayAction] through [dispatch]
 */
class KlipyTrayViewModel(
    private val config: KlipyTrayConfig,
    private val repoProvider: () -> KlipyRepository = { KlipyUi.requireRepository() }
) : ViewModel() {

    private val repo: KlipyRepository by lazy(repoProvider)

    private val _state = MutableStateFlow(KlipyTrayState())
    val state: StateFlow<KlipyTrayState> = _state.asStateFlow()

    private val _effects = Channel<KlipyTrayEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<KlipyTrayEffect> = _effects.receiveAsFlow()

    private var didStart = false
    private var selectedMediaType: MediaType? = null
    private var currentFilter: String = ""
    private var canLoadMore: Boolean = true
    private var isFetching: Boolean = false

    fun start() {
        dispatch(KlipyTrayAction.ScreenStarted)
    }

    fun dispatch(action: KlipyTrayAction) {
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

            is KlipyTrayAction.MediaTypeSelected -> onMediaTypeSelected(action.type)
            is KlipyTrayAction.CategorySelected -> onCategorySelected(action.category)
            is KlipyTrayAction.SearchInputChanged -> onSearchInputChanged(action.query)
            is KlipyTrayAction.MediaItemClicked -> emit(KlipyTrayEffect.MediaChosen(action.item))
            is KlipyTrayAction.LoadNextPage -> onLoadNextPage(action)
        }
    }

    private fun onLoadNextPage(action: KlipyTrayAction.LoadNextPage) {
        // Basic threshold paging (prevents spamming requests on every scroll pixel)
        val shouldLoad = action.totalItemCount > 0 &&
                (action.firstVisibleItem + action.visibleItemCount) >= (action.totalItemCount - 6)

        if (shouldLoad && !isFetching && canLoadMore) {
            fetchMediaPage(reset = false)
        }
    }

    private fun initialFilter(): String = when {
        config.showTrending -> "trending"
        config.showRecents -> "recent"
        else -> ""
    }

    private fun loadInitial() {
        val mediaTypes = config.mediaTypes.ifEmpty {
            listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP, MediaType.MEME)
        }

        // Initial UI state
        _state.value = _state.value.copy(
            isLoading = true,
            mediaTypes = mediaTypes,
            chosenMediaType = null,
            categories = emptyList(),
            chosenCategory = null,
            mediaItems = emptyList(),
            searchInput = "",
            lastSearchedInput = null
        )

        viewModelScope.launch {
            try {
                val initialType = config.initialMediaType
                    .takeIf { mediaTypes.contains(it) }
                    ?: mediaTypes.firstOrNull()

                if (initialType == null) {
                    _state.value = _state.value.copy(isLoading = false)
                    return@launch
                }

                // Ensure selection logic runs
                selectedMediaType = null

                // Reflect selected tab immediately
                _state.value = _state.value.copy(
                    isLoading = true,
                    chosenMediaType = initialType,
                    categories = emptyList(),
                    chosenCategory = null,
                    mediaItems = emptyList(),
                    searchInput = "",
                    lastSearchedInput = null
                )

                onMediaTypeSelected(initialType)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(isLoading = false)
                emit(KlipyTrayEffect.ShowError(t.message ?: "Failed to load Klipy content."))
            }
        }
    }

    private fun onMediaTypeSelected(type: MediaType) {
        if (type == selectedMediaType) return

        selectedMediaType = type
        currentFilter = initialFilter()
        canLoadMore = true

        _state.value = _state.value.copy(
            isLoading = true,
            chosenMediaType = type,
            categories = emptyList(),
            chosenCategory = null,
            mediaItems = emptyList(),
            searchInput = "",
            lastSearchedInput = null
        )

        viewModelScope.launch {
            val mediaResult = repo.getMedia(type, currentFilter)
            mediaResult
                .onSuccess { data ->
                    val items = data.mediaItems
                    _state.value = _state.value.copy(
                        isLoading = false,
                        mediaItems = items
                    )
                    canLoadMore = items.isNotEmpty()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(isLoading = false)
                    emit(KlipyTrayEffect.ShowError(error.message ?: "Failed to load media."))
                }

            if (config.showCategories) {
                val categoriesResult = repo.getCategories(type)
                categoriesResult
                    .onSuccess { categories ->
                        val trending = categories.firstOrNull {
                            it.title.equals("trending", ignoreCase = true)
                        }
                        _state.value = _state.value.copy(
                            categories = categories,
                            chosenCategory = trending
                        )
                    }
                    .onFailure { error ->
                        emit(KlipyTrayEffect.ShowError(error.message ?: "Failed to load categories."))
                    }
            }
        }
    }

    private fun onCategorySelected(category: Category?) {
        currentFilter = category?.title ?: initialFilter()
        canLoadMore = true

        _state.value = _state.value.copy(
            chosenCategory = category,
            searchInput = "",
            lastSearchedInput = category?.title,
            mediaItems = emptyList(),
            isLoading = true
        )

        fetchMediaPage(reset = true)
    }

    private fun onSearchInputChanged(queryRaw: String) {
        if (!config.showSearch) return

        val query = queryRaw.trim()
        val mediaType = state.value.chosenMediaType
            ?: state.value.mediaTypes.firstOrNull()
            ?: MediaType.GIF

        if (query.isEmpty()) {
            currentFilter = initialFilter()
            canLoadMore = true

            _state.value = _state.value.copy(
                searchInput = "",
                lastSearchedInput = "",
                isLoading = true,
                mediaItems = emptyList()
            )

            fetchMediaPage(reset = true)
            return
        }

        currentFilter = query
        canLoadMore = true

        _state.value = _state.value.copy(
            searchInput = query,
            lastSearchedInput = query,
            isLoading = true,
            mediaItems = emptyList()
        )

        viewModelScope.launch {
            val result = repo.getMedia(mediaType, query)
            result
                .onSuccess { data ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        mediaItems = data.mediaItems
                    )
                }
                .onFailure { throwable ->
                    _state.value = _state.value.copy(isLoading = false)
                    emit(KlipyTrayEffect.ShowError(throwable.message ?: "Failed to load Klipy results"))
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

        viewModelScope.launch {
            try {
                val result = repo.getMedia(type, currentFilter)
                result
                    .onSuccess { data ->
                        val items = data.mediaItems
                        if (items.isEmpty()) {
                            canLoadMore = false
                        }

                        val newItems = if (reset) items else state.value.mediaItems + items
                        _state.value = _state.value.copy(
                            isLoading = false,
                            mediaItems = newItems
                        )
                    }
                    .onFailure { error ->
                        _state.value = _state.value.copy(isLoading = false)
                        emit(KlipyTrayEffect.ShowError(error.message ?: "Failed to load media."))
                    }
            } finally {
                isFetching = false
            }
        }
    }

    private fun emit(effect: KlipyTrayEffect) {
        _effects.trySend(effect)
    }

    companion object {
        fun factory(
            config: KlipyTrayConfig,
            repoProvider: () -> KlipyRepository = { KlipyUi.requireRepository() }
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(KlipyTrayViewModel::class.java)) {
                    return KlipyTrayViewModel(config, repoProvider) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
