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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.domain.Pinyin
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.domain.model.CharacterInfo
import fr.kairossolum.pangmao.domain.model.ExampleSentence
import fr.kairossolum.pangmao.ui.common.DefinitionList
import fr.kairossolum.pangmao.ui.common.LocalDefinitionLanguage
import fr.kairossolum.pangmao.ui.common.PinyinText
import fr.kairossolum.pangmao.ui.common.rememberMandarinSpeaker
import fr.kairossolum.pangmao.data.settings.DefinitionLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(viewModel: EntryViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isFlashcard by viewModel.isFlashcard.collectAsStateWithLifecycle()
    val speaker = rememberMandarinSpeaker()
    val definitionLanguage = LocalDefinitionLanguage.current

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
                            Text(
                                entry.displayHeadword,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        entry.sources.split(" · ").forEach { source ->
                            AssistChip(onClick = {}, label = { Text(source) })
                        }
                        if (entry.frequency > 0) {
                            AssistChip(onClick = {}, label = { Text(stringResource(R.string.frequent, entry.frequency)) })
                        }
                    }

                    if (definitionLanguage != DefinitionLanguage.ENGLISH) {
                        DefinitionList(stringResource(R.string.french), entry.definitionsFrench)
                    }
                    if (definitionLanguage != DefinitionLanguage.FRENCH) {
                        DefinitionList(stringResource(R.string.english), entry.definitionsEnglish)
                    }

                    if (state.examples.isNotEmpty()) {
                        HorizontalDivider()
                        Text(stringResource(R.string.authentic_examples), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        state.examples.forEach { ExampleCard(it, onSpeak = { speaker.speak(it.chinese) }) }
                    }

                    if (state.characters.isNotEmpty()) {
                        HorizontalDivider()
                        Text(stringResource(R.string.characters), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        state.characters.forEach { character -> CharacterCard(character) }
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
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ExampleCard(example: ExampleSentence, onSpeak: () -> Unit) {
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
            Text(example.english, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CharacterCard(info: CharacterInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(info.character, fontSize = 38.sp, color = MaterialTheme.colorScheme.primary)
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
