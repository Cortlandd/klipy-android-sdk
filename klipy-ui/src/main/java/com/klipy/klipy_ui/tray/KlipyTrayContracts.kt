package com.klipy.klipy_ui.tray

import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType


/**
 * UI state for the Klipy tray. This can be consumed by both XML and Compose layers.
 */
data class KlipyTrayState(
    val isLoading: Boolean = false,
    val mediaTypes: List<MediaType> = emptyList(),
    val chosenMediaType: MediaType? = null,
    val categories: List<Category> = emptyList(),
    val chosenCategory: Category? = null,
    val mediaItems: List<MediaItem> = emptyList(),
    val searchInput: String = "",
    val lastSearchedInput: String? = null
)

/**
 * Actions that drive the Klipy tray reducer.
 *
 * Both XML and Compose hosts dispatch these actions.
 */
sealed class KlipyTrayAction {
    data object ScreenStarted : KlipyTrayAction()
    data object RetryInitialLoad : KlipyTrayAction()

    data class MediaTypeSelected(val type: MediaType) : KlipyTrayAction()
    data class CategorySelected(val category: Category?) : KlipyTrayAction()

    /**
     * Search text changed. Hosts may debounce before dispatching if desired.
     */
    data class SearchInputChanged(val query: String) : KlipyTrayAction()

    data class MediaItemClicked(val item: MediaItem) : KlipyTrayAction()

    /**
     * Request to load the next page of media.
     * XML host typically dispatches this from RecyclerView scroll listener.
     */
    data class LoadNextPage(
        val visibleItemCount: Int,
        val totalItemCount: Int,
        val firstVisibleItem: Int
    ) : KlipyTrayAction()
}

/**
 * One-off effects from the tray reducer.
 */
sealed class KlipyTrayEffect {
    data class ShowError(val message: String) : KlipyTrayEffect()
    data class MediaChosen(val item: MediaItem) : KlipyTrayEffect()
}

/**
 * Configuration for the Klipy tray (keyboard-like panel).
 *
 * This is shared between XML (Fragment) and Compose.
 *
 * @property mediaTypes The tabs to show (e.g., GIF/STICKER/CLIP/MEME).
 * @property initialMediaType Which tab to select on first load.
 * @property columns Number of columns in the media grid.
 * @property showTrending If true, the tray will load "trending" on first load / clear search.
 * @property showRecents If true and showTrending is false, the tray will load "recent".
 *                       If both are true, trending wins.
 * @property showCategories Whether to fetch and show Klipy categories (Trending, Reactions, etc.).
 * @property showSearch Whether to show the search input and enable query-based search.
 */
data class KlipyTrayConfig(
    val mediaTypes: List<MediaType> = listOf(
        MediaType.GIF,
        MediaType.STICKER,
        MediaType.CLIP,
        MediaType.MEME
    ),
    val initialMediaType: MediaType = MediaType.GIF,
    val columns: Int = 3,
    val showTrending: Boolean = true,
    val showRecents: Boolean = false,
    val showCategories: Boolean = false,
    val showSearch: Boolean = true
)
