package com.romexplorer.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.romexplorer.app.data.RomEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(game: RomEntry, onBack: () -> Unit, onPlay: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game.displayTitle) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            AsyncImage(
                model = game.reBoxArtUrl,
                contentDescription = game.displayTitle,
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(game.displayTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${game.system}${game.reGenre?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))
            Button(onClick = onPlay) { Text("Play") }

            if (game.raAchievementsTotal != null) {
                Spacer(Modifier.height(16.dp))
                Text("RetroAchievements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { (game.raAchievementsUnlocked ?: 0).toFloat() / game.raAchievementsTotal.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                Text("${game.raAchievementsUnlocked ?: 0} / ${game.raAchievementsTotal} unlocked")
            }

            if (!game.reDescription.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(game.reDescription, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text("File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(game.fileName, style = MaterialTheme.typography.bodySmall)
            Text("${game.sizeBytes / (1024 * 1024)} MB", style = MaterialTheme.typography.bodySmall)
        }
    }
}
