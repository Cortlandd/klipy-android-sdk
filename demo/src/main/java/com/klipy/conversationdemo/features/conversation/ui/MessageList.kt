package com.klipy.conversationdemo.features.conversation.ui

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.klipy.conversationdemo.features.conversation.model.ClipMessage
import com.klipy.conversationdemo.features.conversation.model.GifMessage
import com.klipy.conversationdemo.features.conversation.model.MessageUiModel
import com.klipy.conversationdemo.features.conversation.model.TextMessage

@Composable
fun MessagesList(
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

