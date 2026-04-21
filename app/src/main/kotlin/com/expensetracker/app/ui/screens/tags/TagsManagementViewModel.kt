package com.expensetracker.app.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.TagRepository
import com.expensetracker.app.core.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagsManagementUiState(
    val tags: List<Tag> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TagsManagementViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TagsManagementUiState())
    val uiState: StateFlow<TagsManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tagRepository.observeAll().collect { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }
    }

    fun createTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                tagRepository.insert(Tag(name = trimmed))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            try {
                tagRepository.delete(tag)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
