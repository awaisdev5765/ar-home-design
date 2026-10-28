package com.arhomedesign.homedesign.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class OnboardStep(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val steps = listOf(
        OnboardStep("Point at a wall", "Move slowly to scan your room", Icons.Rounded.ViewInAr),
        OnboardStep("Tap a color", "Pick a swatch and preview instantly", Icons.Rounded.Palette),
        OnboardStep("Save your palette", "Keep favorite colors for later", Icons.Rounded.PhotoLibrary),
    )
    val pager = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val step = steps[index]
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(step.icon, contentDescription = null, modifier = Modifier.size(72.dp))
                Text(step.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 18.dp))
                Text(step.subtitle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(steps.size) { idx ->
                    Box(
                        Modifier
                            .size(if (pager.currentPage == idx) 10.dp else 8.dp)
                            .background(
                                if (pager.currentPage == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                CircleShape,
                            )
                    )
                }
            }
        }
        Button(
            onClick = {
                if (pager.currentPage == steps.lastIndex) onFinished()
                else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (pager.currentPage == steps.lastIndex) "Get Started" else "Next")
        }
    }
}
