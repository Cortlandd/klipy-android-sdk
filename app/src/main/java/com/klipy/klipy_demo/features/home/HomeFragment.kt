package com.klipy.klipy_demo.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.cortlandwalker.ghettoxide.Reducer
import com.cortlandwalker.ghettoxide.ReducerFragment
import com.klipy.klipy_ui.KlipyPickerConfig
import com.klipy.klipy_ui.KlipyPickerDialogFragment
import com.klipy.klipy_ui.KlipyPickerListener
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType

class HomeFragment : ReducerFragment<HomeState, HomeAction, HomeEffect>() {
    override val reducer: Reducer<HomeState, HomeAction, HomeEffect> = HomeReducer()
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
                    dispatch = vm::postAction
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
        val config = KlipyPickerConfig(
            // Defaults already give you GIF/STICKER/CLIP; tweak as desired
            columns = 3,
            showRecents = true,
            showTrending = true
        )

        val dialog = KlipyPickerDialogFragment.newInstance(config)
        dialog.listener = object : KlipyPickerListener {

            override fun onMediaSelected(item: MediaItem, searchTerm: String?) {
                vm.postAction(HomeAction.MediaSelected(item))
                searchTerm
                    ?.takeIf { it.isNotBlank() }
                    ?.let { vm.postAction(HomeAction.SearchTermUpdated(it)) }
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