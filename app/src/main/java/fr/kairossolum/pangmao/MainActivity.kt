package fr.kairossolum.pangmao

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import fr.kairossolum.pangmao.ui.about.AboutScreen
import fr.kairossolum.pangmao.ui.about.AboutViewModel
import fr.kairossolum.pangmao.ui.common.viewModelFactory
import fr.kairossolum.pangmao.ui.entry.EntryScreen
import fr.kairossolum.pangmao.ui.entry.EntryViewModel
import fr.kairossolum.pangmao.ui.ocr.OcrScreen
import fr.kairossolum.pangmao.ui.ocr.OcrViewModel
import fr.kairossolum.pangmao.ui.reader.ReaderScreen
import fr.kairossolum.pangmao.ui.reader.ReaderViewModel
import fr.kairossolum.pangmao.ui.search.SearchScreen
import fr.kairossolum.pangmao.ui.search.SearchViewModel
import fr.kairossolum.pangmao.ui.study.StudyScreen
import fr.kairossolum.pangmao.ui.study.StudyViewModel
import fr.kairossolum.pangmao.ui.theme.PangmaoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
    private val _sharedText = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptIntent(intent)
        setContent {
            PangmaoTheme {
                Surface {
                    PangmaoApp(
                        container = (application as PangmaoApplication).container,
                        sharedText = _sharedText.asStateFlow(),
                        consumeSharedText = { _sharedText.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptIntent(intent)
    }

    private fun acceptIntent(intent: Intent?) {
        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }
        if (!text.isNullOrBlank()) _sharedText.value = text
    }
}

private data class MainDestination(val route: String, val label: String, val icon: ImageVector)

private val mainDestinations = listOf(
    MainDestination("dictionary", "Dictionnaire", Icons.Outlined.MenuBook),
    MainDestination("reader", "Lecteur", Icons.Outlined.TextSnippet),
    MainDestination("ocr", "OCR", Icons.Outlined.CameraAlt),
    MainDestination("study", "Cartes", Icons.Outlined.School),
)

@Composable
private fun PangmaoApp(
    container: AppContainer,
    sharedText: kotlinx.coroutines.flow.StateFlow<String?>,
    consumeSharedText: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val incomingText by sharedText.collectAsStateWithLifecycle()
    val showBottomBar = mainDestinations.any { it.route == currentRoute }

    LaunchedEffect(incomingText) {
        if (!incomingText.isNullOrBlank() && currentRoute != "reader") {
            navController.navigateMain("reader")
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    mainDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.navigateMain(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = navController, startDestination = "dictionary") {
                composable("dictionary") {
                    val model: SearchViewModel = viewModel(
                        factory = viewModelFactory { SearchViewModel(container.dictionary, container.study) }
                    )
                    SearchScreen(
                        viewModel = model,
                        onOpenEntry = { navController.navigate("entry/$it") },
                        onOpenAbout = { navController.navigate("about") },
                    )
                }
                composable("reader") {
                    val model: ReaderViewModel = viewModel(
                        factory = viewModelFactory { ReaderViewModel(container.dictionary) }
                    )
                    LaunchedEffect(incomingText) {
                        incomingText?.takeIf(String::isNotBlank)?.let {
                            model.setText(it)
                            consumeSharedText()
                        }
                    }
                    ReaderScreen(model, onOpenEntry = { navController.navigate("entry/$it") })
                }
                composable("ocr") {
                    val model: OcrViewModel = viewModel(
                        factory = viewModelFactory { OcrViewModel(container.dictionary) }
                    )
                    OcrScreen(model, onOpenEntry = { navController.navigate("entry/$it") })
                }
                composable("study") {
                    val model: StudyViewModel = viewModel(
                        factory = viewModelFactory { StudyViewModel(container.study) }
                    )
                    StudyScreen(model, onOpenEntry = { navController.navigate("entry/$it") })
                }
                composable(
                    route = "entry/{entryId}",
                    arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
                ) { entry ->
                    val identifier = entry.arguments?.getLong("entryId") ?: return@composable
                    val model: EntryViewModel = viewModel(
                        key = "entry-$identifier",
                        factory = viewModelFactory {
                            EntryViewModel(identifier, container.dictionary, container.study)
                        },
                    )
                    EntryScreen(model, onBack = navController::navigateUp)
                }
                composable("about") {
                    val model: AboutViewModel = viewModel(
                        factory = viewModelFactory { AboutViewModel(container.dictionary) }
                    )
                    AboutScreen(model, onBack = navController::navigateUp)
                }
            }
        }
    }
}

private fun NavHostController.navigateMain(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
