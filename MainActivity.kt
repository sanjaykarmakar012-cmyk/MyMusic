package com.mymusic.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MusicViewModel = viewModel()
            MyMusicTheme(vm) { App(vm) }
        }
    }
}

@Composable
fun MyMusicTheme(vm: MusicViewModel, content: @Composable () -> Unit) {
    val dark = when (vm.themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val ctx = LocalContext.current
    val accent = Color(vm.accent)
    var scheme = when {
        vm.dynamic && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = accent, onPrimary = Color.White, secondary = accent, tertiary = accent)
        else -> lightColorScheme(primary = accent, onPrimary = Color.White, secondary = accent, tertiary = accent)
    }
    if (dark && vm.amoled) scheme = scheme.copy(background = Color.Black, surface = Color.Black)
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun App(vm: MusicViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(Triple("Home", Icons.Default.Home, 0), Triple("Search", Icons.Default.Search, 1), Triple("Settings", Icons.Default.Settings, 2))
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (label, icon, i) ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i },
                        icon = { Icon(icon, label) }, label = { Text(label) })
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            NowPlaying(vm)
            Crossfade(tab, label = "tab", modifier = Modifier.weight(1f)) { t ->
                when (t) {
                    0 -> TrackList(vm, vm.home, "Trending music")
                    1 -> SearchTab(vm)
                    else -> SettingsTab(vm)
                }
            }
        }
    }
}

@Composable
fun NowPlaying(vm: MusicViewModel) {
    AnimatedVisibility(
        visible = vm.current != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val t = vm.current
        if (t != null) {
            Column {
                YtPlayer(
                    videoId = t.id, onEnded = vm::next, onError = vm::next,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16 / 9f)
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                        Text(t.channel, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = vm::previous) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous") }
                    IconButton(onClick = vm::next) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next") }
                }
            }
        }
    }
}

@Composable
fun TrackList(vm: MusicViewModel, tracks: List<Track>, title: String, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(4.dp)) }
        if (vm.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        vm.error?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error) } }
        itemsIndexed(tracks, key = { _, t -> t.id }) { i, t ->
            TrackRow(t, selected = vm.current?.id == t.id, modifier = Modifier.animateItem()) { vm.play(tracks, i) }
        }
    }
}

@Composable
fun TrackRow(t: Track, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), label = "rowBg"
    )
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(t.thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
            Text(t.channel, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SearchTab(vm: MusicViewModel) {
    var q by rememberSaveable { mutableStateOf("") }
    val kb = LocalSoftwareKeyboardController.current
    Column {
        OutlinedTextField(
            value = q, onValueChange = { q = it },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            singleLine = true, shape = RoundedCornerShape(28.dp),
            placeholder = { Text("Search songs, artists…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search(q); kb?.hide() })
        )
        TrackList(vm, vm.results, "Results", Modifier.weight(1f))
    }
}

private val accents = listOf(0xFFE53935, 0xFFD81B60, 0xFF6750A4, 0xFF1E88E5, 0xFF2E7D32, 0xFFFB8C00)

@Composable
fun SettingsTab(vm: MusicViewModel) {
    var key by remember { mutableStateOf(vm.apiKey) }
    var region by remember { mutableStateOf(vm.region) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Text("Theme", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("System", "Light", "Dark").forEachIndexed { i, l ->
                FilterChip(selected = vm.themeMode == i, onClick = { vm.updateThemeMode(i) }, label = { Text(l) })
            }
        }
        SwitchRow("Pure black (AMOLED)", vm.amoled, vm::updateAmoled)
        if (Build.VERSION.SDK_INT >= 31) SwitchRow("Dynamic colour (Material You)", vm.dynamic, vm::updateDynamic)

        Text("Accent colour", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            accents.forEach { c ->
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color(c))
                        .then(if (vm.accent == c) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        .clickable { vm.updateAccent(c) }
                )
            }
        }

        OutlinedTextField(key, { key = it }, label = { Text("YouTube Data API key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(region, { region = it.uppercase().take(2) }, label = { Text("Region code (e.g. US, IN)") }, singleLine = true)
        Button(onClick = { vm.saveApi(key.trim(), region) }) { Text("Save & refresh") }
        Text(
            "Free key: Google Cloud Console → enable “YouTube Data API v3” → Credentials → API key.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked, onChange)
    }
}
