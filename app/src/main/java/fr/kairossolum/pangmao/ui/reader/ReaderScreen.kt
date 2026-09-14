package fr.kairossolum.pangmao.ui.reader

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.data.settings.DefinitionLanguage
import fr.kairossolum.pangmao.data.settings.SpeechRate
import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.numberedPinyinReading
import fr.kairossolum.pangmao.ui.common.LocalDefinitionLanguage
import fr.kairossolum.pangmao.ui.common.HanziText
import fr.kairossolum.pangmao.ui.common.PinyinText
import fr.kairossolum.pangmao.ui.common.QuickEntryCard
import fr.kairossolum.pangmao.ui.common.TextTranslationState
import fr.kairossolum.pangmao.ui.common.rememberMandarinSpeaker
import fr.kairossolum.pangmao.ui.common.SpeakerPlaybackState
import fr.kairossolum.pangmao.ui.common.SpeakerStatus
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
    val analysis by viewModel.analysis.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val translation by viewModel.translationState.collectAsStateWithLifecycle()
    val selected by viewModel.selectedEntry.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val speaker = rememberMandarinSpeaker()
    val definitionLanguage = LocalDefinitionLanguage.current
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
    var showPinyin by rememberSaveable { mutableStateOf(true) }
    var showTranslation by rememberSaveable { mutableStateOf(true) }
    var showDefinitions by rememberSaveable { mutableStateOf(true) }
    var revealPinyinAtTop by rememberSaveable { mutableStateOf(false) }
    val readingListState = rememberLazyListState()

    LaunchedEffect(revealPinyinAtTop) {
        if (revealPinyinAtTop) {
            readingListState.animateScrollToItem(0)
            revealPinyinAtTop = false
        }
    }

    LaunchedEffect(text) {
        speaker.stop()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.reader_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.reader_subtitle), style = MaterialTheme.typography.labelSmall)
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        when (speaker.playbackState) {
                            SpeakerPlaybackState.IDLE -> speaker.speak(text)
                            SpeakerPlaybackState.PLAYING -> speaker.pause()
                            SpeakerPlaybackState.PAUSED -> speaker.resume()
                        }
                    },
                    enabled = speaker.ready && text.isNotBlank(),
                ) {
                    val icon = when (speaker.playbackState) {
                        SpeakerPlaybackState.IDLE -> Icons.Outlined.RecordVoiceOver
                        SpeakerPlaybackState.PLAYING -> Icons.Outlined.Pause
                        SpeakerPlaybackState.PAUSED -> Icons.Outlined.PlayArrow
                    }
                    val description = when (speaker.playbackState) {
                        SpeakerPlaybackState.IDLE -> R.string.reader_speak
                        SpeakerPlaybackState.PLAYING -> R.string.tts_pause
                        SpeakerPlaybackState.PAUSED -> R.string.tts_resume
                    }
                    Icon(icon, contentDescription = stringResource(description))
                }
                if (speaker.playbackState != SpeakerPlaybackState.IDLE) {
                    IconButton(onClick = speaker::stop) {
                        Icon(
                            Icons.Outlined.Stop,
                            contentDescription = stringResource(R.string.tts_stop),
                        )
                    }
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { filePicker.launch(arrayOf("text/plain", "text/*")) }) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                Text(" ${stringResource(R.string.open)}")
            }
            OutlinedButton(onClick = { clipboard.getText()?.text?.let(viewModel::setText) }) {
                Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                Text(" ${stringResource(R.string.paste)}")
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = viewModel::setText,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp, max = 175.dp)
                .padding(horizontal = 12.dp),
            label = { Text(stringResource(R.string.reader_field)) },
            placeholder = { Text(stringResource(R.string.reader_placeholder)) },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setText("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.clear))
                    }
                }
            },
        )
        error?.let {
            Text(
                it,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (speaker.status == SpeakerStatus.MISSING_CHINESE_VOICE || speaker.status == SpeakerStatus.ERROR) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(
                        if (speaker.status == SpeakerStatus.MISSING_CHINESE_VOICE) R.string.tts_voice_missing
                        else R.string.tts_unavailable
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(onClick = { speaker.retry(context) }) {
                        Text(stringResource(R.string.tts_retry))
                    }
                    TextButton(onClick = { speaker.openVoiceSettings(context) }) {
                        Text(stringResource(R.string.tts_open_settings))
                    }
                }
            }
        }
        if (speaker.ready && text.isNotBlank()) {
            SpeechRateSelector(
                selected = speechRate,
                onSelect = viewModel::setSpeechRate,
            )
        }
        HorizontalDivider(Modifier.padding(top = 10.dp))
        when {
            isAnalyzing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            analysis == null -> Text(
                stringResource(R.string.reader_empty),
                modifier = Modifier.padding(18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                val currentAnalysis = checkNotNull(analysis)
                LazyColumn(
                    state = readingListState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (showPinyin) {
                        item(key = "continuous-pinyin") {
                            ContinuousPinyin(currentAnalysis.numberedPinyinReading())
                        }
                    }
                    item(key = "layers") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = showPinyin,
                                    onClick = {
                                        showPinyin = !showPinyin
                                        if (showPinyin) revealPinyinAtTop = true
                                    },
                                    label = { Text(stringResource(R.string.reader_layer_pinyin)) },
                                )
                            }
                            item {
                                FilterChip(
                                    selected = showTranslation,
                                    onClick = { showTranslation = !showTranslation },
                                    label = { Text(stringResource(R.string.reader_layer_translation)) },
                                )
                            }
                            item {
                                FilterChip(
                                    selected = showDefinitions,
                                    onClick = { showDefinitions = !showDefinitions },
                                    label = { Text(stringResource(R.string.reader_layer_definitions)) },
                                )
                            }
                        }
                    }
                    if (showTranslation) {
                        item(key = "translation") {
                            ReaderTranslationCard(
                                translation = translation,
                                onDownload = viewModel::downloadTranslationModels,
                            )
                        }
                    }
                    item(key = "blocks-title") {
                        Text(
                            stringResource(R.string.reader_segmented),
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    val chineseTokens = currentAnalysis.tokens.filter { it.token.isChinese }
                    itemsIndexed(
                        chineseTokens,
                        key = { index, token -> "${index}-${token.token.text}-${token.token.entryId}" },
                    ) { _, token ->
                        ReaderTokenCard(
                            analyzed = token,
                            definitionLanguage = definitionLanguage,
                            showPinyin = showPinyin,
                            showDefinitions = showDefinitions,
                            onClick = { viewModel.select(token) },
                        )
                    }
                }
            }
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

@Composable
private fun SpeechRateSelector(
    selected: SpeechRate,
    onSelect: (SpeechRate) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.tts_speed),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpeechRate.entries.forEach { rate ->
            FilterChip(
                selected = rate == selected,
                onClick = { onSelect(rate) },
                label = { Text(rate.label) },
            )
        }
    }
}

@Composable
private fun ReaderTranslationCard(
    translation: TextTranslationState,
    onDownload: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            translation.french?.let {
                TranslationLine(stringResource(R.string.french), it)
            }
            translation.english?.let {
                val title = if (translation.englishIsAttested) {
                    stringResource(R.string.reader_english_attested)
                } else {
                    stringResource(R.string.english)
                }
                TranslationLine(title, it)
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
                Text(
                    stringResource(R.string.reader_translation_download_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = onDownload) {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Text("  ${stringResource(R.string.reader_translation_download)}")
                }
            }
            if (translation.error != null) {
                Text(stringResource(R.string.reader_translation_error), color = MaterialTheme.colorScheme.error)
            }
            if (translation.hasAutomaticTranslation) {
                Text(
                    stringResource(R.string.reader_translation_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ContinuousPinyin(numberedPinyin: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    ) {
        PinyinText(
            numbered = numberedPinyin,
            fontSize = 17.sp,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun TranslationLine(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReaderTokenCard(
    analyzed: AnalyzedToken,
    definitionLanguage: DefinitionLanguage,
    showPinyin: Boolean,
    showDefinitions: Boolean,
    onClick: () -> Unit,
) {
    val entry = analyzed.entry
    Surface(
        onClick = onClick,
        enabled = entry != null,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            HanziText(
                hanzi = analyzed.token.text,
                numberedPinyin = entry?.pinyin.orEmpty(),
                fontSize = 28.sp,
                bold = true,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (entry == null) {
                    Text(stringResource(R.string.search_unknown_block), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    if (showPinyin) PinyinText(entry.pinyin, fontSize = 15.sp, bold = true)
                    if (showDefinitions) {
                        when (definitionLanguage) {
                            DefinitionLanguage.FRENCH -> entry.definitionsFrench.firstOrNull()?.let { Text(it) }
                            DefinitionLanguage.ENGLISH -> entry.definitionsEnglish.firstOrNull()?.let { Text(it) }
                            DefinitionLanguage.BOTH -> {
                                entry.definitionsFrench.firstOrNull()?.let { Text("FR · $it") }
                                entry.definitionsEnglish.firstOrNull()?.let {
                                    Text("EN · $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
