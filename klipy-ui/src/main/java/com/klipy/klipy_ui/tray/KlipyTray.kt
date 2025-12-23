package com.klipy.klipy_ui.tray

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klipy.sdk.model.MediaItem
import kotlinx.coroutines.flow.collectLatest

/**
 * Drop-in Klipy tray for Compose.
 *
 * This owns its own ViewModel internally, and calls
 * when a media item is chosen or an error occurs.
 */
@Composable
fun KlipyTray(
    config: KlipyTrayConfig = KlipyTrayConfig(),
    onMediaSelected: (MediaItem) -> Unit,
    onError: (String) -> Unit = {}
) {
    // Factory is remembered so we don't recreate it on every recomposition
    val factory = remember(config) { KlipyTrayViewModel.factory(config) }

    // Use a unique key so we don't collide with other StoreViewModels
    val vm: KlipyTrayViewModel = viewModel(
        key = "KlipyTrayViewModel",
        factory = factory
    )

    val state by vm.state.collectAsState()

    LaunchedEffect(vm) {
        vm.start()
    }

    LaunchedEffect(vm) {
        vm.effects.collectLatest { effect ->
            when (effect) {
                is KlipyTrayEffect.ShowError -> onError(effect.message)
                is KlipyTrayEffect.MediaChosen -> onMediaSelected(effect.item)
            }
        }
    }

    KlipyTrayContent(
        state = state,
        config = config,
        onAction = vm::dispatch
    )
}
