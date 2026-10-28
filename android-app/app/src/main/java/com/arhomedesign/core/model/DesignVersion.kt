package com.arhomedesign.core.model

data class DesignVersion(
    val id: String = "",
    val projectId: String = "",
    val roomId: String = "",
    val ownerId: String = "",
    val versionNumber: Int = 1,
    val notes: String = "",
    val createdAt: Long = 0L
)
