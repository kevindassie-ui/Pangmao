package fr.kairossolum.pangmao.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.domain.Pinyin
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.domain.model.CharacterInfo
import fr.kairossolum.pangmao.domain.model.ExampleSentence
import fr.kairossolum.pangmao.domain.model.RelatedWordPosition
import fr.kairossolum.pangmao.domain.model.RelatedWordSort
import fr.kairossolum.pangmao.domain.model.WordKnowledgeStatus
import fr.kairossolum.pangmao.ui.common.DefinitionList
import fr.kairossolum.pangmao.ui.common.EntryRow
import fr.kairossolum.pangmao.ui.common.HanziText
import fr.kairossolum.pangmao.ui.common.LocalDefinitionLanguage
import fr.kairossolum.pangmao.ui.common.PinyinText
import fr.kairossolum.pangmao.ui.common.rememberMandarinSpeaker
import fr.kairossolum.pangmao.ui.common.WordKnowledgeSelector
import fr.kairossolum.pangmao.data.settings.DefinitionLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    viewModel: EntryViewModel,
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isFlashcard by viewModel.isFlashcard.collectAsStateWithLifecycle()
    val wordKnowledgeStatus by viewModel.wordKnowledgeStatus.collectAsStateWithLifecycle()
    val speaker = rememberMandarinSpeaker()
    val definitionLanguage = LocalDefinitionLanguage.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val learningSavedMessage = stringResource(R.string.knowledge_learning_saved)
    val addToCardsAction = stringResource(R.string.card_add)

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.entry_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            actions = {
                IconButton(onClick = viewModel::toggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = stringResource(if (isFavorite) R.string.favorite_remove else R.string.favorite_add),
                    )
                }
                IconButton(onClick = viewModel::toggleFlashcard) {
                    Icon(
                        if (isFlashcard) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(if (isFlashcard) R.string.card_remove else R.string.card_add),
                    )
                }
            },
        )

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            state.entry != null -> {
                val entry = checkNotNull(state.entry)
                val isSingleCharacter = entry.simplified.codePointCount(0, entry.simplified.length) == 1
                val tabLabels = buildList {
                    add(stringResource(R.string.entry_tab_definitions))
                    add(stringResource(R.string.entry_tab_examples))
                    if (isSingleCharacter) add(stringResource(R.string.entry_tab_stroke_order))
                    add(
                        stringResource(
                            if (isSingleCharacter) R.string.entry_tab_words
                            else R.string.entry_tab_characters,
                        )
                    )
                }
                var selectedTab by remember(entry.id) { mutableIntStateOf(0) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            HanziText(
                                hanzi = entry.displayHeadword,
                                numberedPinyin = entry.pinyin,
                                fontSize = 48.sp,
                                bold = true,
                            )
                            entry.alternateHeadword?.let {
                                Text(
                                    stringResource(R.string.traditional, it),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            PinyinText(entry.pinyin, fontSize = 22.sp, bold = true)
                        }
                        IconButton(onClick = { speaker.speak(entry.simplified) }) {
                            Icon(Icons.Outlined.RecordVoiceOver, contentDescription = stringResource(R.string.pronounce))
                        }
                    }

                    WordKnowledgeSelector(
                        status = wordKnowledgeStatus,
                        onSelect = { status ->
                            viewModel.setWordKnowledgeStatus(status)
                            if (status == WordKnowledgeStatus.LEARNING && !isFlashcard) {
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = learningSavedMessage,
                                        actionLabel = addToCardsAction,
                                        withDismissAction = true,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.addFlashcard()
                                    }
                                }
                            }
                        },
                    )

                    Text(
                        stringResource(R.string.knowledge_cards_independent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(
                        onClick = viewModel::toggleFlashcard,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            if (isFlashcard) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                        )
                        Text(
                            stringResource(if (isFlashcard) R.string.card_remove else R.string.card_add),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        entry.sources.split(" · ").forEach { source ->
                            AssistChip(onClick = {}, label = { Text(source) })
                        }
                        if (entry.frequency > 0) {
                            AssistChip(onClick = {}, label = { Text(stringResource(R.string.frequent, entry.frequency)) })
                        }
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth(),
                        divider = {},
                    ) {
                        tabLabels.forEachIndexed { index, label ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { EntryTabLabel(label) },
                            )
                        }
                    }

                    when (selectedTab) {
                        0 -> {
                            if (definitionLanguage != DefinitionLanguage.ENGLISH) {
                                DefinitionList(stringResource(R.string.french), entry.definitionsFrench)
                            }
                            if (definitionLanguage != DefinitionLanguage.FRENCH) {
                                DefinitionList(stringResource(R.string.english), entry.definitionsEnglish)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            ) {
                                Text(
                                    stringResource(R.string.entry_sources_note, entry.sources),
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        1 -> {
                            if (state.examples.isEmpty()) {
                                EmptyEntrySection(R.string.entry_no_examples)
                            } else {
                                state.examples.forEach { example ->
                                    ExampleCard(
                                        example = example,
                                        definitionLanguage = definitionLanguage,
                                        onSpeak = { speaker.speak(example.chinese) },
                                    )
                                }
                            }
                        }
                        2 -> {
                            if (isSingleCharacter) {
                                StrokeOrderSection(
                                    strokeOrder = state.strokeOrder,
                                    loadError = state.strokeOrderError,
                                )
                            } else if (state.characters.isEmpty()) {
                                EmptyEntrySection(R.string.entry_no_characters)
                            } else {
                                state.characters.forEach { character ->
                                    CharacterCard(
                                        info = character,
                                        entryId = state.characterEntryIds[character.character],
                                        onOpenEntry = onOpenEntry,
                                    )
                                }
                            }
                        }
                        else -> RelatedWordsSection(state, viewModel, onOpenEntry)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter),
    )
    }
}

@Composable
private fun EntryTabLabel(label: String) {
    val canWrapAtWhitespace = label.any(Char::isWhitespace)
    val minimumSingleLineFontSize = if (canWrapAtWhitespace) 9f else 8f
    var fontSize by remember(label) { mutableFloatStateOf(12f) }
    var wrapAtWhitespace by remember(label) { mutableStateOf(false) }
    val displayedLabel = remember(label, wrapAtWhitespace) {
        if (wrapAtWhitespace) entryTabFallbackLabel(label) else label
    }

    Text(
        text = displayedLabel,
        maxLines = if (wrapAtWhitespace) 2 else 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 2f).sp,
            letterSpacing = 0.sp,
        ),
        onTextLayout = { layoutResult ->
            if (layoutResult.didOverflowWidth) {
                when {
                    fontSize > minimumSingleLineFontSize -> {
                        fontSize = (fontSize - 0.5f).coerceAtLeast(minimumSingleLineFontSize)
                    }
                    canWrapAtWhitespace && !wrapAtWhitespace -> {
                        wrapAtWhitespace = true
                        fontSize = 11f
                    }
                }
            }
        },
    )
}

internal fun entryTabFallbackLabel(label: String): String =
    label.trim().split(Regex("\\s+")).joinToString(separator = "\n")

@Composable
private fun ExampleCard(
    example: ExampleSentence,
    definitionLanguage: DefinitionLanguage,
    onSpeak: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(example.chinese, modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onSpeak) {
                    Icon(Icons.Outlined.RecordVoiceOver, contentDescription = stringResource(R.string.pronounce_sentence))
                }
            }
            if (example.pinyin.isNotBlank()) {
                Text(Pinyin.withToneMarks(example.pinyin), color = MaterialTheme.colorScheme.primary)
            }
            when (definitionLanguage) {
                DefinitionLanguage.FRENCH -> {
                    if (example.french.isNotBlank()) {
                        ExampleTranslation(R.string.french, example.frenchSource, example.french)
                    } else {
                        ExampleTranslation(R.string.english, example.englishSource, example.english)
                    }
                }
                DefinitionLanguage.ENGLISH -> {
                    ExampleTranslation(R.string.english, example.englishSource, example.english)
                }
                DefinitionLanguage.BOTH -> {
                    if (example.french.isNotBlank()) {
                        ExampleTranslation(R.string.french, example.frenchSource, example.french)
                    }
                    ExampleTranslation(R.string.english, example.englishSource, example.english)
                }
            }
        }
    }
}

@Composable
private fun ExampleTranslation(
    @androidx.annotation.StringRes language: Int,
    source: String,
    text: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            buildString {
                append(stringResource(language))
                if (source.isNotBlank()) append(" · $source")
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CharacterCard(
    info: CharacterInfo,
    entryId: Long?,
    onOpenEntry: (Long) -> Unit,
) {
    Surface(
        onClick = { entryId?.let(onOpenEntry) },
        enabled = entryId != null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            HanziText(info.character, info.mandarin, fontSize = 38.sp, bold = true)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    listOfNotNull(
                        info.mandarin.takeIf(String::isNotBlank),
                        info.totalStrokes.takeIf(String::isNotBlank)?.let { stringResource(R.string.strokes, it) },
                        info.radical?.let { stringResource(R.string.radical_detail, it, info.additionalStrokes?.let { extra -> " + $extra" }.orEmpty()) },
                    ).joinToString(" · "),
                    fontWeight = FontWeight.SemiBold,
                )
                if (info.definition.isNotBlank()) Text(info.definition, style = MaterialTheme.typography.bodySmall)
                val variants = buildList {
                    if (info.simplifiedVariants.isNotBlank()) add(stringResource(R.string.simplified_short, info.simplifiedVariants))
                    if (info.traditionalVariants.isNotBlank()) add(stringResource(R.string.traditional_short, info.traditionalVariants))
                }.joinToString(" · ")
                if (variants.isNotBlank()) Text(variants, style = MaterialTheme.typography.labelSmall)
                Text(info.codepoint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RelatedWordsSection(
    state: EntryUiState,
    viewModel: EntryViewModel,
    onOpenEntry: (Long) -> Unit,
) {
    Text(
        stringResource(R.string.entry_words_help),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(RelatedWordPosition.entries) { position ->
            val label = when (position) {
                RelatedWordPosition.CONTAINS -> R.string.entry_words_contains
                RelatedWordPosition.STARTS_WITH -> R.string.entry_words_starts
                RelatedWordPosition.ENDS_WITH -> R.string.entry_words_ends
            }
            FilterChip(
                selected = state.relatedWordPosition == position,
                onClick = { viewModel.setRelatedWordPosition(position) },
                label = { Text(stringResource(label)) },
            )
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = state.frequentWordsOnly,
                onClick = { viewModel.setFrequentWordsOnly(!state.frequentWordsOnly) },
                label = { Text(stringResource(R.string.entry_words_frequent_only)) },
            )
        }
        items(RelatedWordSort.entries) { sort ->
            FilterChip(
                selected = state.relatedWordSort == sort,
                onClick = { viewModel.setRelatedWordSort(sort) },
                label = {
                    Text(
                        stringResource(
                            if (sort == RelatedWordSort.FREQUENCY) R.string.entry_words_sort_frequency
                            else R.string.entry_words_sort_pinyin
                        )
                    )
                },
            )
        }
    }
    when {
        state.isWordsLoading -> Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        state.wordsError != null -> Text(state.wordsError, color = MaterialTheme.colorScheme.error)
        state.relatedWords.isEmpty() -> EmptyEntrySection(R.string.entry_no_words)
        else -> state.relatedWords.forEach { word ->
            EntryRow(word, onClick = { onOpenEntry(word.id) })
        }
    }
}

@Composable
private fun EmptyEntrySection(@androidx.annotation.StringRes message: Int) {
    Text(
        stringResource(message),
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
