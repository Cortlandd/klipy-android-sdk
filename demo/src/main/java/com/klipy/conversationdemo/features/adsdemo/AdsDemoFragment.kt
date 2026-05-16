package com.klipy.conversationdemo.features.adsdemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import com.cortlandwalker.ghettoxide.Reducer
import com.cortlandwalker.ghettoxide.ReducerFragment
import com.klipy.conversationdemo.features.mediaitempreview.model.MediaItemNavArg

class AdsDemoFragment :
    ReducerFragment<AdsDemoState, AdsDemoAction, AdsDemoEffect, AdsDemoReducer>() {

    override lateinit var reducer: AdsDemoReducer
    override val initialState: AdsDemoState = AdsDemoState()

    override fun onCreate(savedInstanceState: Bundle?) {
        reducer = AdsDemoReducer()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state = vm.state.collectAsState().value
                AdsDemoScreen(
                    state = state,
                    reducer = reducer
                )
            }
        }
    }

    override fun onEffect(effect: AdsDemoEffect) {
        when (effect) {
            AdsDemoEffect.Back -> {
                findNavController().navigateUp()
            }

            is AdsDemoEffect.OpenMediaPreview -> {
                findNavController().navigate(
                    requireContext().resources.getIdentifier(
                        "mediaItemPreviewFragment",
                        "id",
                        requireContext().packageName
                    ),
                    bundleOf("mediaItem" to MediaItemNavArg.from(effect.item))
                )
            }

            is AdsDemoEffect.ShowMessage -> {
                Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
