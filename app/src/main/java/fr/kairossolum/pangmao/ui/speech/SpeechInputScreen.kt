package fr.kairossolum.pangmao.ui.speech

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.domain.speech.SpeechCapturePhase
import fr.kairossolum.pangmao.domain.speech.SpeechRecognitionEngineMode
import fr.kairossolum.pangmao.domain.speech.SpeechRecognitionFailure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechInputScreen(
    viewModel: SpeechInputViewModel,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onOpenReader: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startCapture() else viewModel.reportPermissionDenied()
    }
    val startCapture = {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startCapture()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.cancelCapture() }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.speech_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SpeechEngineCard(state.engineMode)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (state.phase == SpeechCapturePhase.LISTENING) {
                        Icons.Outlined.GraphicEq
                    } else {
                        Icons.Outlined.MicNone
                    },
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(state.phase.statusResource()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.phase == SpeechCapturePhase.PROCESSING) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }

            if (state.phase == SpeechCapturePhase.LISTENING) {
                Button(
                    onClick = viewModel::stopCapture,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.StopCircle, contentDescription = null)
                    Text("  ${stringResource(R.string.speech_stop)}")
                }
            } else {
                Button(
                    onClick = startCapture,
                    enabled = state.phase != SpeechCapturePhase.PROCESSING &&
                        state.engineMode != SpeechRecognitionEngineMode.UNAVAILABLE,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.MicNone, contentDescription = null)
                    Text("  ${stringResource(R.string.speech_start)}")
                }
            }

            state.failure?.let { failure ->
                Text(
                    text = stringResource(failure.messageResource()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            OutlinedTextField(
                value = state.transcript,
                onValueChange = viewModel::editTranscript,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
                minLines = 3,
                label = { Text(stringResource(R.string.speech_transcript_label)) },
            )

            val alternatives = state.candidates
                .filter { it != state.transcript }
                .take(2)
            if (alternatives.isNotEmpty() && !state.isBusy) {
                Text(
                    text = stringResource(R.string.speech_candidates_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    alternatives.forEach { candidate ->
                        AssistChip(
                            onClick = { viewModel.selectCandidate(candidate) },
                            label = { Text(candidate, maxLines = 1) },
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = { onSearch(state.transcript.trim()) },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Text("  ${stringResource(R.string.speech_search_action)}")
            }
            OutlinedButton(
                onClick = { onOpenReader(state.transcript.trim()) },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.TextSnippet, contentDescription = null)
                Text("  ${stringResource(R.string.speech_reader_action)}")
            }
            TextButton(
                onClick = viewModel::clear,
                enabled = !state.isBusy && state.transcript.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                Text("  ${stringResource(R.string.speech_clear)}")
            }

            Text(
                text = stringResource(R.string.speech_privacy_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SpeechEngineCard(mode: SpeechRecognitionEngineMode) {
    val title = when (mode) {
        SpeechRecognitionEngineMode.ON_DEVICE -> R.string.speech_engine_on_device
        SpeechRecognitionEngineMode.SYSTEM -> R.string.speech_engine_system
        SpeechRecognitionEngineMode.UNAVAILABLE -> R.string.speech_engine_unavailable
    }
    val detail = when (mode) {
        SpeechRecognitionEngineMode.ON_DEVICE -> R.string.speech_engine_on_device_detail
        SpeechRecognitionEngineMode.SYSTEM -> R.string.speech_engine_system_detail
        SpeechRecognitionEngineMode.UNAVAILABLE -> R.string.speech_engine_unavailable_detail
    }
    val icon: ImageVector = when (mode) {
        SpeechRecognitionEngineMode.ON_DEVICE -> Icons.Outlined.CloudOff
        SpeechRecognitionEngineMode.SYSTEM -> Icons.Outlined.Cloud
        SpeechRecognitionEngineMode.UNAVAILABLE -> Icons.Outlined.MicNone
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(title), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@StringRes
private fun SpeechCapturePhase.statusResource(): Int = when (this) {
    SpeechCapturePhase.IDLE -> R.string.speech_status_idle
    SpeechCapturePhase.LISTENING -> R.string.speech_status_listening
    SpeechCapturePhase.PROCESSING -> R.string.speech_status_processing
    SpeechCapturePhase.READY -> R.string.speech_status_ready
    SpeechCapturePhase.ERROR -> R.string.speech_status_error
}

@StringRes
private fun SpeechRecognitionFailure.messageResource(): Int = when (this) {
    SpeechRecognitionFailure.PERMISSION_DENIED -> R.string.speech_error_permission
    SpeechRecognitionFailure.NO_MATCH -> R.string.speech_error_no_match
    SpeechRecognitionFailure.NETWORK -> R.string.speech_error_network
    SpeechRecognitionFailure.BUSY -> R.string.speech_error_busy
    SpeechRecognitionFailure.UNAVAILABLE -> R.string.speech_error_unavailable
    SpeechRecognitionFailure.AUDIO -> R.string.speech_error_audio
    SpeechRecognitionFailure.TIMEOUT -> R.string.speech_error_timeout
    SpeechRecognitionFailure.CLIENT -> R.string.speech_error_client
    SpeechRecognitionFailure.UNKNOWN -> R.string.speech_error_unknown
}
