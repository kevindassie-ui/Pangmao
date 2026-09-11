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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(viewModel: AboutViewModel, onBack: () -> Unit) {
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("À propos et licences") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
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
            Text("Pangmao · MVP 0.1.0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Dictionnaire et compagnon d’apprentissage chinois, conçu pour un usage personnel, local et sans abonnement.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Lock, contentDescription = null)
                    Column {
                        Text("Vie privée par conception", fontWeight = FontWeight.Bold)
                        Text("Aucune permission Internet. Recherches, textes, images, OCR, historique et cartes restent sur cet appareil.")
                    }
                }
            }

            if (metadata.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Contenu hors ligne", fontWeight = FontWeight.Bold)
                        Metric("Entrées", metadata["entry_count"])
                        Metric("Exemples", metadata["example_count"])
                        Metric("Caractères", metadata["character_count"])
                        Metric("Unihan", metadata["unihan_version"])
                    }
                }
            }

            Text("Sources linguistiques", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                "Google ML Kit",
                "Modèle chinois embarqué pour la reconnaissance de texte locale",
                "https://developers.google.com/ml-kit/terms",
                uriHandler::openUri,
            )

            Text("Indépendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Pangmao est un projet indépendant. Il n’est ni affilié à Pleco Software, ni approuvé par celle-ci. Il ne contient aucun dictionnaire, modèle, code, marque graphique ou contenu propriétaire de Pleco.",
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

