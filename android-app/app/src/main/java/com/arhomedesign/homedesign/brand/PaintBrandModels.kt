package com.arhomedesign.homedesign.brand

import androidx.compose.ui.graphics.Color

enum class FinishType(val displayName: String, val targetOpacity: Float) {
    MATTE("Matte", 0.7f),
    SATIN("Satin", 0.6f),
    GLOSS("Gloss", 0.5f),
}

data class PaintBrandColor(
    val name: String,
    val hexColor: String,
    val productCode: String,
    val finishType: FinishType,
)

data class PaintBrand(
    val name: String,
    val logoText: String,
    val colors: List<PaintBrandColor>,
)

val samplePaintBrands: List<PaintBrand> = listOf(
    PaintBrand(
        name = "Dulux",
        logoText = "D",
        colors = listOf(
            PaintBrandColor("Ivory Silk", "F3EBDD", "DX-101", FinishType.MATTE),
            PaintBrandColor("Pebble Mist", "CFC7BC", "DX-102", FinishType.MATTE),
            PaintBrandColor("Ocean Haze", "7FA6C6", "DX-103", FinishType.SATIN),
            PaintBrandColor("Lagoon Teal", "2B7D83", "DX-104", FinishType.SATIN),
            PaintBrandColor("Olive Leaf", "7A8C63", "DX-105", FinishType.MATTE),
            PaintBrandColor("Clay Terracotta", "B7745B", "DX-106", FinishType.MATTE),
            PaintBrandColor("Charcoal Loft", "4A4E56", "DX-107", FinishType.GLOSS),
            PaintBrandColor("Amber Glow", "D79E4C", "DX-108", FinishType.SATIN),
            PaintBrandColor("Misty Rose", "D8A8B2", "DX-109", FinishType.MATTE),
            PaintBrandColor("Midnight Ink", "1E2A3C", "DX-110", FinishType.GLOSS),
        ),
    ),
    PaintBrand(
        name = "Nippon",
        logoText = "N",
        colors = listOf(
            PaintBrandColor("Porcelain", "F5F2EA", "NP-201", FinishType.MATTE),
            PaintBrandColor("Urban Greige", "BFB8AE", "NP-202", FinishType.MATTE),
            PaintBrandColor("Sky Drift", "8DB4D3", "NP-203", FinishType.SATIN),
            PaintBrandColor("Sea Foam", "91B8A8", "NP-204", FinishType.SATIN),
            PaintBrandColor("Fern Trail", "5C7A55", "NP-205", FinishType.MATTE),
            PaintBrandColor("Warm Sand", "C9AA85", "NP-206", FinishType.MATTE),
            PaintBrandColor("Brick Note", "A65E4E", "NP-207", FinishType.SATIN),
            PaintBrandColor("Slate Night", "566171", "NP-208", FinishType.GLOSS),
            PaintBrandColor("Lavender Cloud", "B6AAD3", "NP-209", FinishType.MATTE),
            PaintBrandColor("Graphite Edge", "2C3038", "NP-210", FinishType.GLOSS),
        ),
    ),
    PaintBrand(
        name = "Asian Paints",
        logoText = "A",
        colors = listOf(
            PaintBrandColor("Shell White", "F8F4EC", "AP-301", FinishType.MATTE),
            PaintBrandColor("Almond Dust", "D5C8B5", "AP-302", FinishType.MATTE),
            PaintBrandColor("Cool Aqua", "78B7C5", "AP-303", FinishType.SATIN),
            PaintBrandColor("Marine Glass", "3F8DA0", "AP-304", FinishType.SATIN),
            PaintBrandColor("Pistachio", "9BB28A", "AP-305", FinishType.MATTE),
            PaintBrandColor("Burnt Clay", "BC775F", "AP-306", FinishType.MATTE),
            PaintBrandColor("Royal Plum", "6D4E70", "AP-307", FinishType.SATIN),
            PaintBrandColor("Silver Steel", "7B8897", "AP-308", FinishType.GLOSS),
            PaintBrandColor("Mustard Glow", "C89A42", "AP-309", FinishType.SATIN),
            PaintBrandColor("Ink Black", "1B1E25", "AP-310", FinishType.GLOSS),
        ),
    ),
)

fun PaintBrandColor.asColor(): Color = Color(android.graphics.Color.parseColor("#$hexColor"))
