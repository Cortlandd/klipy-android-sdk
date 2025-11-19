package com.klipy.klipy_ui

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cortlandwalker.klipy_ui.databinding.FragmentKlipyPickerBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.klipy.sdk.model.MediaData
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.singularName
import kotlinx.coroutines.Job
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

    // Paging state
    private val currentItems = mutableListOf<MediaItem>()
    private var isLoading = false
    private var hasMore = true
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        config = requireArguments().getParcelable(ARG_CONFIG)
            ?: KlipyPickerConfig()
    }

    override fun onStart() {
        super.onStart()

        // Make the bottom sheet take ~90% of screen height and expand
        val dialog = dialog ?: return
        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return

        bottomSheet.post {
            val behavior = BottomSheetBehavior.from(bottomSheet)

            val displayMetrics = resources.displayMetrics
            val targetHeight = (displayMetrics.heightPixels * 0.9f).toInt()

            bottomSheet.layoutParams.height = targetHeight
            bottomSheet.requestLayout()

            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
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

        // Set initial media type but do NOT auto-load
        val initialType = config.initialMediaType
            .takeIf { it in config.mediaTypes }
            ?: config.mediaTypes.first()
        currentType = initialType
        binding.tabMediaTypes.getTabAt(config.mediaTypes.indexOf(initialType))?.select()

        clearItems()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.onDismissed(currentType)
    }

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

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun setupRecycler() {
        val layoutManager = GridLayoutManager(requireContext(), config.columns)

        binding.recyclerMedia.apply {
            this.layoutManager = layoutManager
            adapter = this@KlipyPickerDialogFragment.adapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return

                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    val threshold = 6

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
        val edit = binding.inputSearch

        edit.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_NULL
            ) {
                val term = v.text?.toString()?.trim().orEmpty()
                val newTerm = term.takeIf { it.isNotEmpty() }

                currentSearchTerm = newTerm
                newTerm?.let { listener?.didSearchTerm(it) }

                // Hide keyboard
                val imm = requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                startNewSearch()
                true
            } else {
                false
            }
        }
    }

    /** Clear items and reset paging flags without network call. */
    private fun clearItems() {
        currentItems.clear()
        adapter.submitList(currentItems.toList())
        hasMore = false
        isLoading = false
        binding.progressLoading.visibility = View.GONE
    }

    private fun selectMediaType(type: MediaType) {
        if (type == currentType) return
        currentType = type
        repo.reset(type)

        // Only trigger network if we have a search term
        if (!currentSearchTerm.isNullOrBlank()) {
            startNewSearch()
        } else {
            clearItems()
        }
    }

    private fun startNewSearch() {
        val term = currentSearchTerm

        if (term.isNullOrBlank()) {
            clearItems()
            return
        }

        // Reset paging state
        hasMore = true
        isLoading = false
        currentItems.clear()
        adapter.submitList(currentItems.toList())

        loadPage(reset = true)
    }

    private fun loadNextPage() {
        if (!hasMore || isLoading) return
        loadPage(reset = false)
    }

    private fun loadPage(reset: Boolean) {
        val type = currentType ?: return
        val term = currentSearchTerm ?: return

        if (isLoading) return
        isLoading = true

        if (reset) {
            binding.progressLoading.visibility = View.VISIBLE
        }

        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val result: Result<MediaData> = repo.getMedia(type, term)

            result
                .onSuccess { data ->
                    val pageItems = data.mediaItems

                    if (reset) {
                        currentItems.clear()
                    }

                    if (pageItems.isEmpty()) {
                        hasMore = false
                    } else {
                        currentItems.addAll(pageItems)
                    }

                    adapter.submitList(currentItems.toList())

                    binding.progressLoading.visibility = View.GONE
                    binding.recyclerMedia.visibility = View.VISIBLE
                    if (reset) {
                        binding.recyclerMedia.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start()
                    }
                }
                .onFailure {
                    hasMore = false
                    binding.progressLoading.visibility = View.GONE
                    binding.recyclerMedia.visibility = View.VISIBLE
                }

            isLoading = false
        }
    }

    private fun onItemClicked(item: MediaItem) {
        listener?.onMediaSelected(item, currentSearchTerm)
        dismiss()
    }
}
