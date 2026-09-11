package fr.kairossolum.pangmao.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.R

@Composable
fun EntryRow(
    entry: DictionaryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = entry.displayHeadword,
                fontSize = 27.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            entry.alternateHeadword?.let {
                Text(text = it, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PinyinText(entry.pinyin, fontSize = 16.sp)
        }
        val definition = entry.definitionsFrench.firstOrNull()
            ?: entry.definitionsEnglish.firstOrNull()
            ?: "—"
        Text(text = definition, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
fun DefinitionList(title: String, definitions: List<String>, modifier: Modifier = Modifier) {
    if (definitions.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        definitions.forEachIndexed { index, definition ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${index + 1}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = definition, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun QuickEntryCard(entry: DictionaryEntry, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 2.dp) {
        Column(
            modifier = Modifier
                .clickable(onClick = onOpen)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                Text(entry.displayHeadword, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                entry.alternateHeadword?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            PinyinText(entry.pinyin, fontSize = 18.sp, bold = true)
            Text(entry.definitionsFrench.firstOrNull() ?: entry.definitionsEnglish.firstOrNull().orEmpty())
            Text(
                stringResource(R.string.open_full_entry),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
