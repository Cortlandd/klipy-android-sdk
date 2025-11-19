![](logo/android-klipy.png)

# Klipy Android SDK

[![License](https://img.shields.io/cocoapods/l/SwiftChess.svg?style=flat)](https://github.com/Cortlandd/klipy-android-sdk/blob/master/LICENSE.md)

An Android SDK wrapping the [Klipy](https://klipy.com) GIF / Clips / Stickers API.

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
    implementation("com.github.Cortlandd:klipy-android-sdk:klipy-ui:1.0.0") // If you want to use the picker fragment
}
```

## Basic Usage Sample Screenshots
| Default State        | Search Screen          | Gif Results            | Sticker Results        | Clip Results           | Displaying selection   |
|----------------------|------------------------|------------------------|------------------------|------------------------|------------------------|
| ![](samples/img.png) | ![](samples/img_1.png) | ![](samples/img_2.png) | ![](samples/img_3.png) | ![](samples/img_4.png) | ![](samples/img_5.png) |

| Sample Video                              |
|-------------------------------------------|
| ![SAMPLE VIDEO](samples/sample_video.mp4) |

## Basic Usage

Application
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val repo = KlipySdk.create(
            context = this,
            secretKey = "",
            enableLogging = true
        )

        KlipyUi.configure(repo)
    }
}
```

In a ViewModel, you can inject / grab the repository:
```kotlin
class GifViewModel(
    private val repo: KlipyRepository
) : ViewModel() {

    val state = MutableStateFlow(GifState())

    fun loadCategories() {
        viewModelScope.launch {
            repo.getCategories(MediaType.GIF)
                .onSuccess { categories ->
                    state.update { it.copy(categories = categories) }
                }
                .onFailure { error ->
                    // handle error
                }
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            repo.getMedia(MediaType.GIF, filter = "trending")
                .onSuccess { mediaData ->
                    state.update {
                        it.copy(
                            items = mediaData.mediaItems,
                            adMaxResizePct = mediaData.adMaxResizePercentage
                        )
                    }
                }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            // Each subsequent call with same query loads next page.
            repo.getMedia(MediaType.GIF, filter = query)
                .onSuccess { mediaData ->
                    state.update { old ->
                        old.copy(items = old.items + mediaData.mediaItems)
                    }
                }
        }
    }
}
```

To report or hide items:

```kotlin
viewModelScope.launch {
    repo.report(MediaType.GIF, slug = "some-slug", reason = "Inappropriate")
    repo.hideFromRecent(MediaType.GIF, slug = "some-slug")
}
```

To trigger analytics events:
```kotlin
viewModelScope.launch {
    repo.triggerShare(MediaType.CLIP, slug = "clip-slug")
    repo.triggerView(MediaType.CLIP, slug = "clip-slug")
}
```
