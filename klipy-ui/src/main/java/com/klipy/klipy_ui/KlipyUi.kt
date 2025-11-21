package com.klipy.klipy_ui

import com.klipy.sdk.KlipyRepository

object KlipyUi {

    @Volatile
    private var repository: KlipyRepository? = null

    fun configure(repo: KlipyRepository) {
        repository = repo
    }

    fun requireRepository(): KlipyRepository =
        repository ?: error("KlipyUi.configure(repo) must be called before using Klipy picker")
}