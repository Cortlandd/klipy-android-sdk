package com.klipy.klipy_ui.picker

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.graphics.Color
import android.view.KeyEvent
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cortlandwalker.klipy_ui.databinding.FragmentKlipyPickerBinding
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.klipy.klipy_ui.KlipyUi
import com.klipy.sdk.KlipyRepository
import com.klipy.sdk.KlipySdk
import com.klipy.sdk.model.MediaData
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.singularName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Bottom sheet dialog that shows a Klipy-powered media picker:
 * GIFs, stickers, clips and memes.
 *
 * Usage:
 *
 * ```kotlin
 * class ChatFragment : Fragment(), KlipyPickerListener {
 *
 *   private fun openKlipyPicker() {
 *     val config = KlipyPickerConfig(
 *       mediaTypes = listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP, MediaType.MEME),
 *       columns = 3,
 *       showRecents = false,
 *       showTrending = true,
 *       initialMediaType = MediaType.GIF
 *     )
 *
 *     KlipyPickerDialogFragment
 *       .newInstance(config)
 *       .also { it.listener = this }
 *       .show(childFragmentManager, "klipy_picker")
 *   }
 *
 *   override fun onMediaSelected(item: MediaItem, searchTerm: String?) {
 *     // Do something with selection
 *   }
 * }
 * ```
 *
 * **Important:** Before using this fragment you must configure [KlipyUi]
 * with a [com.klipy.sdk.KlipyRepository] instance (typically in `Application.onCreate`).
 */
class KlipyPickerDialogFragment : BottomSheetDialogFragment() {

    private data class PickerPalette(
        val backgroundColor: Int,
        val surfaceColor: Int,
        val primaryColor: Int,
        val loadingIndicatorColor: Int,
        val onSurfaceColor: Int,
        val secondaryTextColor: Int,
        val outlineColor: Int,
        val searchFieldColor: Int,
        val buttonColor: Int,
        val onButtonColor: Int
    )

    companion object {
        private val KLIPY_WEBSITE_URI: Uri = Uri.parse("https://klipy.com/en-US")
        private const val SEARCH_DEBOUNCE_MS = 350L
        private const val ARG_CONFIG = "klipy_config"
        private const val ARG_SECRET_KEY = "klipy_secret_key"
        private const val ARG_BASE_API_URL = "klipy_base_api_url"
        private const val ARG_ENABLE_LOGGING = "klipy_enable_logging"

        fun newInstance(config: KlipyPickerConfig): KlipyPickerDialogFragment {
            return KlipyPickerDialogFragment().apply {
                arguments = bundleOf(ARG_CONFIG to config)
            }
        }

        /**
         * Picker-only usage: pass your Klipy API key directly to the fragment.
         * This lets devs use the picker without calling KlipyUi.configure(repo) first.
         */
        fun newInstance(
            config: KlipyPickerConfig,
            secretKey: String,
            baseApiUrl: String = "https://api.klipy.com/api/v1/",
            enableLogging: Boolean = false
        ): KlipyPickerDialogFragment {
            return KlipyPickerDialogFragment().apply {
                arguments = bundleOf(
                    ARG_CONFIG to config,
                    ARG_SECRET_KEY to secretKey,
                    ARG_BASE_API_URL to baseApiUrl,
                    ARG_ENABLE_LOGGING to enableLogging
                )
            }
        }
    }

    var listener: KlipyPickerListener? = null

    private var _binding: FragmentKlipyPickerBinding? = null
    private val binding get() = _binding!!

    private lateinit var config: KlipyPickerConfig

    private val repo: KlipyRepository by lazy {
        // Prefer a globally-configured repository if the host app provided one.
        KlipyUi.getRepositoryOrNull() ?: buildRepoFromArgs()
    }

    private fun buildRepoFromArgs(): KlipyRepository {
        val args = requireArguments()

        val secretKey = args.getString(ARG_SECRET_KEY)
            ?: error(
                "Missing secretKey. Either call KlipyUi.configure(repo) or use " +
                        "KlipyPickerDialogFragment.newInstance(config, secretKey, ...)"
            )

        val baseApiUrl = args.getString(ARG_BASE_API_URL) ?: "https://api.klipy.com/api/v1/"
        val enableLogging = args.getBoolean(ARG_ENABLE_LOGGING, false)

        return KlipySdk.create(
            context = requireContext().applicationContext,
            secretKey = secretKey,
            baseApiUrl = baseApiUrl,
            enableLogging = enableLogging
        )
    }

    private val adapter by lazy {
        KlipyMediaAdapter(
            loadingIndicatorColor = resolvePickerPalette().loadingIndicatorColor,
            onClick = { onItemClicked(it) }
        )
    }

    private var currentType: MediaType? = null
    private var currentFilter: String? = null

    // Paging state
    private val currentItems = mutableListOf<MediaItem>()
    private var isLoading = false
    private var hasMore = true
    private var loadJob: Job? = null
    private var searchDebounceJob: Job? = null
    private var suppressSearchTextChanges = false

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
            dialog.findViewById<View>(R.id.design_bottom_sheet)
                ?: return

        bottomSheet.post {
            val behavior = BottomSheetBehavior.from(bottomSheet)
            bottomSheet.setBackgroundColor(resolvePickerPalette().backgroundColor)

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
        applyPickerTheme()
        setupTabs()
        setupRecycler()
        setupSearch()
        setupRetry()
        setupPoweredByFooter()

        // Set initial media type but do NOT auto-load
        val initialType = config.initialMediaType
            .takeIf { it in config.mediaTypes }
            ?: config.mediaTypes.first()
        currentType = initialType
        binding.tabMediaTypes.getTabAt(config.mediaTypes.indexOf(initialType))?.select()

        when {
            config.showTrending -> showTrending()
            config.showRecents -> showRecents()
            else -> clearItems()
        }
    }

    override fun onDestroyView() {
        searchDebounceJob?.cancel()
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

        edit.addTextChangedListener { text ->
            if (suppressSearchTextChanges) return@addTextChangedListener

            val term = text?.toString().orEmpty()
            searchDebounceJob?.cancel()
            searchDebounceJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                applySearchTerm(term, hideKeyboard = false)
            }
        }

        edit.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                submitSearch(v)
                true
            } else {
                false
            }
        }

        edit.setOnEditorActionListener { v, actionId, event ->
            if (shouldSubmitSearch(actionId, event)) {
                submitSearch(v)
                true
            } else {
                false
            }
        }
    }

    private fun shouldSubmitSearch(actionId: Int, event: KeyEvent?): Boolean {
        if (
            actionId == EditorInfo.IME_ACTION_SEARCH ||
            actionId == EditorInfo.IME_ACTION_DONE ||
            actionId == EditorInfo.IME_ACTION_GO
        ) {
            return true
        }

        return actionId == EditorInfo.IME_NULL &&
            event?.keyCode == KeyEvent.KEYCODE_ENTER &&
            event.action == KeyEvent.ACTION_DOWN
    }

    private fun submitSearch(view: View) {
        searchDebounceJob?.cancel()
        applySearchTerm(
            rawTerm = binding.inputSearch.text?.toString().orEmpty(),
            hideKeyboard = true
        )
        hideKeyboard(view)
    }

    private fun applySearchTerm(rawTerm: String, hideKeyboard: Boolean) {
        val normalizedTerm = rawTerm.trim()
        val newTerm = normalizedTerm.takeIf { it.isNotEmpty() }

        currentFilter = newTerm
        newTerm?.let { listener?.didSearchTerm(it) }

        if (hideKeyboard) {
            hideKeyboard(binding.inputSearch)
        }

        if (newTerm != null) {
            startNewSearch()
        } else {
            showDefaultFeed()
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun setupRetry() {
        binding.buttonRetry.setOnClickListener {
            retryCurrentRequest()
        }
    }

    private fun setupPoweredByFooter() {
        binding.footerPoweredBy.setOnClickListener {
            openKlipyWebsite()
        }
    }

    private fun applyPickerTheme() {
        val palette = resolvePickerPalette()

        binding.root.setBackgroundColor(palette.backgroundColor)
        binding.layoutContentState.setBackgroundColor(palette.backgroundColor)
        binding.recyclerMedia.setBackgroundColor(palette.backgroundColor)
        binding.footerPoweredBy.setBackgroundColor(palette.surfaceColor)
        binding.viewFooterDivider.setBackgroundColor(palette.outlineColor)

        binding.textOfflineTitle.setTextColor(palette.onSurfaceColor)
        binding.textOfflineMessage.setTextColor(palette.secondaryTextColor)
        binding.textPoweredBy.setTextColor(palette.onSurfaceColor)
        binding.progressLoading.indeterminateTintList = ColorStateList.valueOf(palette.primaryColor)

        binding.buttonRetry.backgroundTintList = ColorStateList.valueOf(palette.buttonColor)
        binding.buttonRetry.setTextColor(palette.onButtonColor)

        binding.tabMediaTypes.setBackgroundColor(palette.surfaceColor)
        binding.tabMediaTypes.setSelectedTabIndicatorColor(palette.primaryColor)
        binding.tabMediaTypes.setTabTextColors(
            palette.secondaryTextColor,
            palette.primaryColor
        )

        binding.searchInputLayout.apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_FILLED
            setBoxBackgroundColor(palette.searchFieldColor)
            val hintColorStateList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_focused),
                    intArrayOf()
                ),
                intArrayOf(
                    palette.secondaryTextColor,
                    palette.secondaryTextColor
                )
            )
            defaultHintTextColor = hintColorStateList
            setHintTextColor(hintColorStateList)
            setBoxStrokeColorStateList(
                ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_focused),
                        intArrayOf()
                    ),
                    intArrayOf(
                        palette.primaryColor,
                        palette.outlineColor
                    )
                )
            )
        }
        binding.inputSearch.setTextColor(palette.onSurfaceColor)
        binding.inputSearch.setHintTextColor(palette.secondaryTextColor)
        binding.inputSearch.highlightColor = palette.primaryColor
    }

    private fun resolvePickerPalette(): PickerPalette {
        val defaults = when (resolveThemeMode()) {
            KlipyPickerThemeMode.DARK -> darkPalette()
            KlipyPickerThemeMode.LIGHT -> lightPalette()
            KlipyPickerThemeMode.AUTOMATIC -> lightPalette()
        }
        val overrides = config.colors

        return defaults.copy(
            backgroundColor = overrides?.backgroundColor ?: defaults.backgroundColor,
            surfaceColor = overrides?.surfaceColor ?: defaults.surfaceColor,
            primaryColor = overrides?.primaryColor ?: defaults.primaryColor,
            onSurfaceColor = overrides?.onSurfaceColor ?: defaults.onSurfaceColor,
            secondaryTextColor = overrides?.secondaryTextColor ?: defaults.secondaryTextColor,
            outlineColor = overrides?.outlineColor ?: defaults.outlineColor,
            searchFieldColor = overrides?.searchFieldColor ?: defaults.searchFieldColor,
            buttonColor = overrides?.buttonColor ?: defaults.buttonColor,
            onButtonColor = overrides?.onButtonColor ?: defaults.onButtonColor
        )
    }

    private fun resolveThemeMode(): KlipyPickerThemeMode {
        return when (config.themeMode) {
            KlipyPickerThemeMode.AUTOMATIC -> {
                val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
                if (isDarkMode) {
                    KlipyPickerThemeMode.DARK
                } else {
                    KlipyPickerThemeMode.LIGHT
                }
            }
            else -> config.themeMode
        }
    }

    private fun lightPalette() = PickerPalette(
        backgroundColor = Color.parseColor("#FFF6F4EF"),
        surfaceColor = Color.WHITE,
        primaryColor = Color.parseColor("#FF111827"),
        loadingIndicatorColor = Color.parseColor("#FFF7C948"),
        onSurfaceColor = Color.parseColor("#FF111827"),
        secondaryTextColor = Color.parseColor("#FF6B7280"),
        outlineColor = Color.parseColor("#FFD1D5DB"),
        searchFieldColor = Color.parseColor("#FFF3F4F6"),
        buttonColor = Color.parseColor("#FF111827"),
        onButtonColor = Color.WHITE
    )

    private fun darkPalette() = PickerPalette(
        backgroundColor = Color.parseColor("#FF111315"),
        surfaceColor = Color.parseColor("#FF1A1D21"),
        primaryColor = Color.parseColor("#FFF7C948"),
        loadingIndicatorColor = Color.parseColor("#FFF7C948"),
        onSurfaceColor = Color.parseColor("#FFF9FAFB"),
        secondaryTextColor = Color.parseColor("#FF9CA3AF"),
        outlineColor = Color.parseColor("#FF374151"),
        searchFieldColor = Color.parseColor("#FF161A1E"),
        buttonColor = Color.parseColor("#FFF7C948"),
        onButtonColor = Color.parseColor("#FF111827")
    )

    private fun openKlipyWebsite() {
        val intent = Intent(Intent.ACTION_VIEW, KLIPY_WEBSITE_URI).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Ignore devices without a browser handler.
        }
    }

    /** Clear items and reset paging flags without network call. */
    private fun clearItems() {
        currentItems.clear()
        adapter.submitList(currentItems.toList())
        hasMore = false
        isLoading = false
        showContentState()
    }

    private fun selectMediaType(type: MediaType) {
        if (type == currentType) return
        currentType = type
        repo.reset(type)

        // Only trigger network if we have a search term
        if (!currentFilter.isNullOrBlank()) {
            startNewSearch()
        } else {
            clearItems()
        }
    }

    private fun startNewSearch() {
        val filter = currentFilter

        if (filter.isNullOrBlank()) {
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
        val filter = currentFilter

        if (filter.isNullOrBlank()) {
            clearItems()
            return
        }

        if (isLoading || !hasMore) return
        if (!isNetworkAvailable()) {
            handleConnectivityFailure(reset)
            return
        }

        isLoading = true

        if (reset) {
            showLoadingState()
        }

        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val result: Result<MediaData> = repo.getMedia(type, filter)

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

                    showContentState()
                    if (reset) {
                        binding.recyclerMedia.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start()
                    }
                }
                .onFailure { error ->
                    hasMore = false
                    if (shouldShowOfflineState(error, reset)) {
                        showOfflineState()
                    } else {
                        showContentState()
                    }
                }

            isLoading = false
        }
    }

    private fun showTrending() {
        currentFilter = "trending" // maps to TRENDING in MediaDataSource
        updateSearchField("")
        startNewSearch()
    }

    private fun showRecents() {
        currentFilter = "recent" // maps to RECENT in MediaDataSource
        updateSearchField("")
        startNewSearch()
    }

    private fun showDefaultFeed() {
        when {
            config.showTrending -> showTrending()
            config.showRecents -> showRecents()
            else -> clearItems()
        }
    }

    private fun updateSearchField(value: String) {
        suppressSearchTextChanges = true
        binding.inputSearch.setText(value)
        binding.inputSearch.setSelection(value.length)
        suppressSearchTextChanges = false
    }

    private fun onItemClicked(item: MediaItem) {
        listener?.onMediaSelected(item, currentFilter)
        dismiss()
    }

    private fun retryCurrentRequest() {
        hasMore = true
        startNewSearch()
    }

    private fun showLoadingState() {
        binding.layoutOfflineState.visibility = View.GONE
        binding.recyclerMedia.visibility = View.VISIBLE
        binding.progressLoading.visibility = View.VISIBLE
    }

    private fun showContentState() {
        binding.layoutOfflineState.visibility = View.GONE
        binding.recyclerMedia.visibility = View.VISIBLE
        binding.progressLoading.visibility = View.GONE
    }

    private fun showOfflineState() {
        binding.layoutOfflineState.visibility = View.VISIBLE
        binding.recyclerMedia.visibility = View.GONE
        binding.progressLoading.visibility = View.GONE
    }

    private fun shouldShowOfflineState(
        throwable: Throwable,
        reset: Boolean
    ): Boolean {
        if (!reset && currentItems.isNotEmpty()) return false
        return !isNetworkAvailable() || throwable.isConnectivityFailure()
    }

    private fun handleConnectivityFailure(reset: Boolean) {
        hasMore = false
        isLoading = false

        if (reset || currentItems.isEmpty()) {
            showOfflineState()
        } else {
            showContentState()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager
            ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

}
