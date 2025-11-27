package com.klipy.klipy_ui.tray

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.klipy.sdk.model.Category
import com.klipy.sdk.model.MediaItem
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.MetaData

/**
 * Internal pure UI for the tray (state + actions only).
 * Exposed via [KlipyTray] in KlipyTrayCompose.kt for SDK users.
 */
@Composable
internal fun KlipyTrayContent(
    state: KlipyTrayState,
    config: KlipyTrayConfig,
    modifier: Modifier = Modifier,
    onAction: (KlipyTrayAction) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 4.dp)
    ) {
        MediaTypeRow(
            mediaTypes = state.mediaTypes.ifEmpty { config.mediaTypes },
            selected = state.chosenMediaType ?: config.initialMediaType,
            onSelected = { type ->
                onAction(KlipyTrayAction.MediaTypeSelected(type))
            }
        )

        if (config.showSearch) {
            SearchRow(
                text = state.searchInput,
                onTextChange = { text ->
                    onAction(KlipyTrayAction.SearchInputChanged(text))
                }
            )
        }

        if (config.showCategories && state.categories.isNotEmpty()) {
            CategoryRow(
                categories = state.categories,
                selected = state.chosenCategory,
                onSelected = { category ->
                    onAction(KlipyTrayAction.CategorySelected(category))
                }
            )
        }

        MediaGrid(
            items = state.mediaItems,
            columns = config.columns,
            isLoading = state.isLoading,
            onItemClick = { item ->
                onAction(KlipyTrayAction.MediaItemClicked(item))
            }
        )
    }
}

private val DarkerPurple = Color(0xFF9C27FF)

@Composable
private fun MediaTypeRow(
    mediaTypes: List<MediaType>,
    selected: MediaType,
    onSelected: (MediaType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        mediaTypes.forEach { type ->
            val isSelected = type == selected

            FilterChip(
                modifier = Modifier.weight(1f),
                selected = isSelected,
                onClick = { onSelected(type) },
                label = {
                    Text(
                        text = type.name.uppercase(),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DarkerPurple,
                    selectedLabelColor = Color.White,
                    containerColor = Color.Transparent,
                    labelColor = Color.Black
                )
            )
        }
    }
}

@Composable
private fun SearchRow(
    text: String,
    onTextChange: (String) -> Unit
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        singleLine = true,
        placeholder = { Text("Search Klipy") },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { /* reducer reacts to text change */ }
        )
    )

    Spacer(Modifier.height(4.dp))
}

@Composable
private fun CategoryRow(
    categories: List<Category>,
    selected: Category?,
    onSelected: (Category?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            AssistChip(
                onClick = { onSelected(null) },
                label = { Text("All") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected == null)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        items(categories) { cat ->
            AssistChip(
                onClick = { onSelected(cat) },
                label = { Text(cat.title) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (cat == selected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun MediaGrid(
    items: List<MediaItem>,
    columns: Int,
    isLoading: Boolean,
    onItemClick: (MediaItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp, max = 260.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns.coerceAtLeast(1)),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items) { item ->
                KlipyTrayMediaCell(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                strokeWidth = 2.dp
            )
        }
    }
}

/**
 * Single media cell using Glide under the hood via AndroidView.
 *
 * For CLIP we prefer the GIF/WebP preview (low/high), and your send logic
 * can still use mp4 as you already wired in ConversationReducer.
 */
@Composable
private fun KlipyTrayMediaCell(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = when (item.mediaType) {
        MediaType.CLIP -> item.lowQualityMetaData ?: item.highQualityMetaData
        else           -> item.highQualityMetaData ?: item.lowQualityMetaData
    }
    val url = meta?.url

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            },
            update = { imageView ->
                if (!url.isNullOrBlank()) {
                    Glide.with(imageView.context)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(imageView)
                } else {
                    imageView.setImageDrawable(null)
                }
            }
        )

        if (item.mediaType == MediaType.CLIP) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(name = "Klipy Tray – GIF Selected")
@Composable
private fun KlipyTrayContentPreview() {
    fun sampleTrendingMediaItems(): List<MediaItem> = listOf(
        MediaItem(
            id = "6333290484303727",
            title = "Cute Audi TT",
            placeHolder = null,
            lowQualityMetaData = MetaData(
                // xs.gif from your sample response
                url = "https://thumbs.dreamstime.com/b/sample-jpeg-fluffy-white-pomeranian-puppy-sits-looks-camera-colorful-balls-front-364720569.jpg",
                width = 156,
                height = 90
            ),
            highQualityMetaData = MetaData(
                // md.gif from your sample response
                url = "https://thumbs.dreamstime.com/b/sample-jpeg-fluffy-white-pomeranian-puppy-sits-looks-camera-colorful-balls-front-364720569.jpg",
                width = 341,
                height = 197
            ),
            mediaType = MediaType.GIF
        ),
        // Duplicate a couple of items for a fuller preview grid.
        MediaItem(
            id = "6333290484303727-2",
            title = "Cute Audi TT (alt)",
            placeHolder = null,
            lowQualityMetaData = MetaData(
                // sm.gif variant
                url = "https://thumbs.dreamstime.com/b/sample-jpeg-fluffy-white-pomeranian-puppy-sits-looks-camera-colorful-balls-front-364720569.jpg",
                width = 220,
                height = 128
            ),
            highQualityMetaData = MetaData(
                // hd.gif variant
                url = "https://thumbs.dreamstime.com/b/sample-jpeg-fluffy-white-pomeranian-puppy-sits-looks-camera-colorful-balls-front-364720569.jpg",
                width = 341,
                height = 197
            ),
            mediaType = MediaType.GIF
        ),
        MediaItem(
            id = "6333290484303727-3",
            title = "Cute Audi TT (small)",
            placeHolder = null,
            lowQualityMetaData = MetaData(
                // another small variant (xs.webp as GIF still works for preview)
                url = "https://thumbs.dreamstime.com/b/sample-jpeg-fluffy-white-pomeranian-puppy-sits-looks-camera-colorful-balls-front-364720569.jpg",
                width = 156,
                height = 90
            ),
            highQualityMetaData = MetaData(
                url = "https://thumbs.dreamstime.com/b/sample-jpeg-fluffy-white-pomeranian-puppy-sits-looks-camera-colorful-balls-front-364720569.jpg",
                width = 341,
                height = 197
            ),
            mediaType = MediaType.GIF
        )
    )
    val sampleCategories = listOf(
        Category(
            title = "hello",
            previewUrl = "https://static.klipy.com/ii/935d7ab9d8c6202580a668421940ec81/14/af/8GCrVAB7.gif",
            query = "hello",
        ),
        Category(
            title = "lol",
            previewUrl = "https://static.klipy.com/ii/f87f46a2c5aeaeed4c68910815f73eaf/4b/5f/ZtUq5OiZ.gif",
            query = "lol"
        ),
        Category(
            title = "love",
            previewUrl = "https://static.klipy.com/ii/4e7bea9f7a3371424e6c16ebc93252fe/0d/dc/ea6uXb0AN38h.gif",
            query = "love"
        ),
    )

    val previewState = KlipyTrayState(
        isLoading = false,
        mediaTypes = listOf(
            MediaType.GIF,
            MediaType.STICKER,
            MediaType.CLIP,
            MediaType.MEME
        ),
        chosenMediaType = MediaType.GIF,
        categories = sampleCategories,
        chosenCategory = sampleCategories.first(),
        mediaItems = sampleTrendingMediaItems(),
        searchInput = "car",
        lastSearchedInput = "cart"
    )

    KlipyTrayContent(
        state = previewState,
        config = KlipyTrayConfig(showCategories = true),
        onAction = { /* no-op for preview */ }
    )
}
