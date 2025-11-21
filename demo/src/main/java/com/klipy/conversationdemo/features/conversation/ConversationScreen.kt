@file:OptIn(ExperimentalMaterial3Api::class)

package com.klipy.conversationdemo.features.conversation

import android.annotation.SuppressLint
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.singularName
import com.klipy.conversationdemo.features.conversation.model.MessageUiModel
import com.klipy.conversationdemo.features.conversation.model.TextMessage
import com.klipy.conversationdemo.features.conversation.model.GifMessage
import com.klipy.conversationdemo.features.conversation.model.ClipMessage
import com.klipy.conversationdemo.features.conversation.model.toMediaItemOrNull
import com.klipy.conversationdemo.ui.theme.Purple80
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ConversationScreen(
    state: ConversationState,
    reducer: ConversationReducer
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val userBubble = Purple80
    val klipyBubble = Color(0xFF9C27FF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = state.title ?: "KLIPY")
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
                        state = state,
                        onMediaTypeSelected = {
                            reducer.postAction(ConversationAction.MediaTypeSelected(it))
                        },
                        onCategorySelected = {
                            reducer.postAction(ConversationAction.CategorySelected(it))
                        },
                        onSearchChanged = {
                            reducer.postAction(ConversationAction.SearchInputChanged(it))
                        },
                        onMediaClicked = {
                            reducer.postAction(ConversationAction.TrayMediaSelected(it))
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onSizeChanged { size ->
                    val widthPx = size.width
                    val heightPx = size.height
                    val widthDp = with(density) { widthPx.toDp().value.toInt() }
                    val heightDp = with(density) { heightPx.toDp().value.toInt() }

                    scope.launch {
                        reducer.postAction(
                            ConversationAction.ScreenMeasured(
                                containerWidthDp = widthDp,
                                containerHeightDp = heightDp,
                                screenWidthPx = widthPx,
                                screenHeightPx = heightPx
                            )
                        )
                    }
                }
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

            if (state.isLoading) {
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

/* -------------------- Messages -------------------- */

@Composable
private fun MessagesList(
    modifier: Modifier = Modifier,
    messages: List<MessageUiModel>,
    userBubble: Color,
    klipyBubble: Color,
    onMediaClicked: (MessageUiModel) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            val isUser = msg.isFromCurrentUser

            when (msg) {
                is TextMessage -> {
                    TextMessageBubble(
                        text = msg.text,
                        isFromUser = isUser,
                        userBubble = userBubble,
                        klipyBubble = klipyBubble
                    )
                }

                is GifMessage -> {
                    Box(
                        modifier = Modifier.clickable { onMediaClicked(msg) }
                    ) {
                        MediaMessageBubble(
                            url = msg.url,
                            isClip = false,
                            isFromUser = isUser
                        )
                    }
                }

                is ClipMessage -> {
                    Box(
                        modifier = Modifier.clickable { onMediaClicked(msg) }
                    ) {
                        MediaMessageBubble(
                            url = msg.url,
                            isClip = true,
                            isFromUser = isUser
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextMessageBubble(
    text: String,
    isFromUser: Boolean,
    userBubble: Color,
    klipyBubble: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomEnd = if (isFromUser) 0.dp else 18.dp,
                        bottomStart = if (isFromUser) 18.dp else 0.dp
                    )
                )
                .background(if (isFromUser) userBubble else klipyBubble)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                color = if (isFromUser) Color.Black else Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MediaMessageBubble(
    url: String,
    isClip: Boolean,
    isFromUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { imageView ->
                    // GIF or clip thumbnail – both use Glide
                    Glide.with(imageView)
                        .asGif()
                        .load(url)
                        .into(imageView)
                }
            )

            if (isClip) {
                // Play overlay for clips
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9C27FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}

/* -------------------- Klipy tray -------------------- */

@Composable
private fun KlipyTray(
    state: ConversationState,
    onMediaTypeSelected: (MediaType) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onMediaClicked: (MediaItem) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    // Local text that mirrors the last search, but we only "commit" on IME action.
    var searchText by remember(state.lastSearchedInput) {
        mutableStateOf(state.lastSearchedInput.orEmpty())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF222228))
    ) {
        /* ------- Search row (top of tray) ------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search Klipy…") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchText = ""
                                // Treat empty as "trending" / reset on your reducer side.
                                onSearchChanged("")
                                keyboardController?.hide()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF2C2C33),
                    unfocusedContainerColor = Color(0xFF2C2C33),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val term = searchText.trim()
                        // Only now do we tell the reducer to actually search.
                        onSearchChanged(term)
                        keyboardController?.hide()
                    }
                )
            )
        }

        /* ------- Results grid (middle of tray) ------- */
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 260.dp)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.mediaItems, key = { it.id }) { item ->
                MediaItemThumbnail(
                    item = item,
                    onClick = { onMediaClicked(item) }
                )
            }
        }

        /* ------- Categories row (above bottom selector) ------- */
        if (state.categories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = state.chosenCategory == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("All") }
                    )
                }

                items(state.categories, key = { it.title }) { cat ->
                    FilterChip(
                        selected = state.chosenCategory == cat,
                        onClick = { onCategorySelected(cat) },
                        label = { Text(cat.title) }
                    )
                }
            }
        }

        /* ------- Media type selector (bottom of tray) ------- */
        if (state.mediaTypes.isNotEmpty()) {
            val selectedType = state.chosenMediaType ?: state.mediaTypes.first()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                state.mediaTypes.forEach { type ->
                    val selected = type == selectedType
                    TextButton(
                        onClick = { onMediaTypeSelected(type) }
                    ) {
                        Text(
                            text = type.singularName().uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaItemThumbnail(
    item: MediaItem,
    onClick: () -> Unit
) {
    AndroidView(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            val hi = item.highQualityMetaData
            val lo = item.lowQualityMetaData
            val url = hi?.url ?: lo?.url

            if (url != null) {
                Glide.with(imageView)
                    .asGif()
                    .load(url)
                    .into(imageView)
            } else {
                imageView.setImageDrawable(null)
            }
        }
    )
}

/* -------------------- Composer -------------------- */

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
