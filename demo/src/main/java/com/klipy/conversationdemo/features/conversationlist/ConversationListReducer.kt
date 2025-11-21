package com.klipy.conversationdemo.features.conversationlist

import com.cortlandwalker.ghettoxide.Reducer

class ConversationListReducer : Reducer<ConversationListState, ConversationListAction, ConversationListEffect>() {

    override suspend fun process(action: ConversationListAction) {
        when(action) {
            is ConversationListAction.ConversationClicked -> {
                emit(effect = ConversationListEffect.NavigateToConversation(action.id))
            }
            ConversationListAction.LoadConversations -> {
                state { it.copy(isLoading = true) }
            }
            ConversationListAction.Retry -> {
                postAction(ConversationListAction.LoadConversations)
            }
        }
    }

    override fun onLoadAction(): ConversationListAction? {
        return ConversationListAction.LoadConversations
    }

}