package com.klipy.klipy_ui.tray

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cortlandwalker.ghettoxide.BaseViewModel
import com.cortlandwalker.ghettoxide.StoreViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.klipy.klipy_ui.picker.KlipyMediaAdapter
import com.klipy.sdk.R
import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Callbacks from the Klipy tray to the host (Activity/Fragment).
 */
interface KlipyTrayListener {
    fun onKlipyMediaChosen(item: MediaItem)
    fun onKlipyTrayError(message: String)
}

/**
 * Pure View-based tray for XML/ViewBinding hosts.
 *
 * This view:
 *  - Inflates its own layout
 *  - Renders [KlipyTrayState]
 *  - Dispatches [KlipyTrayAction] to a Ghettoxide StoreViewModel
 */
class KlipyTrayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val mediaTypeGroup: ChipGroup
    private val categoryGroup: ChipGroup
    private val searchInput: TextInputEditText
    private val mediaRecycler: RecyclerView
    private val loadingBar: ProgressBar

    private lateinit var adapter: KlipyMediaAdapter
    private var config: KlipyTrayConfig = KlipyTrayConfig()

    private var store: BaseViewModel<KlipyTrayState, KlipyTrayAction, KlipyTrayEffect>? = null

    var listener: KlipyTrayListener? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(com.cortlandwalker.klipy_ui.R.layout.view_klipy_tray, this, true)

        mediaTypeGroup = findViewById(com.cortlandwalker.klipy_ui.R.id.mediaTypeGroup)
        categoryGroup = findViewById(com.cortlandwalker.klipy_ui.R.id.categoryGroup)
        searchInput = findViewById(com.cortlandwalker.klipy_ui.R.id.searchInput)
        mediaRecycler = findViewById(com.cortlandwalker.klipy_ui.R.id.mediaRecycler)
        loadingBar = findViewById(com.cortlandwalker.klipy_ui.R.id.loadingBar)

        setupRecycler()
    }

    private fun setupRecycler() {
        adapter = KlipyMediaAdapter { item ->
            store?.postAction(KlipyTrayAction.MediaItemClicked(item))
        }
        mediaRecycler.adapter = adapter
        mediaRecycler.layoutManager = GridLayoutManager(context, config.columns)

        mediaRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val lm = rv.layoutManager as? GridLayoutManager ?: return

                val visibleItemCount = lm.childCount
                val totalItemCount = lm.itemCount
                val firstVisibleItem = lm.findFirstVisibleItemPosition()

                store?.postAction(
                    KlipyTrayAction.LoadNextPage(
                        visibleItemCount = visibleItemCount,
                        totalItemCount = totalItemCount,
                        firstVisibleItem = firstVisibleItem
                    )
                )
            }
        })
    }

    /**
     * Bind this view to a Ghettoxide store.
     *
     * Host is responsible for creating the StoreViewModel with the same config.
     */
    fun bindToStore(
        lifecycleOwner: LifecycleOwner,
        store: BaseViewModel<KlipyTrayState, KlipyTrayAction, KlipyTrayEffect>,
        config: KlipyTrayConfig = KlipyTrayConfig()
    ) {
        this.store = store
        this.config = config

        (mediaRecycler.layoutManager as? GridLayoutManager)?.spanCount = config.columns

        setupMediaTypeChips(config.mediaTypes)

        searchInput.addTextChangedListener { text ->
            store.postAction(
                KlipyTrayAction.SearchInputChanged(text?.toString().orEmpty())
            )
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(
                androidx.lifecycle.Lifecycle.State.STARTED
            ) {
                launch {
                    store.state.collectLatest { renderState(it) }
                }
                launch {
                    store.effects.collectLatest { effect ->
                        when (effect) {
                            is KlipyTrayEffect.ShowError ->
                                listener?.onKlipyTrayError(effect.message)

                            is KlipyTrayEffect.MediaChosen ->
                                listener?.onKlipyMediaChosen(effect.item)
                        }
                    }
                }
            }
        }

        // 🔑 Kick off initial load
        store.reducer.onLoadAction()?.let(store::postAction)
    }

    private fun setupMediaTypeChips(mediaTypes: List<MediaType>) {
        mediaTypeGroup.removeAllViews()
        mediaTypes.forEach { type ->
            val chip = Chip(context).apply {
                text = type.name.uppercase()
                isCheckable = true
                setOnClickListener {
                    store?.postAction(KlipyTrayAction.MediaTypeSelected(type))
                }
            }
            mediaTypeGroup.addView(chip)
        }
    }

    private fun renderState(state: KlipyTrayState) {
        loadingBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        val selectedType = state.chosenMediaType
        for (i in 0 until mediaTypeGroup.childCount) {
            val chip = mediaTypeGroup.getChildAt(i) as Chip
            val type = state.mediaTypes.getOrNull(i)
            chip.isChecked = (type == selectedType)
        }

        renderCategories(state.categories, state.chosenCategory)

        if (searchInput.text?.toString() != state.searchInput) {
            searchInput.setText(state.searchInput)
            searchInput.setSelection(state.searchInput.length)
        }

        adapter.submitList(state.mediaItems)
    }

    private fun renderCategories(categories: List<Category>, selected: Category?) {
        if (!config.showCategories) {
            categoryGroup.visibility = View.GONE
            return
        }

        categoryGroup.visibility = View.VISIBLE
        categoryGroup.removeAllViews()

        val allChip = Chip(context).apply {
            text = "All"
            isCheckable = true
            isChecked = selected == null
            setOnClickListener {
                store?.postAction(KlipyTrayAction.CategorySelected(null))
            }
        }
        categoryGroup.addView(allChip)

        categories.forEach { cat ->
            val chip = Chip(context).apply {
                text = cat.title
                isCheckable = true
                isChecked = cat == selected
                setOnClickListener {
                    store?.postAction(KlipyTrayAction.CategorySelected(cat))
                }
            }
            categoryGroup.addView(chip)
        }
    }
}
