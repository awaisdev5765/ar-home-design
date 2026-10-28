package com.arhomedesign.core.model

data class Project(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val propertyType: String = "apartment",
    val status: String = "draft"
)
