package fr.kairossolum.pangmao.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.domain.model.WordKnowledgeStatus

@Composable
fun WordKnowledgeSelector(
    status: WordKnowledgeStatus,
    onSelect: (WordKnowledgeStatus) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.knowledge_progress),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
            ) {
                Text(statusLabel(status))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                WordKnowledgeStatus.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(statusLabel(option)) },
                        onClick = {
                            expanded = false
                            if (option != status) onSelect(option)
                        },
                        leadingIcon = if (option == status) {
                            { Icon(Icons.Outlined.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: WordKnowledgeStatus): String = stringResource(
    when (status) {
        WordKnowledgeStatus.UNMARKED -> R.string.knowledge_unmarked
        WordKnowledgeStatus.LEARNING -> R.string.knowledge_learning
        WordKnowledgeStatus.KNOWN -> R.string.knowledge_known
    }
)
