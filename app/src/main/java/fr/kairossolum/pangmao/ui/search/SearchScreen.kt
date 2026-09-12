package fr.kairossolum.pangmao.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.ui.common.EntryRow
import fr.kairossolum.pangmao.ui.common.PinyinText
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.model.TextAnalysis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenEntry: (Long) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHandwriting: () -> Unit,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Pangmao · 胖猫", fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.search_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                }
                IconButton(onClick = onOpenAbout) {
                    Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about_and_licenses))
                }
            },
        )
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.clear))
                    }
                }
            },
            label = { Text(stringResource(R.string.search_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(onClick = { focusRequester.requestFocus() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Text("  ${stringResource(R.string.input_keyboard)}")
            }
            FilledTonalButton(onClick = onOpenHandwriting, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Draw, contentDescription = null)
                Text("  ${stringResource(R.string.input_handwriting)}")
            }
        }

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
            query.isBlank() -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (history.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.search_recent)) }
                    items(history, key = { "history-${it.id}" }) { entry ->
                        EntryRow(entry, onClick = { onOpenEntry(entry.id) })
                    }
                    item { SectionTitle(stringResource(R.string.search_frequent)) }
                } else {
                    item { SectionTitle(stringResource(R.string.search_frequent)) }
                }
                items(state.results, key = { "popular-${it.id}" }) { entry ->
                    EntryRow(entry, onClick = { onOpenEntry(entry.id) })
                }
            }
            state.results.isEmpty() && state.analysis == null -> EmptySearch(query)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                state.analysis?.let { analysis ->
                    item(key = "analysis") {
                        SearchAnalysisCard(analysis, onOpenEntry)
                    }
                    if (state.results.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.search_dictionary_results)) }
                    }
                }
                items(state.results, key = { it.id }) { entry ->
                    EntryRow(entry, onClick = { onOpenEntry(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun SearchAnalysisCard(analysis: TextAnalysis, onOpenEntry: (Long) -> Unit) {
    val chineseTokens = analysis.tokens.filter { it.token.isChinese }
    val exactMeaning = analysis.exactEntry?.let(::preferredMeaning)
    val gloss = chineseTokens.mapNotNull { it.entry?.let(::preferredMeaning) }.joinToString(" · ")
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.search_analysis),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(analysis.sourceText, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            when {
                !exactMeaning.isNullOrBlank() -> MeaningBlock(R.string.search_whole_expression, exactMeaning)
                analysis.exactExample != null -> MeaningBlock(
                    R.string.search_attested_translation,
                    analysis.exactExample.english,
                )
                gloss.isNotBlank() -> MeaningBlock(R.string.search_word_gloss, gloss)
            }
            Text(
                stringResource(R.string.search_breakdown),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            chineseTokens.forEach { token -> AnalysisTokenRow(token, onOpenEntry) }
        }
    }
}

@Composable
private fun MeaningBlock(@androidx.annotation.StringRes title: Int, meaning: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(meaning, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AnalysisTokenRow(token: AnalyzedToken, onOpenEntry: (Long) -> Unit) {
    val entry = token.entry
    androidx.compose.material3.Surface(
        onClick = { entry?.let { onOpenEntry(it.id) } },
        enabled = entry != null,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(token.token.text, fontSize = 25.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (entry != null) {
                    PinyinText(entry.pinyin, fontSize = 14.sp)
                    Text(preferredMeaning(entry), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                } else {
                    Text(stringResource(R.string.search_unknown_block), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun preferredMeaning(entry: fr.kairossolum.pangmao.domain.model.DictionaryEntry): String =
    entry.definitionsFrench.firstOrNull() ?: entry.definitionsEnglish.firstOrNull() ?: "—"

@Composable
private fun SectionTitle(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptySearch(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.search_empty, query), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.search_empty_help),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
