package com.expensetracker.app.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.BackupPreview
import com.expensetracker.app.core.data.repository.BackupRepository
import com.expensetracker.app.core.data.repository.RestoreMode
import com.expensetracker.app.core.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val exportJson: String? = null,
    val importJson: String? = null,
    val preview: BackupPreview? = null,
    val verificationCode: String? = null,
    val enteredCode: String = "",
    val verificationPassed: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    val backupEmail: StateFlow<String> = userPreferences.backupEmail.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ""
    )

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun saveBackupEmail(email: String) {
        viewModelScope.launch {
            userPreferences.setBackupEmail(email)
            _state.update { it.copy(message = "Backup email saved") }
        }
    }

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
                        verificationCode = null,
                        enteredCode = "",
                        verificationPassed = preview.isSameDevice,
                        message = null
                    )
                }
            }
        }
    }

    fun generateVerificationCode() {
        val rawJson = _state.value.importJson ?: return
        viewModelScope.launch {
            runBusy {
                val code = backupRepository.generateVerificationCode(rawJson)
                _state.update { it.copy(verificationCode = code, enteredCode = "", message = "Verification code generated") }
            }
        }
    }

    fun updateEnteredCode(code: String) {
        val cleaned = code.filter(Char::isDigit).take(6)
        _state.update { state ->
            state.copy(
                enteredCode = cleaned,
                verificationPassed = state.verificationPassed || (cleaned.length == 6 && cleaned == state.verificationCode)
            )
        }
    }

    fun trustResetDevice() {
        _state.update { it.copy(verificationPassed = true, message = "Device warning accepted") }
    }

    fun restore(mode: RestoreMode) {
        val rawJson = _state.value.importJson ?: return
        if (!_state.value.verificationPassed) {
            _state.update { it.copy(message = "Verify this backup before restoring") }
            return
        }
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
