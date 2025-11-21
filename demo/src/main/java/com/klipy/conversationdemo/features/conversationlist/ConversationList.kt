package com.klipy.conversationdemo.features.conversationlist

import com.klipy.conversationdemo.features.conversationlist.model.ConversationUiModel

data class ConversationListState(
    val isLoading: Boolean = false,
    val conversations: List<ConversationUiModel> = emptyList(),
    val errorMessage: String? = null
)

sealed interface ConversationListAction {
    data object LoadConversations : ConversationListAction
    data class ConversationClicked(val id: String) : ConversationListAction
    data object Retry : ConversationListAction
}

sealed interface ConversationListEffect {
    data class NavigateToConversation(val conversationId: String) : ConversationListEffect
    data class ShowError(val message: String) : ConversationListEffect
}
