package com.klipy.klipy_ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cortlandwalker.klipy_ui.databinding.FragmentKlipyPickerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.klipy.sdk.model.MediaData
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.singularName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KlipyPickerDialogFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_CONFIG = "klipy_config"

        fun newInstance(config: KlipyPickerConfig): KlipyPickerDialogFragment {
            return KlipyPickerDialogFragment().apply {
                arguments = bundleOf(ARG_CONFIG to config)
            }
        }
    }

    var listener: KlipyPickerListener? = null

    private var _binding: FragmentKlipyPickerBinding? = null
    private val binding get() = _binding!!

    private lateinit var config: KlipyPickerConfig
    private val repo get() = KlipyUi.requireRepository()

    private val adapter = KlipyMediaAdapter { onItemClicked(it) }

    private var currentType: MediaType? = null
    private var currentSearchTerm: String? = null
    private var loadJob: Job? = null

    // Paging state
    private val currentItems = mutableListOf<MediaItem>()
    private var isLoading = false
    private var hasMore = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        config = requireArguments().getParcelable(ARG_CONFIG)
            ?: error("KlipyPickerConfig missing")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKlipyPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTabs()
        setupRecycler()
        setupSearch()

        // initial load
        val initialType = config.initialMediaType
            .takeIf { it in config.mediaTypes }
            ?: config.mediaTypes.first()
        selectMediaType(initialType)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.onDismissed(currentType)
    }

    // region UI setup

    private fun setupTabs() {
        val tabLayout = binding.tabMediaTypes
        config.mediaTypes.forEach { type ->
            tabLayout.addTab(tabLayout.newTab().setText(type.singularName()))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val index = tab?.position ?: return
                val type = config.mediaTypes.getOrNull(index) ?: return
                selectMediaType(type)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // optional: scroll to top or refresh
            }
        })
    }

    private fun setupRecycler() {
        val layoutManager = GridLayoutManager(requireContext(), config.columns)

        binding.recyclerMedia.apply {
            this.layoutManager = layoutManager
            adapter = this@KlipyPickerDialogFragment.adapter

            // Paging on scroll
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return // only when scrolling down

                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    val threshold = 6 // how early to start loading more
                    val shouldLoadMore =
                        !isLoading &&
                                hasMore &&
                                totalItemCount > 0 &&
                                visibleItemCount + firstVisibleItemPosition >= totalItemCount - threshold

                    if (shouldLoadMore) {
                        loadNextPage()
                    }
                }
            })
        }
    }

    private fun setupSearch() {
        binding.inputSearch.doOnTextChanged { text, _, _, _ ->
            val term = text?.toString()?.trim().orEmpty()
            currentSearchTerm = term.ifBlank { null }

            listener?.didSearchTerm(term)

            debounceInitialLoad()
        }
    }

    // endregion

    // region Paging / loading

    private fun debounceInitialLoad() {
        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(250) // simple debounce
            loadFirstPage()
        }
    }

    private fun selectMediaType(type: MediaType) {
        if (type == currentType) return
        currentType = type

        // Explicitly reset repository paging for this type
        repo.reset(type)

        // Reset local paging state
        currentItems.clear()
        adapter.submitList(currentItems.toList())
        hasMore = true

        loadFirstPage()
    }

    private fun loadFirstPage() {
        val type = currentType ?: return

        // reset local list & flags
        isLoading = false
        hasMore = true
        currentItems.clear()
        adapter.submitList(currentItems.toList())

        // let repository reset for new filter automatically
        loadPage(reset = true)
    }

    private fun loadNextPage() {
        val type = currentType ?: return
        if (!hasMore || isLoading) return

        loadPage(reset = false)
    }

    /**
     * Core loading function.
     * - If [reset] = true: clears items, treats response as first page.
     * - If [reset] = false: appends to existing list (next pages).
     */
    private fun loadPage(reset: Boolean) {
        val type = currentType ?: return

        val filter = when {
            currentSearchTerm.isNullOrBlank() && config.showTrending -> "trending"
            currentSearchTerm.isNullOrBlank() && config.showRecents -> "recent"
            currentSearchTerm.isNullOrBlank() -> "trending"
            else -> currentSearchTerm!!
        }

        if (isLoading) return
        isLoading = true

        // optional simple shimmer/visibility
        if (reset) {
            binding.recyclerMedia.visibility = View.INVISIBLE
            binding.recyclerMedia.alpha = 0f
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val result: Result<MediaData> = repo.getMedia(type, filter)

            result
                .onSuccess { data ->
                    val pageItems = data.mediaItems

                    if (reset) {
                        currentItems.clear()
                    }

                    if (pageItems.isEmpty()) {
                        // No more data from SDK; prevent further calls
                        hasMore = false
                    } else {
                        currentItems.addAll(pageItems)
                    }

                    adapter.submitList(currentItems.toList())

                    binding.recyclerMedia.visibility = View.VISIBLE
                    if (reset) {
                        binding.recyclerMedia.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start()
                    }
                }
                .onFailure {
                    // on error: stop infinite retry
                    hasMore = false
                }

            isLoading = false
        }
    }

    // endregion

    private fun onItemClicked(item: MediaItem) {
        listener?.onMediaSelected(item, currentSearchTerm)
        dismiss()
    }
}