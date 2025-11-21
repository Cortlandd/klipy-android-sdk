package com.klipy.conversationdemo.features.conversation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cortlandwalker.ghettoxide.Reducer
import com.cortlandwalker.ghettoxide.ReducerFragment
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klipy.conversationdemo.features.mediaitempreview.model.MediaItemNavArg

class ConversationFragment :
    ReducerFragment<ConversationState, ConversationAction, ConversationEffect>() {

    private val args by navArgs<ConversationFragmentArgs>()

    override lateinit var reducer: Reducer<ConversationState, ConversationAction, ConversationEffect>

    override val initialState: ConversationState by lazy {
        ConversationState.initial(args.conversationId ?: "0")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1) Build the reducer using args
        reducer = ConversationReducer(conversationId = args.conversationId ?: "")

        // 2) Then let the base class bind it into the StoreViewModel
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state = vm.state.collectAsStateWithLifecycle().value
                ConversationScreen(
                    state = state,
                    reducer = reducer as ConversationReducer
                )

            }
        }
    }

    override fun onEffect(effect: ConversationEffect) {
        when (effect) {
            is ConversationEffect.ShowError -> {
                Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
            }
            ConversationEffect.Back -> {
                findNavController().navigateUp()
            }
            is ConversationEffect.OpenMediaPreview -> {
                findNavController().navigate(ConversationFragmentDirections.actionConversationFragmentToMediaItemPreviewFragment((MediaItemNavArg.from(effect.item))))
            }
        }
    }
}