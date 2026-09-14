package fr.kairossolum.pangmao.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.data.user.StudyCard
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.domain.ReviewRating
import fr.kairossolum.pangmao.domain.model.WordKnowledgeStatus
import fr.kairossolum.pangmao.ui.common.EntryRow
import fr.kairossolum.pangmao.ui.common.PinyinText
import fr.kairossolum.pangmao.ui.common.HanziText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    viewModel: StudyViewModel,
    onOpenEntry: (Long) -> Unit,
) {
    val due by viewModel.dueCards.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val vocabulary by viewModel.vocabulary.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    var selectedCollection by rememberSaveable { mutableIntStateOf(0) }
    val learningWords = remember(vocabulary) {
        vocabulary.filter { value -> value.knowledge.status == WordKnowledgeStatus.LEARNING }
    }
    val knownWords = remember(vocabulary) {
        vocabulary.filter { value -> value.knowledge.status == WordKnowledgeStatus.KNOWN }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.cards_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.cards_subtitle), style = MaterialTheme.typography.labelSmall)
                }
            }
        )

        if (session.isActive || session.completedCount > 0) {
            ReviewPane(
                session = session,
                onReveal = viewModel::reveal,
                onRate = viewModel::rate,
                onClose = viewModel::closeReview,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                item(key = "collections") {
                    val labels = listOf(
                        stringResource(R.string.study_tab_cards, allCards.size),
                        stringResource(R.string.study_tab_learning, learningWords.size),
                        stringResource(R.string.study_tab_known, knownWords.size),
                        stringResource(R.string.study_tab_favorites, favorites.size),
                    )
                    ScrollableTabRow(
                        selectedTabIndex = selectedCollection,
                        edgePadding = 12.dp,
                        divider = {},
                    ) {
                        labels.forEachIndexed { index, label ->
                            Tab(
                                selected = selectedCollection == index,
                                onClick = { selectedCollection = index },
                                text = { Text(label, maxLines = 1) },
                            )
                        }
                    }
                }

                when (selectedCollection) {
                    0 -> {
                        item(key = "review-summary") {
                            ReviewSummary(
                                dueCount = due.size,
                                onStartReview = viewModel::startReview,
                            )
                        }
                        if (allCards.isNotEmpty()) {
                            item(key = "all-cards-title") {
                                ListTitle(stringResource(R.string.cards_all, allCards.size))
                            }
                            items(allCards, key = { "card-${it.entry.id}" }) { card ->
                                StudyCardRow(
                                    card = card,
                                    onOpen = { onOpenEntry(card.entry.id) },
                                    onRemove = { viewModel.remove(card.entry.id) },
                                )
                            }
                        }
                    }
                    1 -> {
                        if (learningWords.isEmpty()) {
                            item(key = "learning-empty") { EmptyCollection() }
                        } else {
                            items(learningWords, key = { "learning-${it.entry.id}" }) { value ->
                                EntryRow(value.entry, onClick = { onOpenEntry(value.entry.id) })
                            }
                        }
                    }
                    2 -> {
                        if (knownWords.isEmpty()) {
                            item(key = "known-empty") { EmptyCollection() }
                        } else {
                            items(knownWords, key = { "known-${it.entry.id}" }) { value ->
                                EntryRow(value.entry, onClick = { onOpenEntry(value.entry.id) })
                            }
                        }
                    }
                    else -> {
                        if (favorites.isEmpty()) {
                            item(key = "favorites-empty") { EmptyCollection() }
                        } else {
                            items(favorites, key = { "favorite-${it.id}" }) { entry ->
                                EntryRow(entry, onClick = { onOpenEntry(entry.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewSummary(dueCount: Int, onStartReview: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.School, contentDescription = null)
                Text(
                    if (dueCount == 0) stringResource(R.string.cards_up_to_date)
                    else stringResource(R.string.cards_due, dueCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(stringResource(if (dueCount == 0) R.string.cards_add_help else R.string.cards_review_help))
            Button(onClick = onStartReview, enabled = dueCount > 0) {
                Text(stringResource(R.string.cards_start))
            }
        }
    }
}

@Composable
private fun EmptyCollection() {
    Text(
        stringResource(R.string.study_collection_empty),
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ReviewPane(
    session: ReviewSession,
    onReveal: () -> Unit,
    onRate: (ReviewRating) -> Unit,
    onClose: () -> Unit,
) {
    val card = session.current
    if (card == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.review_done), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.review_count, session.completedCount))
                Button(onClick = onClose) { Text(stringResource(R.string.finish)) }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "${session.index + 1} / ${session.cards.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                HanziText(card.entry.displayHeadword, card.entry.pinyin, fontSize = 52.sp, bold = true)
                card.entry.alternateHeadword?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (session.revealed) {
                    PinyinText(card.entry.pinyin, fontSize = 22.sp, bold = true)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        card.entry.definitionsFrench.firstOrNull()
                            ?: card.entry.definitionsEnglish.firstOrNull().orEmpty(),
                        fontSize = 19.sp,
                    )
                } else {
                    OutlinedButton(onClick = onReveal) { Text(stringResource(R.string.show_answer)) }
                }
            }
        }
        if (session.revealed) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RatingButton(stringResource(R.string.rating_again), Color(0xFFB3261E), Modifier.weight(1f)) { onRate(ReviewRating.AGAIN) }
                RatingButton(stringResource(R.string.rating_hard), Color(0xFFC56A00), Modifier.weight(1f)) { onRate(ReviewRating.HARD) }
                RatingButton(stringResource(R.string.rating_good), Color(0xFF26733A), Modifier.weight(1f)) { onRate(ReviewRating.GOOD) }
                RatingButton(stringResource(R.string.rating_easy), Color(0xFF3267A8), Modifier.weight(1f)) { onRate(ReviewRating.EASY) }
            }
        } else {
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun RatingButton(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 3.dp),
    ) {
        Text(label, maxLines = 1, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun StudyCardRow(card: StudyCard, onOpen: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HanziText(card.entry.displayHeadword, card.entry.pinyin, fontSize = 24.sp, bold = true)
                PinyinText(card.entry.pinyin)
                Text(
                    stringResource(R.string.card_schedule, card.scheduling.intervalDays, card.scheduling.repetitions),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.card_delete))
            }
        }
    }
}

@Composable
private fun ListTitle(value: String) {
    Text(
        value,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}
