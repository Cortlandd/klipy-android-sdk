package com.klipy.klipy_demo.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.klipy.klipy_ui.components.MediaItemPreview
import com.klipy.klipy_ui.picker.KlipyPickerThemeMode
import com.klipy.sdk.model.MediaType
import com.klipy.sdk.model.singularName

@Composable
fun HomeScreen(
    state: HomeState,
    reducer: HomeReducer
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
            Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Klipy Demo",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Benchmark-style sample for the Klipy picker. Configure layout, shell behavior, feed defaults, and visible media tabs before opening it.",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = state.pickerSettings.summary(),
            style = MaterialTheme.typography.bodySmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { reducer.postAction(HomeAction.OpenPickerClicked) }
            ) {
                Text(text = "Open Klipy Picker")
            }

            OutlinedButton(
                onClick = { reducer.postAction(HomeAction.OpenSettingsClicked) }
            ) {
                Text(text = "Configure Demo")
            }
        }

        state.lastSelected?.let { item ->
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Last selected:",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = item.title ?: item.mediaType.singularName(),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            MediaItemPreview(item)
        }

        state.lastSearchTerm
            ?.takeIf { it.isNotBlank() }
            ?.let { term ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Last search: \"$term\"",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
    }

    if (state.showSettings) {
        PickerSettingsSheet(
            settings = state.pickerSettings,
            onDismiss = { reducer.postAction(HomeAction.SettingsDismissed) },
            onThemeModeChanged = { reducer.postAction(HomeAction.ThemeModeChanged(it)) },
            onColumnsChanged = { reducer.postAction(HomeAction.ColumnsChanged(it)) },
            onDefaultFeedChanged = { reducer.postAction(HomeAction.DefaultFeedChanged(it)) },
            onCustomColorsChanged = { reducer.postAction(HomeAction.CustomColorsChanged(it)) },
            onSearchVisibilityChanged = { reducer.postAction(HomeAction.SearchVisibilityChanged(it)) },
            onConfirmationScreenChanged = { reducer.postAction(HomeAction.ConfirmationScreenChanged(it)) },
            onItemSpacingChanged = { reducer.postAction(HomeAction.ItemSpacingChanged(it)) },
            onMediaTypeToggled = { reducer.postAction(HomeAction.MediaTypeToggled(it)) }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PickerSettingsSheet(
    settings: PickerDemoSettings,
    onDismiss: () -> Unit,
    onThemeModeChanged: (KlipyPickerThemeMode) -> Unit,
    onColumnsChanged: (Int) -> Unit,
    onDefaultFeedChanged: (DemoPickerDefaultFeed) -> Unit,
    onCustomColorsChanged: (Boolean) -> Unit,
    onSearchVisibilityChanged: (Boolean) -> Unit,
    onConfirmationScreenChanged: (Boolean) -> Unit,
    onItemSpacingChanged: (Int) -> Unit,
    onMediaTypeToggled: (MediaType) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Picker Settings",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "These controls mirror the minimum benchmark features we want integrators to evaluate quickly.",
                style = MaterialTheme.typography.bodyMedium
            )

            SettingsSection(title = "Theme mode") {
                ChipRow {
                    KlipyPickerThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeModeChanged(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }

            SettingsSection(title = "Columns") {
                ChipRow {
                    listOf(2, 3, 4).forEach { columns ->
                        FilterChip(
                            selected = settings.columns == columns,
                            onClick = { onColumnsChanged(columns) },
                            label = { Text("$columns columns") }
                        )
                    }
                }
            }

            SettingsSection(title = "Item spacing") {
                ChipRow {
                    listOf(0, 1, 2, 4, 8).forEach { spacing ->
                        FilterChip(
                            selected = settings.itemSpacingDp == spacing,
                            onClick = { onItemSpacingChanged(spacing) },
                            label = { Text("${spacing}dp") }
                        )
                    }
                }
            }

            SettingsSection(title = "Default feed") {
                ChipRow {
                    DemoPickerDefaultFeed.entries.forEach { feed ->
                        FilterChip(
                            selected = settings.defaultFeed == feed,
                            onClick = { onDefaultFeedChanged(feed) },
                            label = { Text(feed.label) }
                        )
                    }
                }
            }

            SettingsSection(title = "Media tabs") {
                ChipRow {
                    listOf(MediaType.GIF, MediaType.STICKER, MediaType.CLIP, MediaType.MEME).forEach { type ->
                        FilterChip(
                            selected = settings.mediaTypes.contains(type),
                            onClick = { onMediaTypeToggled(type) },
                            label = { Text(type.singularName()) }
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Show search field",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Lets us compare the full picker shell against simpler feed-first integrations.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = settings.showSearch,
                    onCheckedChange = onSearchVisibilityChanged
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Confirm before select",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Adds a preview confirmation step for normal media while keeping ads inline and untouched.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = settings.showConfirmationScreen,
                    onCheckedChange = onConfirmationScreenChanged
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Use demo brand colors",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Shows how host apps can override picker colors without editing SDK resources.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = settings.useCustomColors,
                    onCheckedChange = onCustomColorsChanged
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        content()
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

private val KlipyPickerThemeMode.label: String
    get() = when (this) {
        KlipyPickerThemeMode.AUTOMATIC -> "Automatic"
        KlipyPickerThemeMode.LIGHT -> "Light"
        KlipyPickerThemeMode.DARK -> "Dark"
    }

private val DemoPickerDefaultFeed.label: String
    get() = when (this) {
        DemoPickerDefaultFeed.TRENDING -> "Trending"
        DemoPickerDefaultFeed.RECENTS -> "Recents"
        DemoPickerDefaultFeed.EMPTY -> "Empty"
    }

private fun PickerDemoSettings.summary(): String {
    val mediaSummary = mediaTypes.joinToString { it.singularName() }
    return "Theme: ${themeMode.label} • " +
        "Columns: $columns • Feed: ${defaultFeed.label} • " +
        "Search: ${if (showSearch) "On" else "Off"} • " +
        "Confirm: ${if (showConfirmationScreen) "On" else "Off"} • " +
        "Gap: ${itemSpacingDp}dp • " +
        "Colors: ${if (useCustomColors) "Custom" else "Default"} • " +
        "Tabs: $mediaSummary"
}

@Preview
@Composable
fun HomeScreen_Preview() {
    HomeScreen(
        state = HomeState(),
        reducer = HomeReducer()
    )
}
