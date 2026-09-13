package fr.kairossolum.pangmao.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.domain.model.LearningDictionaryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningEntryScreen(
    viewModel: LearningEntryViewModel,
    onBack: () -> Unit,
    onOpenChineseEntry: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.learning_entry_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
        )
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    state.error.orEmpty(),
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.entry != null -> LearningEntryContent(
                entry = checkNotNull(state.entry),
                chineseEntryIds = state.chineseEntryIds,
                onOpenChineseEntry = onOpenChineseEntry,
            )
        }
    }
}

@Composable
private fun LearningEntryContent(
    entry: LearningDictionaryEntry,
    chineseEntryIds: Map<String, Long>,
    onOpenChineseEntry: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    entry.displayHeadword,
                    fontSize = 42.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (entry.forms.size > 1) {
                    EntryFact(
                        stringResource(R.string.learning_alternative_forms),
                        entry.forms.drop(1).joinToString(" · "),
                    )
                }
                if (entry.pronunciations.isNotEmpty()) {
                    EntryFact(
                        stringResource(R.string.learning_pronunciations),
                        entry.pronunciations.joinToString(" · "),
                    )
                }
                val grammar = (entry.partsOfSpeech + entry.genders).joinToString(" · ")
                if (grammar.isNotBlank()) {
                    EntryFact(stringResource(R.string.learning_grammar), grammar)
                }
            }
        }

        items(entry.senses, key = { it.id }) { sense ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        stringResource(R.string.learning_chinese_meanings),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sense.chineseEquivalents, key = { it }) { meaning ->
                            val target = chineseEntryIds[meaning]
                            AssistChip(
                                onClick = { target?.let(onOpenChineseEntry) },
                                enabled = target != null,
                                label = { Text(meaning, fontSize = 20.sp) },
                            )
                        }
                    }
                    if (sense.definitions.isNotEmpty()) {
                        EntryFact(
                            label = stringResource(R.string.learning_definition),
                            value = sense.definitions.joinToString("\n"),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }

        item {
            Text(
                stringResource(R.string.learning_source_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EntryFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
