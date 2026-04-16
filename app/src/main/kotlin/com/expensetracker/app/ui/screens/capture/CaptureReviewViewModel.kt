package com.expensetracker.app.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.capture.CaptureEventRepository
import com.expensetracker.app.capture.CaptureSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaptureReviewUiState(
    val suggestions: List<CaptureSuggestion> = emptyList(),
    val isImportingSms: Boolean = false,
    val activeSuggestionId: Long? = null,
    val message: String? = null
)

@HiltViewModel
class CaptureReviewViewModel @Inject constructor(
    private val captureEventRepository: CaptureEventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureReviewUiState())
    val uiState: StateFlow<CaptureReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            captureEventRepository.observeReviewQueue().collect { suggestions ->
                _uiState.update { it.copy(suggestions = suggestions) }
            }
        }
    }

    fun importRecentSms(limit: Int? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingSms = true) }
            val message = try {
                val imported = captureEventRepository.importRecentSms(limit)
                if (imported > 0) {
                    "Imported $imported payment message(s) from your SMS history."
                } else {
                    "No new payment SMS messages were found."
                }
            } catch (e: SecurityException) {
                "SMS access is required before importing message history."
            } catch (e: Exception) {
                e.message ?: "Unable to import SMS history right now."
            }
            _uiState.update {
                it.copy(
                    isImportingSms = false,
                    message = message
                )
            }
        }
    }

    fun addSuggestion(eventId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeSuggestionId = eventId) }
            val message = try {
                captureEventRepository.addToLedger(eventId)
                "Transaction added to the ledger."
            } catch (e: Exception) {
                e.message ?: "Unable to add this suggestion right now."
            }
            _uiState.update {
                it.copy(
                    activeSuggestionId = null,
                    message = message
                )
            }
        }
    }

    fun ignoreSuggestion(eventId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(activeSuggestionId = eventId) }
            val message = try {
                captureEventRepository.ignore(eventId)
                "Suggestion ignored."
            } catch (e: Exception) {
                e.message ?: "Unable to ignore this suggestion right now."
            }
            _uiState.update {
                it.copy(
                    activeSuggestionId = null,
                    message = message
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
