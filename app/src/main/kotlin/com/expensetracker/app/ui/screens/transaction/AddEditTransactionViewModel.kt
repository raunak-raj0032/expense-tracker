package com.expensetracker.app.ui.screens.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.AccountRepository
import com.expensetracker.app.core.data.repository.CategoryRepository
import com.expensetracker.app.core.data.repository.TagRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.time.LocalDateTime
import javax.inject.Inject

data class AddEditTransactionUiState(
    val transactionId: Long? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val accountId: Long = 0,
    val categoryId: Long? = null,
    val counterpartyAccountId: Long? = null,
    val description: String = "",
    val notes: String = "",
    val selectedTags: Set<Long> = emptySet(),
    val transactionTime: LocalDateTime = LocalDateTime.now(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()
    private var categoryJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            accountRepository.observeActive().collect { accounts ->
                _uiState.update {
                    it.copy(
                        accounts = accounts,
                        accountId = it.accountId.takeIf { id -> accounts.any { a -> a.id == id } } ?: accounts.firstOrNull()?.id ?: 0
                    )
                }
            }
        }

        viewModelScope.launch {
            observeCategoriesFor(TransactionType.EXPENSE)
        }

        viewModelScope.launch {
            tagRepository.observeAll().collect { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }
    }

    fun loadTransaction(id: Long) {
        viewModelScope.launch {
            val transaction = transactionRepository.getById(id)
            transaction?.let { tx ->
                val tagIds = tagRepository.getTagIdsForTransaction(id)
                _uiState.update {
                    it.copy(
                        transactionId = tx.id,
                        transactionType = tx.type,
                        amount = formatAmount(tx.amountMinor),
                        accountId = tx.accountId,
                        categoryId = tx.categoryId,
                        counterpartyAccountId = tx.counterpartyAccountId,
                        description = tx.description ?: "",
                        notes = tx.notes ?: "",
                        selectedTags = tagIds.toSet(),
                        transactionTime = tx.transactionTime
                    )
                }
            }
        }
    }

    fun updateTransactionType(type: TransactionType) {
        _uiState.update { it.copy(transactionType = type) }
        observeCategoriesFor(type)
    }

    fun updateAmount(amount: String) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun updateAccount(accountId: Long) {
        _uiState.update { it.copy(accountId = accountId) }
    }

    fun updateCategory(categoryId: Long) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun toggleTag(tagId: Long) {
        _uiState.update {
            val newTags = if (it.selectedTags.contains(tagId)) {
                it.selectedTags - tagId
            } else {
                it.selectedTags + tagId
            }
            it.copy(selectedTags = newTags)
        }
    }

    fun saveTransaction() {
        viewModelScope.launch {
            val state = _uiState.value

            val amountMinor = state.amount
                .toBigDecimalOrNull()
                ?.movePointRight(2)
                ?.setScale(0, RoundingMode.HALF_UP)
                ?.longValueExact()
                ?: 0L

            val transaction = Transaction(
                id = state.transactionId ?: 0,
                type = state.transactionType,
                amountMinor = amountMinor,
                transactionTime = state.transactionTime,
                accountId = state.accountId,
                categoryId = state.categoryId,
                counterpartyAccountId = state.counterpartyAccountId,
                description = state.description.takeIf { it.isNotEmpty() },
                notes = state.notes.takeIf { it.isNotEmpty() },
                status = TransactionStatus.CONFIRMED,
                source = CaptureSourceType.MANUAL,
                tags = state.selectedTags.toList()
            )

            try {
                if (state.transactionId != null) {
                    transactionRepository.update(transaction)
                } else {
                    transactionRepository.insert(transaction)
                }
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            val transactionId = _uiState.value.transactionId ?: return@launch
            try {
                transactionRepository.delete(transactionId)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun formatAmount(amountMinor: Long): String {
        val whole = amountMinor / 100
        val fraction = (amountMinor % 100).toString().padStart(2, '0')
        return if (fraction == "00") {
            whole.toString()
        } else {
            "$whole.$fraction"
        }
    }

    private fun observeCategoriesFor(type: TransactionType) {
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            val categories = when (type) {
                TransactionType.EXPENSE, TransactionType.REFUND -> categoryRepository.observeExpenseCategories()
                TransactionType.INCOME -> categoryRepository.observeIncomeCategories()
                TransactionType.TRANSFER -> categoryRepository.observeTree()
            }
            categories.collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }
}
