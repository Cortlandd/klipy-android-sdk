# Klipy Android SDK Docs

This is the detailed integration guide for the Klipy Android SDK.

If you want the shorter setup path, screenshots, and release references, also see:
- [README.md](/Users/cortland/development/android/klipy-android-sdk/README.md)
- [CHANGELOG.md](/Users/cortland/development/android/klipy-android-sdk/CHANGELOG.md)

## Overview

The SDK is split into two layers:

- `klipy`
  The core client and repository for talking to Klipy APIs.
- `klipy-ui`
  Ready-made UI surfaces like the picker and tray.

Klipy is a little different from a plain media API because ads can be returned inline inside normal media feeds. That means a real integration should think about:

- GIFs, stickers, clips, and memes
- search, trending, and recent feeds
- inline ads returned inside those feeds
- attribution and analytics events like share/report/hide

This SDK is built around that model rather than treating ads as a separate product.

## Requirements

- `minSdk 24`
- Android app using Fragments for the ready-made picker
- Kotlin recommended

The picker content surface is Compose-based internally, but the host app does not need to be a Compose-first app.

## Installation

Add JitPack:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Add dependencies:

```kotlin
dependencies {
    implementation("com.github.Cortlandd.klipy-android-sdk:klipy:0.1.13")
    implementation("com.github.Cortlandd.klipy-android-sdk:klipy-ui:0.1.13")
}
```

If you only want the core client and plan to build your own UI, `klipy-ui` is optional.

## Create a Repository

The main entry point is `KlipySdk.create(...)`.

```kotlin
val repo = KlipySdk.create(
    context = applicationContext,
    apiKey = "YOUR_KLIPY_API_KEY",
    enableLogging = true
)
```

Parameters:

- `context`
  Any Android `Context`. The SDK converts it to `applicationContext` internally.
- `apiKey`
  Your Klipy API key.
- `enableLogging`
  Enables BASIC-level OkHttp logging.

The SDK does not expose a configurable `baseApiUrl`. It always uses Klipy’s production API host:

`https://api.klipy.com/api/v1/`

That keeps integrator setup simpler and avoids drift from the supported platform endpoint.

## Configure the Shared UI Repository

If you want the picker or tray to use one shared repository, configure `KlipyUi` once:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val repo = KlipySdk.create(
            context = this,
            apiKey = "YOUR_KLIPY_API_KEY",
            enableLogging = true
        )

        KlipyUi.configure(repo)
    }
}
```

That is the simplest setup if your app uses the SDK in more than one place.

## Core SDK Usage

The main interface is `KlipyRepository`.

Typical operations:

```kotlin
val mediaTypes = repo.getAvailableMediaTypes()
val trending = repo.getTrending(MediaType.GIF)
val recent = repo.getRecent(MediaType.GIF)
val search = repo.search(MediaType.GIF, "good morning")
val categories = repo.getCategories(MediaType.GIF)
val items = repo.getItems(
    mediaType = MediaType.GIF,
    ids = listOf("abc123"),
    slugs = emptyList()
)
```

There is also a generic content surface:

```kotlin
val result = repo.getMedia(
    mediaType = MediaType.GIF,
    filter = "trending"
)
```

Supported filter values include:

- ordinary search terms
- `trending`
- `recent`

## Example: Using the Core SDK Inside an Activity

This is a plain Activity-based example with no picker involved.

```kotlin
class TrendingActivity : AppCompatActivity() {

    private lateinit var repo: KlipyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trending)

        repo = KlipySdk.create(
            context = applicationContext,
            apiKey = BuildConfig.KLIPY_API_KEY,
            enableLogging = true
        )

        loadTrending()
    }

    private fun loadTrending() {
        lifecycleScope.launch {
            val result = repo.getTrending(
                mediaType = MediaType.GIF,
                options = MediaRequestOptions(
                    locale = "us"
                )
            )

            result
                .onSuccess { data ->
                    val mediaOnly = data.mediaItems.filterNot { it.isAD() }
                    val inlineAds = data.mediaItems.filter { it.isAD() }

                    // Render the mixed feed or split it however your screen needs.
                    renderFeed(mediaOnly, inlineAds)
                }
                .onFailure { error ->
                    showError(error.message ?: "Failed to load Klipy content")
                }
        }
    }

    private fun renderFeed(media: List<MediaItem>, ads: List<MediaItem>) {
        // Your own Activity UI code here
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
```

That example is intentionally simple, but it shows the most important point for Klipy:

- normal feeds may contain inline ads
- your screen can preserve them or filter them depending on the product surface you are building

## Media Types

```kotlin
enum class MediaType {
    GIF,
    CLIP,
    STICKER,
    MEME,
    AD
}
```

Important behavior:

- `AD` items are returned inside normal content feeds
- apps usually do not fetch `AD` as a standalone feed
- the ready-made UI keeps ads inline rather than stripping them out

Helper:

```kotlin
if (item.isAD()) {
    // Treat as inline ad content
}
```

## Request Options

Use `MediaRequestOptions` when you need more control over API calls:

```kotlin
val result = repo.search(
    mediaType = MediaType.GIF,
    query = "celebration",
    options = MediaRequestOptions(
        customerId = currentUser.id,
        locale = "us",
        contentFilter = ContentFilter.LOW,
        formatFilter = linkedSetOf(MediaFormat.GIF, MediaFormat.WEBP)
    )
)
```

Use `ShareTriggerOptions` for share tracking:

```kotlin
repo.triggerShare(
    mediaType = MediaType.GIF,
    slug = item.id,
    options = ShareTriggerOptions(
        customerId = currentUser.id,
        searchQuery = "celebration"
    )
)
```

## Data Model Notes

`MediaData` includes:

- `mediaItems`
- `itemMinWidth`
- `adMaxResizePercentage`

`mediaItems` may contain both standard media and ads.

`MediaItem` includes:

- `id`
- `title`
- `blurPreview`
- `lowQualityMetaData`
- `highQualityMetaData`
- `mediaType`

For ad items, `lowQualityMetaData` represents the inline ad asset payload used by the UI layer.

## Picker

`KlipyPickerDialogFragment` is the drop-in selection surface.

It supports:

- GIFs
- stickers
- clips
- memes
- inline ads returned by Klipy
- search
- trending / recents defaults
- optional selection confirmation for normal media
- picker theming

### Picker With Shared Configuration

```kotlin
class ChatFragment : Fragment(), KlipyPickerListener {

    private fun openKlipyPicker() {
        val config = KlipyPickerConfig(
            mediaTypes = listOf(
                MediaType.GIF,
                MediaType.STICKER,
                MediaType.CLIP,
                MediaType.MEME
            ),
            columns = 3,
            showTrending = true,
            showRecents = false,
            showSearch = true,
            showConfirmationScreen = false,
            itemSpacingDp = 1,
            initialMediaType = MediaType.GIF
        )

        KlipyPickerDialogFragment
            .newInstance(config)
            .also { it.listener = this }
            .show(childFragmentManager, "klipy_picker")
    }

    override fun onMediaSelected(item: MediaItem, searchTerm: String?) {
        // Only normal media selection is delivered here.
        // Inline ads stay inside the picker feed.
    }

    override fun onDismissed(lastContentType: MediaType?) = Unit

    override fun didSearchTerm(term: String) = Unit
}
```

### Picker Without Global Configuration

If you do not want to configure `KlipyUi` globally:

```kotlin
val dialog = KlipyPickerDialogFragment.newInstance(
    config = config,
    apiKey = "YOUR_KLIPY_API_KEY",
    enableLogging = true
).apply {
    listener = this@ChatFragment
}

dialog.show(childFragmentManager, "klipy_picker")
```

### Picker Configuration

`KlipyPickerConfig` currently supports:

- `mediaTypes`
- `columns`
- `showTrending`
- `showRecents`
- `showSearch`
- `showConfirmationScreen`
- `itemSpacingDp`
- `initialMediaType`
- `themeMode`
- `colors`

Example:

```kotlin
val config = KlipyPickerConfig(
    mediaTypes = listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP),
    columns = 3,
    showTrending = true,
    showRecents = true,
    showSearch = true,
    showConfirmationScreen = true,
    itemSpacingDp = 2,
    initialMediaType = MediaType.GIF,
    themeMode = KlipyPickerThemeMode.DARK
)
```

### Picker Behavior

- search uses submit/debounce behavior
- opening a fresh picker session resets pagination state
- confirmation applies only to normal media
- ads stay inline and are not routed through selection confirmation
- ad ordering follows the original API feed order

## Picker Theming

Theme modes:

- `AUTOMATIC`
- `LIGHT`
- `DARK`

Optional color overrides:

```kotlin
val colors = KlipyPickerColors(
    backgroundColor = 0xFFFFFBEB.toInt(),
    surfaceColor = 0xFFFFFFFF.toInt(),
    primaryColor = 0xFFD97706.toInt(),
    onSurfaceColor = 0xFF1F2937.toInt(),
    secondaryTextColor = 0xFF6B7280.toInt(),
    outlineColor = 0xFFF3D28A.toInt(),
    searchFieldColor = 0xFFFFF4CC.toInt(),
    buttonColor = 0xFF111827.toInt(),
    onButtonColor = 0xFFFFFFFF.toInt()
)
```

Then:

```kotlin
val config = KlipyPickerConfig(
    themeMode = KlipyPickerThemeMode.LIGHT,
    colors = colors
)
```

## Tray

`KlipyTrayView` is the more embeddable keyboard/panel-style UI surface.

The tray config supports:

- `mediaTypes`
- `initialMediaType`
- `columns`
- `showTrending`
- `showRecents`
- `showCategories`
- `showSearch`

Example:

```kotlin
val config = KlipyTrayConfig(
    mediaTypes = listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP),
    initialMediaType = MediaType.GIF,
    columns = 3,
    showTrending = true,
    showRecents = false,
    showCategories = true,
    showSearch = true
)
```

The tray is a good fit when you want:

- a keyboard-like media surface
- a more embedded integration than the picker
- tighter control over how the panel fits into an existing screen

## Ads

Klipy’s platform can return ads inside regular content feeds. That is a core part of the integration model, not an afterthought.

### What the SDK does automatically

- adds `ad-iframe=1` to ad-enabled content requests
- includes the ad request metadata needed for inline inventory
- parses `type: "ad"` items into `MediaType.AD`
- keeps ads inside the returned content list
- renders picker ads inline through `ads-android` using `GIFWebView` and `KlipyContent`

### What integrators should expect

- ads may appear in `trending`, `recent`, or search results
- ads are not guaranteed in every page or every query
- picker selection callbacks are for normal media items, not inline ad taps
- if you build your own UI on top of `KlipyRepository`, you decide whether to preserve or filter inline ads

### Why this matters

If you compare Klipy to a simpler GIF SDK, the most important difference is that “content feed” really means mixed content. The SDK is designed around that assumption so the default UI and the core data layer stay consistent.

## Rendering Media Yourself

The picker returns `MediaItem`, and the core SDK also gives you `MediaItem` directly. You can render those however you want.

GIF/sticker example:

```kotlin
Glide.with(imageView)
    .asGif()
    .load(item.highQualityMetaData?.url ?: item.lowQualityMetaData?.url)
    .into(imageView)
```

Compose preview component:

```kotlin
MediaItemPreview(item)
```

## Public Surface

Core:

- `KlipySdk`
- `KlipyRepository`
- `MediaType`
- `MediaItem`
- `MediaData`
- `Category`
- `MediaRequestOptions`
- `ShareTriggerOptions`
- `ContentFilter`
- `MediaFormat`

UI:

- `KlipyUi`
- `KlipyPickerDialogFragment`
- `KlipyPickerConfig`
- `KlipyPickerListener`
- `KlipyPickerThemeMode`
- `KlipyPickerColors`
- `KlipyTrayConfig`
- `KlipyTrayState`
- `KlipyTrayAction`
- `KlipyTrayEffect`

Helpers:

- `MediaType.singularName()`
- `MediaItem.isAD()`

## Notes

- The picker content surface is Compose-based internally, but the host app can still be Fragment-based and non-Compose overall.
- The tray remains a strong option for embedded and keyboard-like experiences.
- If you are migrating from Tenor, use the current Klipy request options and attribution guidance from the platform docs.

If you find a bug or parity gap, open an issue and include whether the problem is in:

- core repository/API behavior
- picker behavior
- tray behavior
- inline ad rendering
