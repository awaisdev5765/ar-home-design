package com.arhomedesign.projects.data

import com.arhomedesign.core.model.Project
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProjectsRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun createProject(title: String, description: String): String {
        val uid = auth.currentUser?.uid ?: error("User must be authenticated")
        val ref = firestore.collection("projects").document()
        val payload = mapOf(
            "ownerId" to uid,
            "title" to title.trim(),
            "description" to description.trim(),
            "propertyType" to "apartment",
            "status" to "draft"
        )
        ref.set(payload).await()
        return ref.id
    }

    suspend fun loadProjects(): List<Project> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snap = firestore.collection("projects")
            .whereEqualTo("ownerId", uid)
            .get()
            .await()

        return snap.documents.map { doc ->
            Project(
                id = doc.id,
                ownerId = doc.getString("ownerId") ?: "",
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                propertyType = doc.getString("propertyType") ?: "apartment",
                status = doc.getString("status") ?: "draft"
            )
        }
    }
}
