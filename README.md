# TECHNICALLY ITS READY FOR USE AS INDICATED BY THE SAMPLE BUT NOT PUBLISHED YET FOR USE

![](logo/android-klipy.png)

# Klipy Android SDK

[![License](https://img.shields.io/cocoapods/l/SwiftChess.svg?style=flat)](https://github.com/Cortlandd/klipy-android-sdk/blob/master/LICENSE.md)

An Android SDK wrapping the [Klipy](https://klipy.com) GIF / Clips / Stickers API.

---

## Modern Android Stack

The SDK and sample app are built using current Android patterns and libraries:

- **Modular SDK**
    - `klipy`: core networking + data models (Kotlin, coroutines, Flow).
    - `klipy-ui`: optional UI module providing a ready-made picker dialog (BottomSheetDialogFragment, RecyclerView, Glide).

- **Sample App Features**
    - **Single-Activity / Nav Graph** using **Jetpack Navigation** for screen flow.
    - **Fragment-based UI** (e.g., `HomeFragment`) hosting:
      - A **Compose** `HomeScreen` via `ComposeView`.
      - The **Klipy picker** as a `BottomSheetDialogFragment`.
    - **Jetpack Compose** for the home screen and selections:
      - Uses `@Composable` UI for the demo screen.
      - Integrates SDK results back into Compose via `AndroidView`.
    - **Coroutines + Flow**:
      - `viewModelScope`, `suspend` functions, and `Result` wrapping for network calls.
      - `StateFlow` to drive UI state from ViewModels.
    - **Glide 5** for image & GIF rendering and thumbnailing (GIFs, stickers, mp4 clips).
    - **Paging-like UX** in the picker:
      - Infinite scroll on RecyclerView to load additional pages.
    - **Material Components**:
      - Uses Material 3 in the Compose demo screen.
      - Uses Material bottom sheet for the picker UI.
    - **Ghettoxide**:
      - Helper files based on Redux. Meant to make android development seamless.

You can use the SDK in:
- Pure XML / Fragment apps.
- Compose-first apps (by hosting Fragment or Dialog via `AndroidView` or regular fragment transactions).

---

## Installation

1. Add the JitPack repository to your project:
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
   repositories {
       google()
       mavenCentral()
       maven("https://jitpack.io")
   }
}
```

2. Add the dependency with latest version:
```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.Cortlandd:klipy-android-sdk:klipy:1.0.0")
    // If you want to use the picker fragment, the above isn't necessary to implement as ui uses it arleady
    implementation("com.github.Cortlandd:klipy-android-sdk:klipy-ui:1.0.0")
}
```

## Basic Usage Sample Screenshots
| Default State        | Search Screen          | Gif Results            | Sticker Results        | Clip Results           | Displaying selection   |
|----------------------|------------------------|------------------------|------------------------|------------------------|------------------------|
| ![](samples/img.png) | ![](samples/img_1.png) | ![](samples/img_2.png) | ![](samples/img_3.png) | ![](samples/img_4.png) | ![](samples/img_5.png) |

| Sample Video                              |
|-------------------------------------------|
| ![SAMPLE VIDEO](samples/sample_video.mp4) |

## Quick Start

1. Configure in your Application
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val repo = KlipySdk.create(
            context = this,
            secretKey = "KLIPY_API_KEY",
            enableLogging = true
        )

        KlipyUi.configure(repo)
    }
}
```

2. Show the picker in a Fragment
```kotlin
class ChatFragment : Fragment(R.layout.fragment_chat), KlipyPickerListener {

    private fun openKlipyPicker() {
        val config = KlipyPickerConfig(
            mediaTypes = listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP),
            columns = 3,
            showRecents = true,
            showTrending = true,
            initialMediaType = MediaType.GIF
        )

        val dialog = KlipyPickerDialogFragment.newInstance(config).apply {
            listener = this@ChatFragment
        }

        dialog.show(childFragmentManager, "klipy_picker")
    }

    override fun onMediaSelected(item: MediaItem, searchTerm: String?) {
        // Send the selected item in your chat:
        // - item.mediaType (GIF/STICKER/CLIP/AD)
        // - item.lowQualityMetaData / highQualityMetaData
        // - filter out ads with item.isAD()
    }

    override fun onDismissed(lastContentType: MediaType?) {
        // Optional: do something when the sheet is dismissed
    }

    override fun didSearchTerm(term: String) {
        // Optional: observe search terms
    }
}
```

3. Rendering media
You decide how to render the MediaItem:
```kotlin
Glide.with(imageView)
    .asGif()
    .load(item.highQualityMetaData?.url ?: item.lowQualityMetaData?.url)
    .into(imageView)
```

# Using the UI Picker (klipy-ui)
klipy-ui ships a ready-made picker similar in spirit to Giphy’s dialog:
- Implemented as a BottomSheetDialogFragment.
- Supports GIFs, stickers, and clips (mp4).
- Search-on-submit (no auto-search on open).
- Infinite scroll / paging via RecyclerView.
- Not tied to Compose – works in any Fragment-based app.

Typical usage from a Fragment:
```kotlin
private fun openKlipyPicker() {
    val config = KlipyPickerConfig(
        columns = 3,
        showRecents = true,
        showTrending = true
    )

    val dialog = KlipyPickerDialogFragment.newInstance(config)
    dialog.listener = object : KlipyPickerListener {

        override fun onMediaSelected(item: MediaItem, searchTerm: String?) {
            // Handle selection (GIF, sticker, or clip)
        }

        override fun onDismissed(lastContentType: MediaType?) {
            // Optional: handle dismiss
        }

        override fun didSearchTerm(term: String) {
            // Optional: observe terms being searched
        }
    }

    dialog.show(childFragmentManager, "klipy_picker")
}
```

You can then render the selected MediaItem however you like (e.g., Glide for GIFs/stickers, VideoView/ExoPlayer for clips, or Compose wrappers in your own app).

---

# API Surface (1.0)
### Public core types:
- com.klipy.sdk.KlipySdk
- com.klipy.sdk.KlipyRepository
- com.klipy.sdk.model.MediaType
- com.klipy.sdk.model.MediaItem
- com.klipy.sdk.model.Category
- com.klipy.sdk.model.MediaData
- fun MediaType.singularName()
- fun MediaItem.isAD()
### Public UI types:
- com.klipy.klipy_ui.KlipyUi
- com.klipy.klipy_ui.KlipyPickerConfig
- com.klipy.klipy_ui.KlipyPickerDialogFragment
- com.klipy.klipy_ui.KlipyPickerListener
Everything under com.klipy.sdk.data and KlipyMediaAdapter is internal implementation detail.

---

You find an issue, open an issue

# FAQ
Q: Do I have to use the UI module?
A: No. You can use `KlipySdk.create(...)` + `KlipyRepository` directly and build a custom UI on top of `MediaItem`.

Q: Gonna do ios also?
A: Probably.