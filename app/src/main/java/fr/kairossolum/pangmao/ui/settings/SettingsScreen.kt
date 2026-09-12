package fr.kairossolum.pangmao.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.data.settings.AppLanguage
import fr.kairossolum.pangmao.data.settings.DefinitionLanguage
import fr.kairossolum.pangmao.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                }
            },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionTitle(R.string.settings_language)
            LanguageRow(R.string.settings_language_system, settings.language == AppLanguage.SYSTEM) {
                viewModel.setLanguage(AppLanguage.SYSTEM)
            }
            LanguageRow(R.string.settings_language_french, settings.language == AppLanguage.FRENCH) {
                viewModel.setLanguage(AppLanguage.FRENCH)
            }
            LanguageRow(R.string.settings_language_english, settings.language == AppLanguage.ENGLISH) {
                viewModel.setLanguage(AppLanguage.ENGLISH)
            }
            LanguageRow(R.string.settings_language_chinese, settings.language == AppLanguage.CHINESE) {
                viewModel.setLanguage(AppLanguage.CHINESE)
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionTitle(R.string.settings_definitions)
            LanguageRow(R.string.settings_definitions_french, settings.definitionLanguage == DefinitionLanguage.FRENCH) {
                viewModel.setDefinitionLanguage(DefinitionLanguage.FRENCH)
            }
            LanguageRow(R.string.settings_definitions_english, settings.definitionLanguage == DefinitionLanguage.ENGLISH) {
                viewModel.setDefinitionLanguage(DefinitionLanguage.ENGLISH)
            }
            LanguageRow(R.string.settings_definitions_both, settings.definitionLanguage == DefinitionLanguage.BOTH) {
                viewModel.setDefinitionLanguage(DefinitionLanguage.BOTH)
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionTitle(R.string.settings_theme)
            LanguageRow(R.string.settings_theme_system, settings.themeMode == ThemeMode.SYSTEM) {
                viewModel.setTheme(ThemeMode.SYSTEM)
            }
            LanguageRow(R.string.settings_theme_light, settings.themeMode == ThemeMode.LIGHT) {
                viewModel.setTheme(ThemeMode.LIGHT)
            }
            LanguageRow(R.string.settings_theme_dark, settings.themeMode == ThemeMode.DARK) {
                viewModel.setTheme(ThemeMode.DARK)
            }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(
        stringResource(title),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LanguageRow(@StringRes label: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(stringResource(label), modifier = Modifier.padding(vertical = 15.dp))
    }
}
