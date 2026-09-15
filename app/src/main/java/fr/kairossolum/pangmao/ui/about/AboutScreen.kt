package fr.kairossolum.pangmao.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.BuildConfig
import fr.kairossolum.pangmao.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(viewModel: AboutViewModel, onBack: () -> Unit) {
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.about_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("胖猫", fontSize = 52.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyLarge,
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Lock, contentDescription = null)
                    Column {
                        Text(stringResource(R.string.privacy_title), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.privacy_body))
                    }
                }
            }

            if (metadata.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(stringResource(R.string.offline_content), fontWeight = FontWeight.Bold)
                        Metric(stringResource(R.string.entries), metadata["entry_count"])
                        Metric(stringResource(R.string.examples), metadata["example_count"])
                        Metric(stringResource(R.string.characters), metadata["character_count"])
                        Metric(stringResource(R.string.stroke_characters), metadata["stroke_character_count"])
                        Metric("Français → 中文", metadata["learning_entry_count_fr"])
                        Metric("English → 中文", metadata["learning_entry_count_en"])
                        Metric("Unihan", metadata["unihan_version"])
                    }
                }
            }

            Text(stringResource(R.string.sources), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LicenseItem(
                "CC-CEDICT",
                "Données chinois–anglais et pinyin · CC BY-SA 4.0",
                "https://www.mdbg.net/chinese/dictionary?page=cc-cedict",
                uriHandler::openUri,
            )
            LicenseItem(
                "CFDICT / Chine Informations",
                "Définitions chinoises–françaises · CC BY-SA",
                "https://chine.in/mandarin/dictionnaire/CFDICT/",
                uriHandler::openUri,
            )
            LicenseItem(
                "Tatoeba",
                "Paires de phrases mandarin–anglais · CC BY 2.0 FR",
                "https://tatoeba.org/",
                uriHandler::openUri,
            )
            LicenseItem(
                "Unicode Unihan 17.0",
                "Lectures et propriétés des caractères · Unicode License v3",
                "https://www.unicode.org/license.txt",
                uriHandler::openUri,
            )
            LicenseItem(
                "Hanzi Writer Data / Make Me a Hanzi",
                stringResource(R.string.stroke_source_license),
                "https://github.com/chanind/hanzi-writer-data",
                uriHandler::openUri,
            )
            LicenseItem(
                "FreeDict / WikDict · français–chinois",
                "Vedettes, IPA, grammaire et sens chinois · CC BY-SA 3.0",
                "https://download.freedict.org/dictionaries/fra-zho/",
                uriHandler::openUri,
            )
            LicenseItem(
                "FreeDict / WikDict · anglais–chinois",
                "Vedettes, prononciations, grammaire et sens chinois · CC BY-SA 3.0",
                "https://download.freedict.org/dictionaries/eng-zho/",
                uriHandler::openUri,
            )
            LicenseItem(
                "Google ML Kit",
                "Modèle chinois embarqué pour la reconnaissance de texte locale",
                "https://developers.google.com/ml-kit/terms",
                uriHandler::openUri,
            )

            Text(stringResource(R.string.independence), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.independence_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Metric(label: String, value: String?) {
    if (value == null) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LicenseItem(name: String, detail: String, url: String, open: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open(url) }
            .padding(vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(detail, style = MaterialTheme.typography.bodySmall)
    }
}
