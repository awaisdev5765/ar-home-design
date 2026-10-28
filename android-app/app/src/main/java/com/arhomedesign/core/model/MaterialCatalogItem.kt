package com.arhomedesign.core.model

data class MaterialCatalogItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val presetCode: String = "",
    val colorHex: String = "#FFFFFF",
    val active: Boolean = true
)
