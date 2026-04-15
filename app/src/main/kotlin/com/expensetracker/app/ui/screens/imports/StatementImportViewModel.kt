package com.expensetracker.app.ui.screens.imports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.AccountRepository
import com.expensetracker.app.core.model.Account
import com.expensetracker.app.core.model.AccountType
import com.expensetracker.app.statement.StatementImportPreview
import com.expensetracker.app.statement.StatementImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatementImportUiState(
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long? = null,
    val pastedText: String = "",
    val preview: StatementImportPreview? = null,
    val isParsing: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class StatementImportViewModel @Inject constructor(
    accountRepository: AccountRepository,
    private val statementImportRepository: StatementImportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatementImportUiState())
    val uiState: StateFlow<StatementImportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.observeActive().collect { accounts ->
                _uiState.update { state ->
                    state.copy(
                        accounts = accounts,
                        selectedAccountId = state.selectedAccountId
                            ?.takeIf { current -> accounts.any { it.id == current } }
                            ?: accounts.firstOrNull { it.type == AccountType.BANK }?.id
                            ?: accounts.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun updateSelectedAccount(accountId: Long) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun updatePastedText(value: String) {
        _uiState.update { it.copy(pastedText = value) }
    }

    fun previewDocument(
        documentName: String,
        mimeType: String?,
        bytes: ByteArray
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isParsing = true, message = null) }
            val result = runCatching {
                statementImportRepository.previewDocument(
                    documentName = documentName,
                    mimeType = mimeType,
                    bytes = bytes
                )
            }

            _uiState.update { state ->
                result.fold(
                    onSuccess = { preview ->
                        state.copy(
                            isParsing = false,
                            preview = preview,
                            message = if (preview.entries.isEmpty()) {
                                "No statement rows could be parsed from $documentName."
                            } else {
                                "Parsed ${preview.entries.size} entries from $documentName."
                            }
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            isParsing = false,
                            message = error.message ?: "Unable to read that statement right now."
                        )
                    }
                )
            }
        }
    }

    fun previewPastedText() {
        val rawText = _uiState.value.pastedText.trim()
        if (rawText.isBlank()) {
            _uiState.update { it.copy(message = "Paste a statement first so it can be parsed.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isParsing = true, message = null) }
            val result = runCatching {
                statementImportRepository.previewPastedText(
                    sourceName = "Pasted Statement",
                    rawText = rawText
                )
            }

            _uiState.update { state ->
                result.fold(
                    onSuccess = { preview ->
                        state.copy(
                            isParsing = false,
                            preview = preview,
                            message = if (preview.entries.isEmpty()) {
                                "No transaction rows were detected in the pasted statement."
                            } else {
                                "Parsed ${preview.entries.size} entries from pasted text."
                            }
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            isParsing = false,
                            message = error.message ?: "Unable to parse the pasted statement."
                        )
                    }
                )
            }
        }
    }

    fun importPreview() {
        val state = _uiState.value
        val preview = state.preview
        val accountId = state.selectedAccountId

        if (preview == null) {
            _uiState.update { it.copy(message = "Preview a statement before importing.") }
            return
        }
        if (accountId == null) {
            _uiState.update { it.copy(message = "Choose an account for these imported entries.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, message = null) }
            val result = runCatching {
                statementImportRepository.importPreview(preview, accountId)
            }

            _uiState.update { current ->
                result.fold(
                    onSuccess = { importResult ->
                        current.copy(
                            isImporting = false,
                            preview = null,
                            pastedText = "",
                            message = buildString {
                                append("Imported ${importResult.importedCount} statement entr")
                                append(if (importResult.importedCount == 1) "y" else "ies")
                                if (importResult.duplicateCount > 0) {
                                    append(" and skipped ${importResult.duplicateCount} duplicate")
                                    append(if (importResult.duplicateCount == 1) "." else "s.")
                                } else {
                                    append(".")
                                }
                            }
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isImporting = false,
                            message = error.message ?: "Unable to import this statement right now."
                        )
                    }
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
