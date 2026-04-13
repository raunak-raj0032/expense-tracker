package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.AccountDao
import com.expensetracker.app.core.database.entity.AccountEntity
import com.expensetracker.app.core.model.Account
import com.expensetracker.app.core.model.AccountType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    fun observeAll(): Flow<List<Account>> = accountDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    fun observeActive(): Flow<List<Account>> = accountDao.observeAllActive().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun insert(account: Account): Long = accountDao.insert(account.toEntity())

    suspend fun update(account: Account) = accountDao.update(account.toEntity())

    suspend fun archive(id: Long) = accountDao.archive(id)

    suspend fun getById(id: Long): Account? = accountDao.getById(id)?.toDomain()

    suspend fun getCount(): Int = accountDao.getCount()

    private fun AccountEntity.toDomain(): Account = Account(
        id = id,
        name = name,
        type = AccountType.valueOf(type),
        currencyCode = currencyCode,
        openingBalance = openingBalance,
        isArchived = isArchived,
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault()),
        updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneId.systemDefault())
    )

    private fun Account.toEntity(): AccountEntity = AccountEntity(
        id = id,
        name = name,
        type = type.name,
        currencyCode = currencyCode,
        openingBalance = openingBalance,
        isArchived = isArchived,
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedAt = updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}