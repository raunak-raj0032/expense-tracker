package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.CategoryDao
import com.expensetracker.app.core.database.dao.CategoryTotal
import com.expensetracker.app.core.database.entity.CategoryEntity
import com.expensetracker.app.core.model.Category
import com.expensetracker.app.core.model.CategoryKind
import com.expensetracker.app.core.model.CategoryBreakdown
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun observeTree(): Flow<List<Category>> = categoryDao.observeTree().map { entities ->
        entities.map { it.toDomain() }
    }

    fun observeExpenseCategories(): Flow<List<Category>> = categoryDao.observeExpenseCategories().map { entities ->
        entities.map { it.toDomain() }
    }

    fun observeIncomeCategories(): Flow<List<Category>> = categoryDao.observeIncomeCategories().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun insert(category: Category): Long = categoryDao.insert(category.toEntity())

    suspend fun update(category: Category) = categoryDao.update(category.toEntity())

    suspend fun archive(id: Long) = categoryDao.archive(id)

    suspend fun getById(id: Long): Category? = categoryDao.getById(id)?.toDomain()

    suspend fun getCount(): Int = categoryDao.getCount()

    private fun CategoryEntity.toDomain(): Category = Category(
        id = id,
        name = name,
        kind = CategoryKind.valueOf(kind),
        parentId = parentId,
        iconKey = iconKey,
        colorHex = colorHex,
        sortOrder = sortOrder,
        isSystem = isSystem,
        isArchived = isArchived
    )

    private fun Category.toEntity(): CategoryEntity = CategoryEntity(
        id = id,
        name = name,
        kind = kind.name,
        parentId = parentId,
        iconKey = iconKey,
        colorHex = colorHex,
        sortOrder = sortOrder,
        isSystem = isSystem,
        isArchived = isArchived
    )
}