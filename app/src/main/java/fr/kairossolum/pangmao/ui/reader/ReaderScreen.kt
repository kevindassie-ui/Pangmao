package fr.kairossolum.pangmao.ui.reader

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.ui.common.QuickEntryCard
import fr.kairossolum.pangmao.ui.common.TokenizedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onOpenEntry: (Long) -> Unit,
) {
    val text by viewModel.text.collectAsStateWithLifecycle()
    val tokens by viewModel.tokens.collectAsStateWithLifecycle()
    val selected by viewModel.selectedEntry.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                content?.let(viewModel::setText)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Lecteur", fontWeight = FontWeight.Bold)
                    Text("Touchez un mot pour le définir", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { filePicker.launch(arrayOf("text/plain", "text/*")) }) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                Text(" Ouvrir")
            }
            OutlinedButton(onClick = { clipboard.getText()?.text?.let(viewModel::setText) }) {
                Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                Text(" Coller")
            }
            OutlinedButton(onClick = { viewModel.setText("") }) {
                Icon(Icons.Outlined.Clear, contentDescription = null)
                Text(" Effacer")
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = viewModel::setText,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 190.dp)
                .padding(horizontal = 12.dp),
            label = { Text("Texte chinois") },
            placeholder = { Text("Collez, partagez ou ouvrez un fichier texte…") },
        )
        error?.let {
            Text(
                it,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        HorizontalDivider(Modifier.padding(top = 10.dp))
        Text(
            "LECTURE SEGMENTÉE",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (tokens.isEmpty()) {
            Text(
                "Le texte annoté apparaîtra ici.",
                modifier = Modifier.padding(18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TokenizedText(
                tokens = tokens,
                onTokenClick = viewModel::select,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                darkTheme = isSystemInDarkTheme(),
            )
        }
    }

    selected?.let { entry ->
        ModalBottomSheet(onDismissRequest = viewModel::dismissSelection) {
            QuickEntryCard(
                entry = entry,
                onOpen = {
                    viewModel.dismissSelection()
                    onOpenEntry(entry.id)
                },
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}
