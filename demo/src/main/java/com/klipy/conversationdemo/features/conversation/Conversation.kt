package com.klipy.conversationdemo.features.conversation

import com.klipy.conversationdemo.features.conversation.model.ClipMessage
import com.klipy.conversationdemo.features.conversation.model.GifMessage
import com.klipy.conversationdemo.features.conversation.model.MessageUiModel
import com.klipy.conversationdemo.features.conversation.model.TextMessage
import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType

data class ConversationState(
    val conversationId: String,
    val title: String? = null,
    val messages: List<MessageUiModel> = emptyList(),
    val messageText: String = "",
    val isLoading: Boolean = false,
    val mediaTypes: List<MediaType> = listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP),
    val chosenMediaType: MediaType? = null,
    val categories: List<Category> = emptyList(),
    val chosenCategory: Category? = null,
    val mediaItems: List<MediaItem> = emptyList(),
    val searchInput: String = "",
    val lastSearchedInput: String? = null,
    val isPickerVisible: Boolean = false,
) {
    companion object {
        fun initial(conversationId: String?): ConversationState {
            val id = conversationId ?: "0"
            return ConversationState(
                conversationId = id,
                title = getConversationTitle(id),
                messages = getMockMessages(id),
                messageText = "",
                isLoading = false,
                mediaTypes = listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP),
                chosenMediaType = null,
                categories = emptyList(),
                chosenCategory = null,
                mediaItems = emptyList(),
                lastSearchedInput = null
            )
        }
    }
}

sealed interface ConversationAction {
    data object Back : ConversationAction
    data object ScreenStarted : ConversationAction
    data class ScreenMeasured(
        val containerWidthDp: Int,
        val containerHeightDp: Int,
        val screenWidthPx: Int,
        val screenHeightPx: Int
    ) : ConversationAction

    data class MessageTextChanged(val text: String) : ConversationAction
    data object SendClicked : ConversationAction

    data class MediaTypeSelected(val type: MediaType) : ConversationAction
    data class CategorySelected(val category: Category?) : ConversationAction
    data class SearchInputChanged(val query: String) : ConversationAction
    data class TrayMediaSelected(val item: MediaItem) : ConversationAction
    data class MediaItemClicked(val item: MediaItem) : ConversationAction
    data object PickerToggleClicked : ConversationAction
    data object RetryInitialLoad : ConversationAction
}

sealed interface ConversationEffect {
    data class ShowError(val message: String) : ConversationEffect
    data class OpenMediaPreview(val item: MediaItem) : ConversationEffect
    data object Back : ConversationEffect
}

// --- Simple helpers to give each conversation a title / starter messages --- //

private fun getConversationTitle(conversationId: String?): String {
    return when (conversationId) {
        "0" -> "KLIPY"
        "1" -> "John Brown"
        "2" -> "Sarah 💅🏻"
        "3" -> "Alex"
        else -> "Conversation"
    }
}

private fun getMockMessages(conversationId: String?): List<MessageUiModel> {
    return when (conversationId) {
        "0" -> mockMessagesKlipy
        "1" -> mockMessages1
        "2" -> mockMessages2
        "3" -> mockMessages3
        else -> emptyList()
    }
}

// Trimmed sample-like messages just so UI has something to render.

private val mockMessagesKlipy = listOf(
    TextMessage(
        text = "Hey! I’m using this demo app to help me with the integration to KLIPY.",
        isFromCurrentUser = true
    ),
    TextMessage(
        text = "Hi! Welcome to the KLIPY Demo App.",
        isFromCurrentUser = false
    ),
    TextMessage(
        text = "Feel free to use all the fun content",
        isFromCurrentUser = false
    ),
    GifMessage(
        url = "https://static.klipy.com/ii/d7aec6f6f171607374b2065c836f92f4/4d/7b/tOuOhBXs.gif",
        width = 498,
        height = 372,
        isFromCurrentUser = false
    ),
    ClipMessage(
        url = "https://static.klipy.com/ii/48a9760ecdd5307ed701eb96ba85d319/95/37/pGLc17Ld.mp4",
        width = 498,
        height = 372,
        isFromCurrentUser = true
    )
)

private val mockMessages1 = listOf(
    TextMessage(
        text = "Hey John",
        isFromCurrentUser = true
    ),
    GifMessage(
        url = "https://static.klipy.com/ii/da290b156d64898341638f3c299e7478/e8/ae/ejlWiBy8.gif",
        width = 480,
        height = 480,
        isFromCurrentUser = true
    ),
    TextMessage(
        text = "Hi, how’s it going?",
        isFromCurrentUser = false
    ),
    GifMessage(
        url = "https://static.klipy.com/ii/bea85337777ad0e23e63683391435543/6d/be/raThpJyc.gif",
        width = 499,
        height = 499,
        isFromCurrentUser = false
    ),
    TextMessage(
        text = "All good!",
        isFromCurrentUser = true
    )
)

private val mockMessages2 = listOf(
    TextMessage(
        text = "Please remind me about my appointment time",
        isFromCurrentUser = true
    ),
    TextMessage(
        text = "at 4",
        isFromCurrentUser = false
    ),
    GifMessage(
        url = "https://static.klipy.com/ii/d7aec6f6f171607374b2065c836f92f4/0a/3a/kiKi6leN.gif",
        width = 640,
        height = 400,
        isFromCurrentUser = false
    ),
)

private val mockMessages3 = listOf(
    TextMessage(
        text = "hey, how’s it going?",
        isFromCurrentUser = false
    ),
)