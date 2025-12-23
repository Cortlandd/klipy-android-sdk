package com.klipy.klipy_ui

import com.klipy.sdk.KlipyRepository

/**
 * Simple holder for a shared [KlipyRepository] used by the UI components in `klipy-ui`.
 *
 * You should configure this once, typically in your [Application] class:
 *
 * ```kotlin
 * class MyApp : Application() {
 *   override fun onCreate() {
 *     super.onCreate()
 *
 *     val repo = KlipySdk.create(
 *       context = this,
 *       secretKey = "YOUR_KLIPY_SECRET",
 *       baseUrl = "https://api.klipy.com"
 *     )
 *
 *     KlipyUi.configure(repo)
 *   }
 * }
 * ```
 *
 * After that, classes like [com.klipy.klipy_ui.picker.KlipyPickerDialogFragment] can internally call
 * [requireRepository] to fetch media without you needing to pass the repository around.
 */
object KlipyUi {

    @Volatile
    private var repository: KlipyRepository? = null

    fun configure(repo: KlipyRepository) {
        repository = repo
    }

    fun requireRepository(): KlipyRepository =
        repository ?: error("KlipyUi.configure(repo) must be called before using Klipy picker")
}