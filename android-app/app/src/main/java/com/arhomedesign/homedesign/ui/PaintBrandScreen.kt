package com.arhomedesign.homedesign.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arhomedesign.homedesign.HomeDesignViewModel
import com.arhomedesign.homedesign.brand.FinishType
import com.arhomedesign.homedesign.brand.PaintBrand
import com.arhomedesign.homedesign.brand.PaintBrandColor
import com.arhomedesign.homedesign.brand.asColor
import com.arhomedesign.homedesign.brand.samplePaintBrands
import com.arhomedesign.homedesign.state.UiState
import androidx.compose.animation.AnimatedVisibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaintBrandScreen(
    viewModel: HomeDesignViewModel = hiltViewModel(),
    onBackToAr: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val successState = state as? UiState.Success
    val selectedBrandName = successState?.data?.selectedBrandName
    val selectedProductCode = successState?.data?.selectedProductCode
    val selectedFinish = successState?.data?.finishType ?: FinishType.SATIN
    var query by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf(samplePaintBrands.first()) }
    if (selectedBrandName != null) {
        selectedBrand = samplePaintBrands.firstOrNull { it.name == selectedBrandName } ?: selectedBrand
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Paint Brands") }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrandLogoRow(
                brands = samplePaintBrands,
                selected = selectedBrand,
                onSelect = { selectedBrand = it },
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search brand colors") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                FinishType.entries.forEachIndexed { index, finish ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = FinishType.entries.size),
                        onClick = { viewModel.setFinishType(finish) },
                        selected = selectedFinish == finish,
                    ) {
                        Text(finish.displayName)
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                gridItems(selectedBrand.colors.filter { it.name.contains(query, ignoreCase = true) }) { brandColor ->
                    AnimatedVisibility(visible = true) {
                        BrandColorCard(
                            brandName = selectedBrand.name,
                            color = brandColor,
                            selected = brandColor.productCode == selectedProductCode,
                            onSelect = {
                                viewModel.selectBrandColor(selectedBrand.name, brandColor)
                                onBackToAr()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandLogoRow(
    brands: List<PaintBrand>,
    selected: PaintBrand,
    onSelect: (PaintBrand) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(brands) { brand ->
            FilterChip(
                selected = selected.name == brand.name,
                onClick = { onSelect(brand) },
                label = { Text(brand.name) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(brand.logoText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                },
            )
        }
    }
}

@Composable
private fun BrandColorCard(
    brandName: String,
    color: PaintBrandColor,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(48.dp)
                    .background(color.asColor(), RoundedCornerShape(10.dp)),
            )
            Text(color.name, style = MaterialTheme.typography.titleSmall)
            Text("$brandName • ${color.productCode}", style = MaterialTheme.typography.bodySmall)
            Text("Finish: ${color.finishType.displayName}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
