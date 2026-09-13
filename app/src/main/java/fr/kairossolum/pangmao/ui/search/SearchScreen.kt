package fr.kairossolum.pangmao.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.ui.common.EntryRow
import fr.kairossolum.pangmao.ui.common.HanziText
import fr.kairossolum.pangmao.ui.common.coloredHanzi
import fr.kairossolum.pangmao.ui.common.PinyinText
import fr.kairossolum.pangmao.ui.common.LocalDefinitionLanguage
import fr.kairossolum.pangmao.ui.common.primaryDefinition
import fr.kairossolum.pangmao.ui.common.TextTranslationState
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.model.LearningLanguage
import fr.kairossolum.pangmao.domain.model.TextAnalysis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenEntry: (Long) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHandwriting: () -> Unit,
    onOpenOcr: () -> Unit,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val queryHistory by viewModel.queryHistory.collectAsStateWithLifecycle()
    val learningLanguage by viewModel.learningLanguage.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_pangmao_v2),
                        contentDescription = stringResource(R.string.app_mascot),
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Column {
                        Text("胖猫", fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(learningLanguage.subtitleResource()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
        LearningProfileSelector(
            selected = learningLanguage,
            onSelect = viewModel::setLearningLanguage,
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
            label = { Text(stringResource(learningLanguage.searchHintResource())) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(
                onClick = { focusRequester.requestFocus() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Text(" ${stringResource(R.string.input_keyboard)}", maxLines = 1)
            }
            FilledTonalButton(
                onClick = onOpenHandwriting,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Outlined.Draw, contentDescription = null)
                Text(" ${stringResource(R.string.input_handwriting)}", maxLines = 1)
            }
            FilledTonalButton(
                onClick = onOpenOcr,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                Text(" ${stringResource(R.string.input_ocr)}", maxLines = 1)
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
                if (queryHistory.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = stringResource(R.string.search_query_history),
                            action = stringResource(R.string.clear_all),
                            onAction = viewModel::clearQueryHistory,
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(queryHistory, key = { "query-${it.query}" }) { item ->
                                InputChip(
                                    selected = false,
                                    onClick = { viewModel.setQuery(item.query) },
                                    label = { Text(item.query, maxLines = 1) },
                                    trailingIcon = {
                                        IconButton(onClick = { viewModel.deleteQuery(item.query) }, modifier = Modifier.size(24.dp)) {
                                            Icon(
                                                Icons.Outlined.Close,
                                                contentDescription = stringResource(R.string.delete_history_item),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                if (history.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.search_viewed_history)) }
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
                state.analysis
                    ?.takeIf { it.exactEntry == null || state.results.isEmpty() }
                    ?.let { analysis ->
                    item(key = "analysis") {
                        SearchAnalysisCard(
                            analysis = analysis,
                            translation = state.translation,
                            onDownloadTranslation = viewModel::downloadTranslationModels,
                            onOpenEntry = onOpenEntry,
                        )
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
private fun LearningProfileSelector(
    selected: LearningLanguage,
    onSelect: (LearningLanguage) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            stringResource(R.string.learning_profile_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LearningLanguage.entries, key = { it.name }) { language ->
                InputChip(
                    selected = language == selected,
                    onClick = { onSelect(language) },
                    label = { Text(stringResource(language.labelResource())) },
                )
            }
        }
    }
}

@StringRes
private fun LearningLanguage.labelResource(): Int = when (this) {
    LearningLanguage.CHINESE -> R.string.learning_profile_chinese
    LearningLanguage.FRENCH -> R.string.learning_profile_french
    LearningLanguage.ENGLISH -> R.string.learning_profile_english
}

@StringRes
private fun LearningLanguage.subtitleResource(): Int = when (this) {
    LearningLanguage.CHINESE -> R.string.search_subtitle
    LearningLanguage.FRENCH -> R.string.search_subtitle_french_profile
    LearningLanguage.ENGLISH -> R.string.search_subtitle_english_profile
}

@StringRes
private fun LearningLanguage.searchHintResource(): Int = when (this) {
    LearningLanguage.CHINESE -> R.string.search_hint
    LearningLanguage.FRENCH -> R.string.search_hint_french_profile
    LearningLanguage.ENGLISH -> R.string.search_hint_english_profile
}

@Composable
private fun SearchAnalysisCard(
    analysis: TextAnalysis,
    translation: TextTranslationState,
    onDownloadTranslation: () -> Unit,
    onOpenEntry: (Long) -> Unit,
) {
    val definitionLanguage = LocalDefinitionLanguage.current
    val chineseTokens = analysis.tokens.filter { it.token.isChinese }
    val exactMeaning = analysis.exactEntry?.primaryDefinition(definitionLanguage)
    val gloss = chineseTokens.mapNotNull { it.entry?.primaryDefinition(definitionLanguage) }.joinToString(" · ")
    var showWordGloss by remember(analysis.sourceText) { mutableStateOf(false) }
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
            Text(
                buildAnnotatedString {
                    analysis.tokens.forEach { token ->
                        append(coloredHanzi(token.token.text, token.entry?.pinyin.orEmpty(), bold = true))
                    }
                },
                fontSize = 30.sp,
                lineHeight = 42.sp,
            )
            translation.french?.let { MeaningBlock(R.string.french, it) }
            translation.english?.let {
                MeaningBlock(
                    if (translation.englishIsAttested) R.string.reader_english_attested else R.string.english,
                    it,
                )
            }
            if (translation.isBusy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(
                            if (translation.isDownloading) R.string.reader_translation_downloading
                            else R.string.reader_translation_working
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (translation.missingTargets.isNotEmpty() && !translation.isDownloading) {
                Button(onClick = onDownloadTranslation) {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Text("  ${stringResource(R.string.reader_translation_download)}")
                }
            }
            if (translation.error != null) {
                Text(stringResource(R.string.reader_translation_error), color = MaterialTheme.colorScheme.error)
            }
            if (!exactMeaning.isNullOrBlank()) {
                MeaningBlock(R.string.search_whole_expression, exactMeaning)
            }
            if (gloss.isNotBlank()) TextButton(onClick = { showWordGloss = !showWordGloss }) {
                Icon(
                    if (showWordGloss) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                )
                Text(
                    stringResource(
                        if (showWordGloss) R.string.search_hide_breakdown
                        else R.string.search_show_breakdown
                    )
                )
            }
            if (showWordGloss) MeaningBlock(R.string.search_word_gloss, gloss)
            Text(
                stringResource(R.string.search_breakdown),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            chineseTokens.forEach { token -> AnalysisTokenRow(token, definitionLanguage, onOpenEntry) }
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
private fun AnalysisTokenRow(
    token: AnalyzedToken,
    definitionLanguage: fr.kairossolum.pangmao.data.settings.DefinitionLanguage,
    onOpenEntry: (Long) -> Unit,
) {
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
            HanziText(
                token.token.text,
                entry?.pinyin.orEmpty(),
                fontSize = 25.sp,
                bold = true,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (entry != null) {
                    PinyinText(entry.pinyin, fontSize = 14.sp)
                    Text(entry.primaryDefinition(definitionLanguage), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                } else {
                    Text(stringResource(R.string.search_unknown_block), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
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
