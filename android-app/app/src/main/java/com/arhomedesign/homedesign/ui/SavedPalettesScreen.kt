package com.arhomedesign.homedesign.ui

import android.Manifest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arhomedesign.homedesign.HomeDesignViewModel
import com.arhomedesign.homedesign.data.SavedPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SavedPalettesScreen(
    viewModel: HomeDesignViewModel = hiltViewModel(),
    onTryAr: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val palettes by viewModel.savedPalettes.collectAsStateWithLifecycle()
    val streak by viewModel.streakDays.collectAsStateWithLifecycle()
    val notifOn by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var expandedPalette by remember { mutableStateOf<SavedPalette?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (brand, colorOfDay, colorHex) = remember { viewModel.colorOfTheDay() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.setDailyNotifications(!notifOn)
    }
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Saved Palettes") },
                actions = {
                    IconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setDailyNotifications(!notifOn)
                        }
                    }) {
                        Icon(Icons.Rounded.Notifications, contentDescription = "Daily notifications")
                    }
                },
            )
        },
    ) { paddingValues ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = refreshing,
            onRefresh = {
                refreshing = true
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    refreshing = false
                }
            },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState),
        ) {
            if (palettes.isEmpty()) {
                EmptyState(onTryAr = { onTryAr(colorHex) })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorOfDayCard(
                                brand = brand,
                                name = colorOfDay.name,
                                hex = colorHex,
                                onTry = { onTryAr(colorHex) },
                            )
                            if (streak >= 3) {
                                FilterChip(
                                    selected = true,
                                    onClick = {},
                                    label = { Text("🔥 ${streak}-day streak") },
                                )
                            }
                        }
                    }
                    items(palettes, key = { it.id }) { palette ->
                        PaletteCard(
                            palette = palette,
                            onLongPress = {
                                viewModel.deletePaletteWithUndo(palette)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator?.vibrate(60L)
                                }
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Palette deleted",
                                        actionLabel = "Undo",
                                    )
                                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                        viewModel.undoDeletePalette()
                                    }
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(palette) },
                            onClick = { expandedPalette = palette },
                        )
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    if (expandedPalette != null) {
        ModalBottomSheet(
            onDismissRequest = { expandedPalette = null },
            sheetState = sheetState,
        ) {
            val palette = expandedPalette ?: return@ModalBottomSheet
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(palette.name, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.colors.forEach {
                        Box(Modifier.size(24.dp).background(it, CircleShape))
                    }
                }
                Text(SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(palette.timestamp)))
                TextButton(onClick = {
                    palette.colors.firstOrNull()?.let {
                        onTryAr(String.format("%06X", it.value.toInt() and 0xFFFFFF))
                    }
                    expandedPalette = null
                }) {
                    Text("Load in AR")
                }
            }
        }
    }
}

@Composable
private fun ColorOfDayCard(brand: String, name: String, hex: String, onTry: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(80.dp).background(
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#$hex")),
                    RoundedCornerShape(14.dp),
                )
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Color of the Day", fontWeight = FontWeight.SemiBold)
                Text(name)
                Text("$brand • #$hex", style = MaterialTheme.typography.bodySmall)
            }
            FilterChip(selected = false, onClick = onTry, label = { Text("Try in AR") })
        }
    }
}

@Composable
private fun EmptyState(onTryAr: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(64.dp))
            Text("No palettes yet", style = MaterialTheme.typography.titleLarge)
            Text("Your saved colors will appear here", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onTryAr) { Text("Try AR Preview") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaletteCard(
    palette: SavedPalette,
    onLongPress: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                palette.colors.take(5).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }
            Text(palette.name, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(16.dp).clickable { onToggleFavorite() },
                    tint = if (palette.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                )
                Text(if (palette.isFavorite) "1" else "0", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(palette.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
