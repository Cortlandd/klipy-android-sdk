package com.klipy.klipy_demo.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.klipy.klipy_demo.features.home.ui.SelectedMediaPreview
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

        Button(
            onClick = { reducer.postAction(HomeAction.OpenPickerClicked) }
        ) {
            Text(text = "Open Klipy Picker")
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

            SelectedMediaPreview(item)
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
}

@Preview
@Composable
fun HomeScreen_Preview() {
//    HomeScreen(
//        state = HomeState(),
//        dispatch = reducer::postAction
//    )
}