package com.klipy.conversationdemo.features.conversation

import com.cortlandwalker.ghettoxide.Reducer
import com.klipy.conversationdemo.features.conversation.ConversationEffect.*
import com.klipy.conversationdemo.features.conversation.model.ClipMessage
import com.klipy.conversationdemo.features.conversation.model.GifMessage
import com.klipy.conversationdemo.features.conversation.model.MessageUiModel
import com.klipy.conversationdemo.features.conversation.model.TextMessage
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType

class ConversationReducer(
    private val conversationId: String,
    private val initialTitle: String = "Demo Conversation"
) : Reducer<ConversationState, ConversationAction, ConversationEffect>() {

    private var didStart = false

    override fun onLoadAction(): ConversationAction = ConversationAction.ScreenStarted

    override suspend fun process(action: ConversationAction) {
        when (action) {
            ConversationAction.Back -> emit(Back)

            ConversationAction.ScreenStarted -> {
                didStart = true
            }

            is ConversationAction.PickerToggleClicked -> {
                state { it.copy(isPickerVisible = !it.isPickerVisible) }
            }

            is ConversationAction.MessageTextChanged -> {
                state { it.copy(messageText = action.text) }
            }

            ConversationAction.SendClicked -> {
                onSendClicked()
            }

            is ConversationAction.TrayMediaSelected -> {
                onTrayMediaSelected(action.item)
            }

            is ConversationAction.MediaItemClicked -> {
                emit(ConversationEffect.OpenMediaPreview(action.item))
            }
        }
    }

    private fun loadInitial() {
        // If you want a skeleton / initial messages, do it here.
        state {
            it.copy(
                isLoadingInitial = false,
                title = initialTitle
            )
        }
    }

    private fun onSendClicked() {
        state { current ->
            val trimmed = current.messageText.trim()
            if (trimmed.isBlank()) return@state current

            val newMessage: MessageUiModel = TextMessage(
                text = trimmed,
                isFromCurrentUser = true
            )

            current.copy(
                messages = current.messages + newMessage,
                messageText = ""
            )
        }
    }

    private fun onTrayMediaSelected(item: MediaItem) {
        val metaHigh = item.highQualityMetaData
        val metaLow = item.lowQualityMetaData
        val width = metaHigh?.width ?: metaLow?.width ?: 0
        val height = metaHigh?.height ?: metaLow?.height ?: 0

        state { current ->
            val nextId = current.messages.size + 1

            val urlForMessage =
                metaHigh?.url ?: metaLow?.url.orEmpty()

            val msg: MessageUiModel = when (item.mediaType) {
                MediaType.GIF -> GifMessage(
                    id = nextId.toString(),
                    url = urlForMessage,
                    isFromCurrentUser = true,
                    width = width,
                    height = height
                )

                MediaType.CLIP -> ClipMessage(
                    id = nextId.toString(),
                    url = urlForMessage,
                    isFromCurrentUser = true,
                    width = width,
                    height = height
                )

                // For STICKER / MEME etc we can treat them like GIF images in the chat bubble.
                else -> GifMessage(
                    id = nextId.toString(),
                    url = urlForMessage,
                    isFromCurrentUser = true,
                    width = width,
                    height = height
                )
            }

            current.copy(
                messages = current.messages + msg,
                isPickerVisible = false // hide tray after selection, if you want
            )
        }
    }
}
