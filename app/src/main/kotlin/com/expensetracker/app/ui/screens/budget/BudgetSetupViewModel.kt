package com.expensetracker.app.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.RoundingMode
import javax.inject.Inject

data class BudgetSetupUiState(
    val budgetName: String = "Monthly Budget",
    val amount: String = "",
    val existingAmountMinor: Long? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BudgetSetupViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BudgetSetupUiState())
    val uiState: StateFlow<BudgetSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            budgetRepository.observeLatestMonthly().collect { budget ->
                _uiState.update { state ->
                    state.copy(
                        budgetName = budget?.name ?: state.budgetName,
                        amount = budget?.amountMinor?.toMoneyString() ?: state.amount,
                        existingAmountMinor = budget?.amountMinor
                    )
                }
            }
        }
    }

    fun updateBudgetName(value: String) {
        _uiState.update { it.copy(budgetName = value, error = null) }
    }

    fun updateAmount(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() || it == '.' }) {
            val parts = value.split(".")
            if (parts.size <= 2 && (parts.size != 2 || parts[1].length <= 2)) {
                _uiState.update { it.copy(amount = value, error = null) }
            }
        }
    }

    fun saveBudget() {
        viewModelScope.launch {
            val amountMinor = _uiState.value.amount
                .toBigDecimalOrNull()
                ?.movePointRight(2)
                ?.setScale(0, RoundingMode.HALF_UP)
                ?.longValueExact()

            if (amountMinor == null || amountMinor <= 0) {
                _uiState.update { it.copy(error = "Enter a valid budget amount.") }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                budgetRepository.upsertMonthlyBudget(
                    amountMinor = amountMinor,
                    name = _uiState.value.budgetName.ifBlank { "Monthly Budget" }
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Unable to save budget right now."
                    )
                }
            }
        }
    }

    private fun Long.toMoneyString(): String {
        val whole = this / 100
        val fraction = (this % 100).toString().padStart(2, '0')
        return if (fraction == "00") whole.toString() else "$whole.$fraction"
    }
}
