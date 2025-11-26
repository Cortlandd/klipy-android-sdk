package com.klipy.conversationdemo.features.conversation

import com.cortlandwalker.ghettoxide.Reducer
import com.klipy.conversationdemo.features.conversation.ConversationEffect.*
import com.klipy.conversationdemo.features.conversation.model.ClipMessage
import com.klipy.conversationdemo.features.conversation.model.GifMessage
import com.klipy.conversationdemo.features.conversation.model.MessageUiModel
import com.klipy.conversationdemo.features.conversation.model.TextMessage
import com.klipy.klipy_ui.KlipyUi
import com.klipy.sdk.KlipyRepository
import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConversationReducer(
    private val conversationId: String
) : Reducer<ConversationState, ConversationAction, ConversationEffect>() {

    private val repo: KlipyRepository = KlipyUi.requireRepository()

    // Local paging / search state (we can’t read state directly from Reducer)
    private var didStart = false
    private var selectedMediaType: MediaType? = null
    private var currentFilter: String = ""
    private var canLoadMore: Boolean = true
    private var isFetching: Boolean = false
    private var searchJob: Job? = null
    private var lastSubmittedSearch: String? = null

    override fun onLoadAction(): ConversationAction = ConversationAction.ScreenStarted

    override suspend fun process(action: ConversationAction) {
        when (action) {
            ConversationAction.Back -> emit(Back)

            is ConversationAction.PickerToggleClicked -> {
                state { it.copy(isPickerVisible = !it.isPickerVisible) }
            }

            ConversationAction.ScreenStarted -> {
                if (!didStart) {
                    didStart = true
                    loadInitial()
                }
            }

            is ConversationAction.ScreenMeasured -> {
                // For now: no-op. If you later add a custom masonry layout that needs
                // screen dimensions, you can plumb it in here.
            }

            is ConversationAction.MessageTextChanged -> {
                state { it.copy(messageText = action.text) }
            }

            ConversationAction.SendClicked -> {
                onSendClicked()
            }

            is ConversationAction.MediaTypeSelected -> {
                onMediaTypeSelected(action.type)
            }

            is ConversationAction.CategorySelected -> {
                onCategorySelected(action.category)
            }

            is ConversationAction.SearchInputChanged -> {
                val query = action.query.trim()

                // If search is cleared, fall back to Trending for the current media type
                if (query.isEmpty()) {
                    val mediaType = currentState.chosenMediaType
                        ?: currentState.mediaTypes.firstOrNull()
                        ?: MediaType.GIF

                    selectedMediaType = mediaType
                    currentFilter = "trending"
                    canLoadMore = true
                    lastSubmittedSearch = null
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

                // Regular search (non-empty)
                val mediaType = currentState.chosenMediaType
                    ?: currentState.mediaTypes.firstOrNull()
                    ?: MediaType.GIF

                // update UI to “searching…”
                state {
                    it.copy(
                        searchInput = query,
                        lastSearchedInput = query,
                        isLoading = true,
                        mediaItems = emptyList()
                    )
                }

                // Direct search (single-page for now)
                val result = repo.getMedia(mediaType, query)

                result
                    .onSuccess { mediaData ->
                        state {
                            it.copy(
                                isLoading = false,
                                mediaItems = mediaData.mediaItems
                            )
                        }
                    }
                    .onFailure { throwable ->
                        state { it.copy(isLoading = false) }
                        emit(
                            ShowError(
                                throwable.message
                                    ?: "Failed to load Klipy results"
                            )
                        )
                    }
            }

            is ConversationAction.MediaItemClicked -> {
                onMediaItemClicked(action.item)
            }

            ConversationAction.RetryInitialLoad -> {
                didStart = false
                loadInitial()
            }

            is ConversationAction.TrayMediaSelected -> {
                val item = action.item
                val metaHigh = item.highQualityMetaData
                val metaLow = item.lowQualityMetaData
                val width = metaHigh?.width ?: metaLow?.width ?: 0
                val height = metaHigh?.height ?: metaLow?.height ?: 0
                val nextId = currentState.messages.size + 1

                val msg = when (item.mediaType) {
                    MediaType.GIF -> GifMessage(
                        id = nextId.toString(),
                        url = item.highQualityMetaData?.url
                            ?: item.lowQualityMetaData?.url
                            ?: "",
                        isFromCurrentUser = true,
                        width = width,
                        height = height
                    )

                    MediaType.CLIP -> ClipMessage(
                        id = nextId.toString(),
                        url = item.highQualityMetaData?.url
                            ?: item.lowQualityMetaData?.url
                            ?: "",
                        isFromCurrentUser = true,
                        width = width,
                        height = height
                    )

                    else -> GifMessage(
                        id = nextId.toString(),
                        url = item.highQualityMetaData?.url
                            ?: item.lowQualityMetaData?.url
                            ?: "",
                        isFromCurrentUser = true,
                        width = width,
                        height = height
                    )
                }

                state {
                    it.copy(
                        messages = it.messages + msg,
                        // hide the tray
                        isPickerVisible = false
                    )
                }
            }
        }
    }

    private fun loadInitial() {
        // 1) Media types (GIF / STICKER / CLIP / MEME) – keyboard wants all available
        val mediaTypes = listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP, MediaType.MEME)
        state {
            ConversationState(
                conversationId = conversationId,
                title = it.title,
                messages = it.messages,
                messageText = "",
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
                val firstType = mediaTypes.firstOrNull()

                selectedMediaType = firstType
                state { s ->
                    s.copy(
                        isLoading = true,
                        mediaTypes = mediaTypes,
                        chosenMediaType = firstType,
                        categories = emptyList(),
                        chosenCategory = null,
                        mediaItems = emptyList(),
                        searchInput = "",
                        lastSearchedInput = null
                    )
                }

                if (firstType == null) {
                    state { it.copy(isLoading = false) }
                    return@launch
                }

                // 2) Trending is our default filter – no need to ask categories first
                currentFilter = "trending"
                lastSubmittedSearch = null
                canLoadMore = true

                // Fetch trending immediately so the tray isn't empty
                val mediaResult = repo.getMedia(firstType, currentFilter)
                mediaResult
                    .onSuccess { page ->
                        state { s ->
                            s.copy(
                                isLoading = false,
                                mediaItems = page.mediaItems
                            )
                        }
                        canLoadMore = page.mediaItems.isNotEmpty()
                    }
                    .onFailure { error ->
                        state { it.copy(isLoading = false) }
                        emit(
                            ShowError(
                                error.message ?: "Failed to load Klipy media."
                            )
                        )
                    }

                // 3) Categories for that type (optional, don't gate Trending)
                val categoriesResult = repo.getCategories(firstType)
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
                        // Categories are non-critical; just surface an error message if you want
                        emit(
                            ShowError(
                                error.message ?: "Failed to load Klipy categories."
                            )
                        )
                    }
            } catch (t: Throwable) {
                state { it.copy(isLoading = false) }
                emit(
                    ShowError(
                        t.message ?: "Failed to load Klipy content."
                    )
                )
            }
        }
    }

    private fun onSendClicked() {
        state { current ->
            val trimmed = current.messageText.trim()
            if (trimmed.isBlank()) return@state current

            val newMessage: MessageUiModel = TextMessage(
                text = trimmed,
                isFromCurrentUser = true
            )

            current.copy(
                messages = current.messages + newMessage,
                messageText = ""
            )
        }
    }

    private fun onMediaTypeSelected(type: MediaType) {
        if (type == selectedMediaType) return

        selectedMediaType = type
        currentFilter = "trending"
        canLoadMore = true
        lastSubmittedSearch = null
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
            // 1) Load trending immediately for this type
            val mediaResult = repo.getMedia(type, currentFilter)
            mediaResult
                .onSuccess { page ->
                    state { s ->
                        s.copy(
                            isLoading = false,
                            mediaItems = page.mediaItems
                        )
                    }
                    canLoadMore = page.mediaItems.isNotEmpty()
                }
                .onFailure { error ->
                    state { it.copy(isLoading = false) }
                    emit(
                        ShowError(
                            error.message ?: "Failed to load media."
                        )
                    )
                }

            // 2) Load categories in the background (optional)
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
                        ShowError(
                            error.message ?: "Failed to load categories."
                        )
                    )
                }
        }
    }

    private fun onCategorySelected(category: Category?) {
        // If null, just clear to an empty filter and let the user search / fall back to trending via SearchInputChanged
        searchJob?.cancel()
        lastSubmittedSearch = category?.title
        currentFilter = category?.title ?: ""
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

    private suspend fun onMediaItemClicked(item: MediaItem) {
        emit(OpenMediaPreview(item))
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
                            ShowError(
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

// Simple debounce helper, equivalent to the original ViewModel’s behavior.
private fun CoroutineScope.debounceSearch(
    delayMillis: Long = 400L,
    block: () -> Unit
): Job = launch {
    delay(delayMillis)
    block()
}
