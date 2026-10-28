package com.arhomedesign.projects.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arhomedesign.core.model.Project
import com.arhomedesign.projects.data.ProjectsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val isLoading: Boolean = false,
    val projects: List<Project> = emptyList(),
    val error: String? = null
)

class ProjectsViewModel(
    private val repository: ProjectsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.loadProjects() }
                .onSuccess { items ->
                    _uiState.value = ProjectsUiState(projects = items)
                }
                .onFailure { throwable ->
                    _uiState.value = ProjectsUiState(error = throwable.message ?: "Failed to load projects")
                }
        }
    }

    fun createProject(title: String, description: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.createProject(title, description) }
                .onSuccess { loadProjects() }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to create project"
                    )
                }
        }
    }
}
