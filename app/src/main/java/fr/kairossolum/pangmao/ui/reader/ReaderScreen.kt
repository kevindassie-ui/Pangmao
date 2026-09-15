package fr.kairossolum.pangmao.ui.reader

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.data.settings.DefinitionLanguage
import fr.kairossolum.pangmao.data.settings.SpeechRate
import fr.kairossolum.pangmao.data.settings.closestSpeechRate
import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.ReadingCoverage
import fr.kairossolum.pangmao.domain.ReadingDifficultyBand
import fr.kairossolum.pangmao.domain.codePointAtDisplayOffset
import fr.kairossolum.pangmao.domain.model.WordKnowledgeStatus
import fr.kairossolum.pangmao.domain.containsHan
import fr.kairossolum.pangmao.domain.numberedPinyinReading
import fr.kairossolum.pangmao.domain.tokenAtDisplayOffset
import fr.kairossolum.pangmao.domain.speech.SpeechSegment
import fr.kairossolum.pangmao.domain.speech.SpeechSourceRange
import fr.kairossolum.pangmao.domain.speech.buildSpeechDocument
import fr.kairossolum.pangmao.ui.common.coloredPinyin
import fr.kairossolum.pangmao.ui.common.LocalDefinitionLanguage
import fr.kairossolum.pangmao.ui.common.EntryRow
import fr.kairossolum.pangmao.ui.common.HanziText
import fr.kairossolum.pangmao.ui.common.PinyinText
import fr.kairossolum.pangmao.ui.common.QuickEntryCard
import fr.kairossolum.pangmao.ui.common.TextTranslationState
import fr.kairossolum.pangmao.ui.common.WordKnowledgeSelector
import fr.kairossolum.pangmao.ui.common.rememberMandarinSpeaker
import fr.kairossolum.pangmao.ui.common.SpeakerPlaybackState
import fr.kairossolum.pangmao.ui.common.SpeakerStatus
import fr.kairossolum.pangmao.ui.common.SpeakerVoiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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
    val selectionLookup by viewModel.selectionLookup.collectAsStateWithLifecycle()
    val selectedFlashcard by viewModel.selectedFlashcard.collectAsStateWithLifecycle()
    val selectedWordKnowledge by viewModel.selectedWordKnowledge.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val readingCoverage by viewModel.readingCoverage.collectAsStateWithLifecycle()
    val wordKnowledgeByEntryId by viewModel.wordKnowledgeByEntryId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val speaker = rememberMandarinSpeaker()
    val voiceSample = stringResource(R.string.tts_test_sample)
    val copiedMessage = stringResource(R.string.reader_copied)
    val definitionLanguage = LocalDefinitionLanguage.current
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var showCoverageDetails by rememberSaveable { mutableStateOf(false) }
    var highlightReviewWords by rememberSaveable { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                content?.let { loaded ->
                    isEditing = false
                    viewModel.setText(loaded)
                }
            }
        }
    }
    var showPinyin by rememberSaveable { mutableStateOf(true) }
    var showTranslation by rememberSaveable { mutableStateOf(true) }
    var showDefinitions by rememberSaveable { mutableStateOf(true) }
    var revealPinyinAtTop by rememberSaveable { mutableStateOf(false) }
    var showVoiceDetails by rememberSaveable { mutableStateOf(false) }
    var editorValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text))
    }
    val readingListState = rememberLazyListState()
    val speechDocument = remember(text) { buildSpeechDocument(text) }
    var selectedSentenceIndex by remember(text) { mutableIntStateOf(0) }
    val activePlayback = speaker.playbackModel.takeIf { speaker.playbackSource == text }
    val activeSegmentIndex = activePlayback?.position?.segmentIndex
    val activeSegment = activeSegmentIndex?.let { speechDocument.segments.getOrNull(it) }
    val selectedSegment = speechDocument.segments.getOrNull(selectedSentenceIndex)
    val highlightedSegment = activeSegment ?: selectedSegment
    val showSentenceNavigator = !isEditing && speaker.ready && speechDocument.segments.size > 1
    val showTtsError = speaker.status == SpeakerStatus.MISSING_CHINESE_VOICE ||
        speaker.status == SpeakerStatus.ERROR
    val showSpeechRate = !isEditing && speaker.ready && text.isNotBlank()
    val controlsBeforePinyin = 1 +
        (if (showSentenceNavigator) 1 else 0) +
        (if (error != null) 1 else 0) +
        (if (showTtsError) 1 else 0) +
        (if (showSpeechRate) 1 else 0)
    val sentenceHighlight = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
    val spokenHighlight = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f)
    val speechTransformation = remember(
        highlightedSegment?.sourceStart,
        highlightedSegment?.sourceEndExclusive,
        activePlayback?.spokenRange,
        sentenceHighlight,
        spokenHighlight,
    ) {
        SpeechHighlightTransformation(
            activeSegment = highlightedSegment,
            spokenRange = activePlayback?.spokenRange,
            sentenceColor = sentenceHighlight,
            spokenColor = spokenHighlight,
        )
    }
    val selectedText = editorValue.selectedTextOrNull().takeIf { isEditing }
    val copyText: (String) -> Unit = { value ->
        if (value.isNotBlank()) {
            clipboard.setText(AnnotatedString(value))
            Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
        }
    }
    val selectSentence: (Int) -> Unit = { index ->
        if (index in speechDocument.segments.indices) {
            selectedSentenceIndex = index
            when (speaker.playbackState) {
                SpeakerPlaybackState.PLAYING -> speaker.speakFrom(text, index)
                SpeakerPlaybackState.PAUSED -> speaker.stop()
                SpeakerPlaybackState.IDLE -> Unit
            }
        }
    }
    val defineAtOffset: (Int) -> Unit = { offset ->
        val analyzed = analysis?.tokenAtDisplayOffset(text, offset)
        if (analyzed?.entry != null) {
            viewModel.select(analyzed)
        } else {
            val candidate = analyzed?.token?.text
                ?.takeIf { analyzed.token.isChinese && containsHan(it) }
                ?: text.codePointAtDisplayOffset(offset)?.takeIf(::containsHan)
            candidate?.let(viewModel::defineSelection)
        }
    }

    LaunchedEffect(revealPinyinAtTop, controlsBeforePinyin) {
        if (revealPinyinAtTop) {
            readingListState.animateScrollToItem(controlsBeforePinyin)
            revealPinyinAtTop = false
        }
    }

    LaunchedEffect(text) {
        if (editorValue.text != text) {
            editorValue = TextFieldValue(text, selection = androidx.compose.ui.text.TextRange(text.length))
        }
        speaker.stop()
    }

    LaunchedEffect(activeSegmentIndex) {
        activeSegmentIndex?.let { index ->
            if (index in speechDocument.segments.indices) selectedSentenceIndex = index
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.reader_title), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.reader_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        when (speaker.playbackState) {
                            SpeakerPlaybackState.IDLE -> speaker.speakFrom(
                                text,
                                speechDocument.segments.getOrNull(selectedSentenceIndex)?.index ?: 0,
                            )
                            SpeakerPlaybackState.PLAYING -> speaker.pause()
                            SpeakerPlaybackState.PAUSED -> speaker.resume()
                        }
                    },
                    enabled = !isEditing && speaker.ready && text.isNotBlank(),
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
                    IconButton(onClick = { speaker.restartSpeech(text) }) {
                        Icon(
                            Icons.Outlined.Replay,
                            contentDescription = stringResource(R.string.tts_restart),
                        )
                    }
                    IconButton(onClick = speaker::stop) {
                        Icon(
                            Icons.Outlined.Stop,
                            contentDescription = stringResource(R.string.tts_stop),
                        )
                    }
                }
            },
        )
        Column(Modifier.padding(horizontal = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.reader_field),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        if (isEditing) {
                            isEditing = false
                            viewModel.setText(editorValue.text)
                        } else {
                            speaker.stop()
                            editorValue = TextFieldValue(
                                text,
                                selection = androidx.compose.ui.text.TextRange(text.length),
                            )
                            isEditing = true
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Icon(
                        if (isEditing) Icons.Outlined.Check else Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        " ${stringResource(if (isEditing) R.string.reader_finish_editing else R.string.reader_edit)}"
                    )
                }
            }
            if (isEditing) {
                OutlinedTextField(
                    value = editorValue,
                    onValueChange = { updated -> editorValue = updated },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp, max = 135.dp),
                    placeholder = { Text(stringResource(R.string.reader_placeholder)) },
                    trailingIcon = {
                        if (editorValue.text.isNotEmpty()) {
                            IconButton(onClick = { editorValue = TextFieldValue("") }) {
                                Icon(
                                    Icons.Outlined.Clear,
                                    contentDescription = stringResource(R.string.clear),
                                )
                            }
                        }
                    },
                )
            } else {
                ReaderTextField(
                    text = text,
                    transformedText = speechTransformation.filter(AnnotatedString(text)).text,
                    highlightedOffset = highlightedSegment?.sourceStart,
                    placeholder = stringResource(R.string.reader_placeholder),
                    onSentenceTap = { offset ->
                        speechDocument.segmentAtSourceOffset(offset)?.let { segment ->
                            selectSentence(segment.index)
                        }
                    },
                    onWordLongPress = defineAtOffset,
                    onClear = { viewModel.setText("") },
                )
            }
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
        LazyColumn(
            state = readingListState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "input-actions") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = { filePicker.launch(arrayOf("text/plain", "text/*")) }) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                            Text(" ${stringResource(R.string.open)}")
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.getText()?.text?.let { pasted ->
                                    isEditing = false
                                    viewModel.setText(pasted)
                                }
                            },
                        ) {
                            Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                            Text(" ${stringResource(R.string.paste)}")
                        }
                    }
                    if (text.isNotBlank() || (selectedText != null && containsHan(selectedText))) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            if (text.isNotBlank()) {
                                TextButton(onClick = { copyText(text) }) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                                    Text(" ${stringResource(R.string.reader_copy_all)}")
                                }
                            }
                            if (selectedText != null && containsHan(selectedText)) {
                                TextButton(onClick = { viewModel.defineSelection(selectedText) }) {
                                    Icon(Icons.Outlined.Search, contentDescription = null)
                                    Text(" ${stringResource(R.string.reader_define_selection)}")
                                }
                            }
                        }
                    }
                }
            }
            if (showSentenceNavigator) {
                item(key = "sentence-navigator") {
                    SentenceNavigator(
                        segments = speechDocument.segments,
                        selectedSegmentIndex = selectedSentenceIndex,
                        activeSegmentIndex = activeSegmentIndex,
                        onSelect = selectSentence,
                    )
                }
            }
            error?.let { message ->
                item(key = "reader-error") {
                    Text(
                        message,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (showTtsError) {
                item(key = "tts-error") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(
                                if (speaker.status == SpeakerStatus.MISSING_CHINESE_VOICE) {
                                    R.string.tts_voice_missing
                                } else {
                                    R.string.tts_unavailable
                                }
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
            }
            if (showSpeechRate) {
                item(key = "speech-rate") {
                    SpeechRateSelector(
                        selected = speechRate,
                        onSelect = viewModel::setSpeechRate,
                        onTestVoice = { speaker.previewVoice(voiceSample) },
                        onShowVoiceDetails = { showVoiceDetails = true },
                        voiceDetailsAvailable = speaker.voiceInfo != null,
                    )
                }
            }
            when {
                isEditing -> item(key = "editing-hint") {
                    Text(
                        stringResource(R.string.reader_finish_editing_hint),
                        modifier = Modifier.padding(4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                isAnalyzing -> item(key = "analyzing") {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                analysis == null -> item(key = "empty") {
                    Text(
                        stringResource(R.string.reader_empty),
                        modifier = Modifier.padding(4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    val currentAnalysis = checkNotNull(analysis)
                    if (showPinyin) {
                        item(key = "continuous-pinyin") {
                            ContinuousPinyin(
                                numberedPinyin = currentAnalysis.numberedPinyinReading(),
                                onCopy = copyText,
                            )
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
                                onCopy = copyText,
                            )
                        }
                    }
                    readingCoverage?.let { coverage ->
                        item(key = "reading-coverage") {
                            ReadingCoverageSummary(
                                coverage = coverage,
                                onClick = { showCoverageDetails = true },
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
                            knowledgeStatus = token.token.entryId
                                ?.let(wordKnowledgeByEntryId::get)
                                ?: WordKnowledgeStatus.UNMARKED,
                            highlightForReview = highlightReviewWords,
                            onClick = { viewModel.select(token) },
                            onCopy = copyText,
                        )
                    }
                }
            }
        }
    }

    selected?.let { entry ->
        ModalBottomSheet(onDismissRequest = viewModel::dismissSelection) {
            Column(Modifier.padding(bottom = 24.dp)) {
                QuickEntryCard(
                    entry = entry,
                    onOpen = {
                        viewModel.dismissSelection()
                        onOpenEntry(entry.id)
                    },
                )
                val knowledgeReady = selectedWordKnowledge.isReady &&
                    selectedWordKnowledge.entryId == entry.id
                WordKnowledgeSelector(
                    status = selectedWordKnowledge.status,
                    onSelect = viewModel::setSelectedWordKnowledgeStatus,
                    enabled = knowledgeReady,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    EntryCopyMenu(
                        hanzi = entry.displayHeadword,
                        numberedPinyin = entry.pinyin,
                        french = entry.definitionsFrench.firstOrNull(),
                        english = entry.definitionsEnglish.firstOrNull(),
                        definitionLanguage = definitionLanguage,
                        iconOnly = false,
                        onCopy = copyText,
                    )
                }
                val flashcardReady = selectedFlashcard.isReady &&
                    selectedFlashcard.entryId == entry.id
                val isSelectedFlashcard = flashcardReady && selectedFlashcard.isFlashcard
                Button(
                    onClick = viewModel::toggleSelectedFlashcard,
                    enabled = flashcardReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Icon(
                        if (isSelectedFlashcard) Icons.Filled.Bookmark
                        else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                    )
                    Text(
                        stringResource(
                            if (isSelectedFlashcard) R.string.card_remove
                            else R.string.card_add
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }

    selectionLookup?.let { lookup ->
        ModalBottomSheet(onDismissRequest = viewModel::dismissSelectionLookup) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item(key = "selection-title") {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.reader_selection_results),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            lookup.query,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (lookup.entries.isEmpty()) {
                    item(key = "selection-empty") {
                        Text(
                            stringResource(R.string.reader_selection_none),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    itemsIndexed(
                        lookup.entries,
                        key = { _, entry -> entry.id },
                    ) { _, entry ->
                        EntryRow(
                            entry = entry,
                            onClick = {
                                viewModel.dismissSelectionLookup()
                                onOpenEntry(entry.id)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showVoiceDetails) {
        ModalBottomSheet(onDismissRequest = { showVoiceDetails = false }) {
            SpeakerVoiceDetails(
                info = speaker.voiceInfo,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }

    readingCoverage?.takeIf { showCoverageDetails }?.let { coverage ->
        ModalBottomSheet(onDismissRequest = { showCoverageDetails = false }) {
            ReadingCoverageDetails(
                coverage = coverage,
                highlightForReview = highlightReviewWords,
                onHighlightChange = { highlightReviewWords = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ReaderTextField(
    text: String,
    transformedText: AnnotatedString,
    highlightedOffset: Int?,
    placeholder: String,
    onSentenceTap: (Int) -> Unit,
    onWordLongPress: (Int) -> Unit,
    onClear: () -> Unit,
) {
    var layoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val scrollState = rememberScrollState()
    val displayText = if (text.isEmpty()) AnnotatedString(placeholder) else transformedText

    LaunchedEffect(text) {
        scrollState.scrollTo(0)
    }
    LaunchedEffect(highlightedOffset, layoutResult) {
        val layout = layoutResult
        val offset = highlightedOffset
        if (layout != null && offset != null && offset in text.indices) {
            val line = layout.getLineForOffset(offset)
            scrollState.animateScrollTo(layout.getLineTop(line).roundToInt())
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp, max = 135.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = Color.Transparent,
    ) {
        Box {
            Text(
                text = displayText,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(
                        start = 16.dp,
                        top = 14.dp,
                        end = if (text.isEmpty()) 16.dp else 52.dp,
                        bottom = 14.dp,
                    )
                    .pointerInput(text, onSentenceTap, onWordLongPress) {
                        if (text.isNotEmpty()) {
                            detectTapGestures(
                                onTap = { position ->
                                    layoutResult?.getOffsetForPosition(position)?.let(onSentenceTap)
                                },
                                onLongPress = { position ->
                                    layoutResult?.getOffsetForPosition(position)?.let(onWordLongPress)
                                },
                            )
                        }
                    },
                color = if (text.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp, lineHeight = 34.sp),
                onTextLayout = { layoutResult = it },
            )
            if (text.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.clear))
                }
            }
        }
    }
}

@Composable
private fun SpeechRateSelector(
    selected: SpeechRate,
    onSelect: (SpeechRate) -> Unit,
    onTestVoice: () -> Unit,
    onShowVoiceDetails: () -> Unit,
    voiceDetailsAvailable: Boolean,
) {
    var sliderValue by remember(selected) { mutableStateOf(selected.multiplier) }
    var menuExpanded by remember { mutableStateOf(false) }
    val previewRate = closestSpeechRate(sliderValue)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${stringResource(R.string.tts_speed)} · ${previewRate.label}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                val chosenRate = closestSpeechRate(sliderValue)
                sliderValue = chosenRate.multiplier
                if (chosenRate != selected) onSelect(chosenRate)
            },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            valueRange = SpeechRate.VERY_SLOW.multiplier..SpeechRate.FAST.multiplier,
            steps = SpeechRate.entries.size - 2,
        )
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.tts_more_actions),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tts_test_voice)) },
                    onClick = {
                        menuExpanded = false
                        onTestVoice()
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tts_voice_details)) },
                    onClick = {
                        menuExpanded = false
                        onShowVoiceDetails()
                    },
                    enabled = voiceDetailsAvailable,
                    leadingIcon = {
                        Icon(Icons.Outlined.Info, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
private fun SpeakerVoiceDetails(
    info: SpeakerVoiceInfo?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.tts_voice_details_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (info == null) {
            Text(
                stringResource(R.string.tts_voice_details_unavailable),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            VoiceDetailRow(
                label = stringResource(R.string.tts_voice_engine),
                value = info.enginePackage ?: stringResource(R.string.tts_voice_unknown),
            )
            VoiceDetailRow(
                label = stringResource(R.string.tts_voice_name),
                value = info.voiceName ?: stringResource(R.string.tts_voice_unknown),
            )
            VoiceDetailRow(
                label = stringResource(R.string.tts_voice_locale),
                value = info.localeTag ?: stringResource(R.string.tts_voice_unknown),
            )
            VoiceDetailRow(
                label = stringResource(R.string.tts_voice_connection),
                value = when (info.requiresNetwork) {
                    false -> stringResource(R.string.tts_voice_offline)
                    true -> stringResource(R.string.tts_voice_network)
                    null -> stringResource(R.string.tts_voice_unknown)
                },
            )
            Text(
                stringResource(R.string.tts_voice_details_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VoiceDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SentenceNavigator(
    segments: List<SpeechSegment>,
    selectedSegmentIndex: Int,
    activeSegmentIndex: Int?,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayedIndex = (activeSegmentIndex ?: selectedSegmentIndex).coerceIn(segments.indices)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.reader_start_from_sentence),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onSelect(displayedIndex - 1) },
                enabled = displayedIndex > 0,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.reader_previous_sentence),
                )
            }
            Box(Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "${displayedIndex + 1}/${segments.size} · " +
                            segments[displayedIndex].text.singleLinePreview(maxCodePoints = 22),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.heightIn(max = 360.dp),
                ) {
                    segments.forEach { segment ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${segment.index + 1} · ${segment.text.singleLinePreview(36)}",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            onClick = {
                                expanded = false
                                onSelect(segment.index)
                            },
                            trailingIcon = {
                                if (segment.index == displayedIndex) {
                                    Icon(Icons.Outlined.Check, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            }
            IconButton(
                onClick = { onSelect(displayedIndex + 1) },
                enabled = displayedIndex < segments.lastIndex,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = stringResource(R.string.reader_next_sentence),
                )
            }
        }
    }
}

@Composable
private fun ReaderTranslationCard(
    translation: TextTranslationState,
    onDownload: () -> Unit,
    onCopy: (String) -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            translation.french?.let {
                TranslationLine(stringResource(R.string.french), it, onCopy)
            }
            translation.english?.let {
                val title = if (translation.englishIsAttested) {
                    stringResource(R.string.reader_english_attested)
                } else {
                    stringResource(R.string.english)
                }
                TranslationLine(title, it, onCopy)
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
private fun ContinuousPinyin(
    numberedPinyin: String,
    onCopy: (String) -> Unit,
) {
    val markedPinyin = remember(numberedPinyin) { coloredPinyin(numberedPinyin).text }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PinyinText(
                numbered = numberedPinyin,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f).padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
            )
            IconButton(onClick = { onCopy(markedPinyin) }) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.reader_copy_pinyin),
                )
            }
        }
    }
}

@Composable
private fun TranslationLine(
    title: String,
    text: String,
    onCopy: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
        IconButton(onClick = { onCopy(text) }) {
            Icon(
                Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.reader_copy_translation, title),
            )
        }
    }
}

@Composable
private fun ReadingCoverageSummary(
    coverage: ReadingCoverage,
    onClick: () -> Unit,
) {
    val classifiedDistinctWords = coverage.knownDistinctWords + coverage.learningDistinctWords
    val summary = if (coverage.difficulty == ReadingDifficultyBand.UNRATED) {
        stringResource(
            R.string.reader_coverage_profile_pending,
            classifiedDistinctWords,
            coverage.distinctRecognizedWords,
        )
    } else {
        stringResource(
            R.string.reader_coverage_summary,
            (coverage.knownCoverageRatio * 100).roundToInt(),
            difficultyLabel(coverage.difficulty),
        )
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.38f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.reader_coverage_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Icon(
                Icons.Outlined.Info,
                contentDescription = stringResource(R.string.reader_coverage_details),
            )
        }
    }
}

@Composable
private fun ReadingCoverageDetails(
    coverage: ReadingCoverage,
    highlightForReview: Boolean,
    onHighlightChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.reader_coverage_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        CoverageMetric(
            stringResource(R.string.reader_coverage_recognized),
            coverage.recognizedOccurrences.toString(),
        )
        CoverageMetric(
            stringResource(R.string.reader_coverage_distinct),
            coverage.distinctRecognizedWords.toString(),
        )
        CoverageMetric(
            stringResource(R.string.reader_coverage_known),
            stringResource(
                R.string.reader_coverage_occurrence_words,
                coverage.knownOccurrences,
                coverage.knownDistinctWords,
            ),
        )
        CoverageMetric(
            stringResource(R.string.reader_coverage_learning),
            stringResource(
                R.string.reader_coverage_occurrence_words,
                coverage.learningOccurrences,
                coverage.learningDistinctWords,
            ),
        )
        CoverageMetric(
            stringResource(R.string.reader_coverage_unmarked),
            stringResource(
                R.string.reader_coverage_occurrence_words,
                coverage.unmarkedOccurrences,
                coverage.unmarkedDistinctWords,
            ),
        )
        CoverageMetric(
            stringResource(R.string.reader_coverage_unknown),
            coverage.unknownChineseBlocks.toString(),
        )
        Text(
            stringResource(
                if (coverage.difficulty == ReadingDifficultyBand.UNRATED) {
                    R.string.reader_coverage_pending_help
                } else {
                    R.string.reader_coverage_method
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.reader_coverage_highlight),
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Switch(checked = highlightForReview, onCheckedChange = onHighlightChange)
        }
    }
}

@Composable
private fun CoverageMetric(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun difficultyLabel(difficulty: ReadingDifficultyBand): String = stringResource(
    when (difficulty) {
        ReadingDifficultyBand.UNRATED -> R.string.reader_coverage_unrated
        ReadingDifficultyBand.ACCESSIBLE -> R.string.reader_coverage_accessible
        ReadingDifficultyBand.STIMULATING -> R.string.reader_coverage_stimulating
        ReadingDifficultyBand.DENSE -> R.string.reader_coverage_dense
    }
)

@Composable
private fun ReaderTokenCard(
    analyzed: AnalyzedToken,
    definitionLanguage: DefinitionLanguage,
    showPinyin: Boolean,
    showDefinitions: Boolean,
    knowledgeStatus: WordKnowledgeStatus,
    highlightForReview: Boolean,
    onClick: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val entry = analyzed.entry
    val needsReview = entry == null || knowledgeStatus != WordKnowledgeStatus.KNOWN
    Surface(
        onClick = onClick,
        enabled = entry != null,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (highlightForReview && needsReview) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
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
            EntryCopyMenu(
                hanzi = analyzed.token.text,
                numberedPinyin = entry?.pinyin,
                french = entry?.definitionsFrench?.firstOrNull(),
                english = entry?.definitionsEnglish?.firstOrNull(),
                definitionLanguage = definitionLanguage,
                iconOnly = true,
                onCopy = onCopy,
            )
        }
    }
}

@Composable
private fun EntryCopyMenu(
    hanzi: String,
    numberedPinyin: String?,
    french: String?,
    english: String?,
    definitionLanguage: DefinitionLanguage,
    iconOnly: Boolean,
    onCopy: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        if (iconOnly) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.reader_copy_word_data),
                )
            }
        } else {
            TextButton(onClick = { expanded = true }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                Text(" ${stringResource(R.string.copy)}")
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CopyMenuItem(
                label = stringResource(R.string.reader_copy_hanzi),
                value = hanzi,
                close = { expanded = false },
                onCopy = onCopy,
            )
            numberedPinyin
                ?.takeIf(String::isNotBlank)
                ?.let { numbered ->
                    CopyMenuItem(
                        label = stringResource(R.string.reader_copy_pinyin),
                        value = coloredPinyin(numbered).text,
                        close = { expanded = false },
                        onCopy = onCopy,
                    )
                }
            if (definitionLanguage != DefinitionLanguage.ENGLISH) {
                french?.takeIf(String::isNotBlank)?.let { definition ->
                    CopyMenuItem(
                        label = stringResource(R.string.reader_copy_french_definition),
                        value = definition,
                        close = { expanded = false },
                        onCopy = onCopy,
                    )
                }
            }
            if (definitionLanguage != DefinitionLanguage.FRENCH) {
                english?.takeIf(String::isNotBlank)?.let { definition ->
                    CopyMenuItem(
                        label = stringResource(R.string.reader_copy_english_definition),
                        value = definition,
                        close = { expanded = false },
                        onCopy = onCopy,
                    )
                }
            }
        }
    }
}

@Composable
private fun CopyMenuItem(
    label: String,
    value: String,
    close: () -> Unit,
    onCopy: (String) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
        onClick = {
            close()
            onCopy(value)
        },
    )
}

private class SpeechHighlightTransformation(
    private val activeSegment: SpeechSegment?,
    private val spokenRange: SpeechSourceRange?,
    private val sentenceColor: Color,
    private val spokenColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val segment = activeSegment
        if (
            segment == null ||
            segment.sourceStart !in 0 until text.length ||
            segment.sourceEndExclusive !in 1..text.length
        ) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val styled = AnnotatedString.Builder(text)
        styled.addStyle(
            SpanStyle(background = sentenceColor),
            segment.sourceStart,
            segment.sourceEndExclusive,
        )
        spokenRange
            ?.takeIf {
                it.segmentIndex == segment.index &&
                    it.sourceStart >= segment.sourceStart &&
                    it.sourceEndExclusive <= segment.sourceEndExclusive
            }
            ?.let { range ->
                styled.addStyle(
                    SpanStyle(background = spokenColor),
                    range.sourceStart,
                    range.sourceEndExclusive,
                )
            }
        return TransformedText(styled.toAnnotatedString(), OffsetMapping.Identity)
    }
}

private fun String.singleLinePreview(maxCodePoints: Int = 14): String {
    val normalized = replace(whitespaceRuns, " ").trim()
    if (normalized.codePointCount(0, normalized.length) <= maxCodePoints) return normalized
    val end = normalized.offsetByCodePoints(0, maxCodePoints)
    return normalized.substring(0, end) + "…"
}

private val whitespaceRuns = Regex("\\s+")

private fun TextFieldValue.selectedTextOrNull(): String? {
    if (selection.collapsed) return null
    val start = selection.min.coerceIn(0, text.length)
    val end = selection.max.coerceIn(start, text.length)
    return text.substring(start, end).trim().takeIf(String::isNotEmpty)
}
