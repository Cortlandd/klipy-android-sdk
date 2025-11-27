@file:OptIn(ExperimentalMaterial3Api::class)

package com.klipy.conversationdemo.features.conversation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.klipy.conversationdemo.features.conversation.model.toMediaItemOrNull
import com.klipy.conversationdemo.features.conversation.ui.MessagesList
import com.klipy.conversationdemo.ui.theme.Purple80
import com.klipy.klipy_ui.tray.KlipyTray
import com.klipy.klipy_ui.tray.KlipyTrayConfig

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ConversationScreen(
    state: ConversationState,
    reducer: ConversationReducer
) {
    val userBubble = Purple80
    val klipyBubble = Color(0xFF9C27FF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = state.title)
                },
                navigationIcon = {
                    IconButton(onClick = { reducer.postAction(ConversationAction.Back) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = klipyBubble
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                MessageComposer(
                    messageText = state.messageText,
                    onTextChanged = {
                        reducer.postAction(ConversationAction.MessageTextChanged(it))
                    },
                    onSendClicked = {
                        reducer.postAction(ConversationAction.SendClicked)
                    },
                    onPickerClicked = {
                        reducer.postAction(ConversationAction.PickerToggleClicked)
                    }
                )

                if (state.isPickerVisible) {
                    KlipyTray(
                        config = KlipyTrayConfig(showCategories = true),
                        onMediaSelected = { item ->
                            reducer.postAction(ConversationAction.TrayMediaSelected(item))
                        },
                        onError = {
                            // optionally add a snackbar / effect plumbing here
                        }
                    )
                }
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                MessagesList(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    messages = state.messages,
                    userBubble = userBubble,
                    klipyBubble = klipyBubble,
                    onMediaClicked = { messageId ->
                        messageId.toMediaItemOrNull()?.let { mediaItem ->
                            reducer.postAction(ConversationAction.MediaItemClicked(mediaItem))
                        }
                    }
                )
            }

            if (state.isLoadingInitial) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun MessageComposer(
    messageText: String,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onPickerClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            value = messageText,
            onValueChange = onTextChanged,
            placeholder = { Text("Enter message") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = Color(0xFF9C27FF)
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = { onSendClicked() }
            )
        )

        IconButton(onClick = onPickerClicked) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Toggle Klipy picker",
                tint = Color(0xFF9C27FF)
            )
        }

        FilledIconButton(
            onClick = onSendClicked,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFF9C27FF),
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = Color.White
            )
        }
    }
}
