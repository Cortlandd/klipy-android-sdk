package com.klipy.klipy_ui

import com.klipy.sdk.KlipyRepository

/**
 * Simple holder for a shared [KlipyRepository] used by UI components in `klipy-ui`.
 *
 * Supported usage patterns:
 *
 * 1) Configure once (recommended):
 *
 * ```kotlin
 * val repo = KlipySdk.create(
 *   context = this,
 *   secretKey = "YOUR_KEY",
 *   enableLogging = true
 * )
 * KlipyUi.configure(repo)
 * ```
 *
 * 2) Do NOT configure globally. Instead pass the API key directly when opening
 *    [com.klipy.klipy_ui.picker.KlipyPickerDialogFragment]. This mirrors the way
 *    GIPHY does it and keeps setup simple for apps that only want a picker.
 */
object KlipyUi {

    @Volatile
    private var repository: KlipyRepository? = null

    @Volatile
    private var repositoryFactory: (() -> KlipyRepository)? = null

    fun configure(repo: KlipyRepository) {
        repository = repo
        repositoryFactory = null
    }

    /** Configure with a factory (lazy). */
    fun configure(factory: () -> KlipyRepository) {
        repository = null
        repositoryFactory = factory
    }

    /** Returns the configured repository or null if none has been configured. */
    fun getRepositoryOrNull(): KlipyRepository? {
        repository?.let { return it }

        val factory = repositoryFactory ?: return null
        synchronized(this) {
            repository?.let { return it }
            val created = factory.invoke()
            repository = created
            repositoryFactory = null
            return created
        }
    }

    /** Legacy behavior for callers that want a hard requirement. */
    fun requireRepository(): KlipyRepository {
        return getRepositoryOrNull()
            ?: error(
                "KlipyUi is not configured. Either call KlipyUi.configure(repo) or use " +
                        "KlipyPickerDialogFragment.newInstance(config, secretKey, ...)"
            )
    }
}