package com.klipy.klipy_ui.picker

import android.os.Build.VERSION.SDK_INT
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

@Composable
internal fun PickerGifImage(
    modifier: Modifier = Modifier,
    key: Any,
    url: String?,
    contentScale: ContentScale,
    placeholder: Painter? = null,
    error: Painter? = null
) {
    val context = LocalContext.current
    val imageRequest = remember(key) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        modifier = modifier,
        model = imageRequest,
        contentDescription = "Media",
        imageLoader = remember {
            ImageLoader.Builder(context)
                .components {
                    if (SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        },
        contentScale = contentScale,
        placeholder = placeholder,
        error = error
    )
}
