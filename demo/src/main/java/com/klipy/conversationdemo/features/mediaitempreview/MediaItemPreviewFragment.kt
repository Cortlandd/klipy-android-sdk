package com.klipy.conversationdemo.features.mediaitempreview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cortlandwalker.ghettoxide.Reducer
import com.cortlandwalker.ghettoxide.ReducerFragment
import com.klipy.conversationdemo.features.mediaitempreview.model.MediaItemNavArg

class MediaItemPreviewFragment : ReducerFragment<MediaItemPreviewState, MediaItemPreviewAction, MediaItemPreviewEffect>() {

    private val args by navArgs<MediaItemPreviewFragmentArgs>()

    override lateinit var reducer: Reducer<MediaItemPreviewState, MediaItemPreviewAction, MediaItemPreviewEffect>

    override val initialState: MediaItemPreviewState by lazy {
        MediaItemPreviewState(args.mediaItem.toMediaItem())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        reducer = MediaItemPreviewReducer()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val s = vm.state.collectAsState().value
                MediaItemPreviewScreen(
                    state = s,
                    reducer = reducer as MediaItemPreviewReducer
                )
            }
        }
    }

    override fun onEffect(effect: MediaItemPreviewEffect) {
        when (effect) {
            MediaItemPreviewEffect.Close -> {
                findNavController().navigateUp()
            }
            is MediaItemPreviewEffect.Share -> {

            }
        }
    }

}
