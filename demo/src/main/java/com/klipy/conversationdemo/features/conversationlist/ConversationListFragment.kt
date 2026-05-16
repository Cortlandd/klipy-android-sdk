package com.klipy.conversationdemo.features.conversationlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import com.cortlandwalker.ghettoxide.Reducer
import com.cortlandwalker.ghettoxide.ReducerFragment

class ConversationListFragment :
    ReducerFragment<ConversationListState, ConversationListAction, ConversationListEffect, ConversationListReducer>() {

    override var reducer: ConversationListReducer = ConversationListReducer()
    override val initialState: ConversationListState = ConversationListState()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val s = vm.state.collectAsState().value
                ConversationListScreen(s, reducer)
            }
        }
    }

    override fun onEffect(effect: ConversationListEffect) {
        when(effect) {
            is ConversationListEffect.NavigateToConversation -> {
                if (effect.conversationId == "99") {
                    findNavController()
                        .navigate(
                            requireContext().resources.getIdentifier(
                                "ads_demo_fragment",
                                "id",
                                requireContext().packageName
                            ),
                            bundleOf()
                        )
                } else {
                    findNavController()
                        .navigate(ConversationListFragmentDirections.actionConversationListFragmentToConversationFragment((effect.conversationId)))
                }
            }
            is ConversationListEffect.ShowError -> {

            }
        }
    }

}
