package com.klipy.klipy_demo.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import com.cortlandwalker.ghettoxide.ReducerFragment
import com.klipy.klipy_demo.BuildConfig
import com.klipy.klipy_ui.KlipyUi
import com.klipy.klipy_ui.picker.KlipyPickerConfig
import com.klipy.klipy_ui.picker.KlipyPickerDialogFragment
import com.klipy.klipy_ui.picker.KlipyPickerListener
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.ShareTriggerOptions
import kotlinx.coroutines.launch

class HomeFragment : ReducerFragment<HomeState, HomeAction, HomeEffect, HomeReducer>() {
    override var reducer: HomeReducer = HomeReducer()
    override val initialState: HomeState = HomeState()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val s = vm.state.collectAsState().value
                HomeScreen(
                    state = s,
                    reducer = reducer
                )
            }
        }
    }

    override fun onEffect(effect: HomeEffect) {
        when(effect) {
            HomeEffect.OpenPicker -> {
                openKlipyPicker()
            }
            is HomeEffect.ShowMessage -> {
                Toast
                    .makeText(requireContext(), effect.message, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun openKlipyPicker() {
        if (BuildConfig.KLIPY_API_KEY.isBlank()) {
            Toast
                .makeText(
                    requireContext(),
                    "Set KLIPY_API_KEY in your Gradle properties before opening the sample picker.",
                    Toast.LENGTH_LONG
                )
                .show()
            return
        }

        val config = KlipyPickerConfig(
            columns = 3,
            showTrending = true,
            showRecents = true,
            mediaTypes = listOf(
                MediaType.GIF,
                MediaType.STICKER,
                MediaType.CLIP,
                MediaType.MEME
            )
        )

        val dialog = KlipyPickerDialogFragment.newInstance(config)
        dialog.listener = object : KlipyPickerListener {

            override fun onMediaSelected(item: MediaItem, searchTerm: String?) {
                vm.postAction(HomeAction.MediaSelected(item))
                searchTerm
                    ?.takeIf { it.isNotBlank() }
                    ?.let { vm.postAction(HomeAction.SearchTermUpdated(it)) }

                lifecycleScope.launch {
                    KlipyUi.requireRepository().triggerShare(
                        mediaType = item.mediaType,
                        slug = item.id,
                        options = ShareTriggerOptions(
                            searchQuery = searchTerm?.takeIf { it.isNotBlank() }
                        )
                    )
                }
            }

            override fun onDismissed(lastContentType: MediaType?) {
                vm.postAction(HomeAction.PickerDismissed)
            }

            override fun didSearchTerm(term: String) {
                vm.postAction(HomeAction.SearchTermUpdated(term))
            }
        }

        dialog.show(childFragmentManager, "klipy_picker")
    }
}
