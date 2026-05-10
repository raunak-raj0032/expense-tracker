package com.expensetracker.app.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.BackupPreview
import com.expensetracker.app.core.data.repository.BackupRepository
import com.expensetracker.app.core.data.repository.RestoreMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val exportJson: String? = null,
    val importJson: String? = null,
    val preview: BackupPreview? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun prepareExport() {
        viewModelScope.launch {
            runBusy {
                val json = backupRepository.exportBackup()
                _state.update { it.copy(exportJson = json, message = null) }
            }
        }
    }

    fun exportHandled(success: Boolean) {
        _state.update {
            it.copy(
                exportJson = null,
                message = if (success) "Backup exported" else "Backup export cancelled"
            )
        }
    }

    fun loadImport(rawJson: String) {
        viewModelScope.launch {
            runBusy {
                val preview = backupRepository.previewBackup(rawJson)
                _state.update {
                    it.copy(
                        importJson = rawJson,
                        preview = preview,
                        message = null
                    )
                }
            }
        }
    }

    fun restore(mode: RestoreMode) {
        val rawJson = _state.value.importJson ?: return
        viewModelScope.launch {
            runBusy {
                backupRepository.restoreBackup(rawJson, mode)
                _state.update {
                    BackupUiState(message = if (mode == RestoreMode.REWRITE) "Backup restored" else "Backup merged")
                }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private suspend fun runBusy(block: suspend () -> Unit) {
        _state.update { it.copy(busy = true, message = null) }
        try {
            block()
        } catch (t: Throwable) {
            _state.update { it.copy(message = t.message ?: "Backup operation failed") }
        } finally {
            _state.update { it.copy(busy = false) }
        }
    }
}
