package fr.kairossolum.pangmao.ui.ocr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.ui.common.QuickEntryCard
import fr.kairossolum.pangmao.ui.common.TokenizedText

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun OcrScreen(
    viewModel: OcrViewModel,
    onOpenEntry: (Long) -> Unit,
) {
    val context = LocalContext.current
    val recognizedText by viewModel.recognizedText.collectAsStateWithLifecycle()
    val tokens by viewModel.tokens.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val liveEnabled by viewModel.liveEnabled.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val selected by viewModel.selectedEntry.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraPermission = it
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.recognizeImage(context, it) }
    }
    val analyzer = remember(viewModel) { ImageAnalysis.Analyzer { image -> viewModel.analyze(image) } }

    LaunchedEffect(Unit) {
        if (!cameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("OCR chinois", fontWeight = FontWeight.Bold)
                    Text("Caméra et images · traitement local", style = MaterialTheme.typography.labelSmall)
                }
            },
            actions = {
                AssistChip(
                    onClick = {},
                    label = { Text("Hors ligne") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                )
            },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(285.dp)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black),
        ) {
            if (cameraPermission) {
                CameraPreview(
                    analyzer = analyzer,
                    modifier = Modifier.fillMaxSize(),
                    onCameraError = { cameraError = it },
                )
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color.White)
                    Text("Autorisez la caméra pour l’OCR en direct.", color = Color.White)
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Autoriser")
                    }
                }
            }
            cameraError?.let {
                Text(
                    it,
                    modifier = Modifier.align(Alignment.Center).padding(20.dp),
                    color = Color.White,
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.62f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::toggleLive) {
                        Icon(
                            if (liveEnabled) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (liveEnabled) "Figer" else "Reprendre",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Outlined.Image, contentDescription = "Choisir une image", tint = Color.White)
                    }
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(recognizedText)) },
                        enabled = recognizedText.isNotBlank(),
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copier", tint = Color.White)
                    }
                    IconButton(onClick = viewModel::clear, enabled = recognizedText.isNotBlank()) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Effacer", tint = Color.White)
                    }
                }
            }
        }

        if (isProcessing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 18.dp))
        error?.let {
            Text(
                it,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            if (recognizedText.isBlank()) "Cadrez du texte chinois ou choisissez une image." else "Touchez un mot reconnu pour afficher sa définition.",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (tokens.isNotEmpty()) {
            TokenizedText(
                tokens = tokens,
                onTokenClick = viewModel::select,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                darkTheme = isSystemInDarkTheme(),
            )
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
