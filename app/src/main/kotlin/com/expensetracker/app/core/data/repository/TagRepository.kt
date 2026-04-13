package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.TagDao
import com.expensetracker.app.core.database.dao.TransactionTagDao
import com.expensetracker.app.core.database.entity.TagEntity
import com.expensetracker.app.core.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao,
    private val transactionTagDao: TransactionTagDao
) {
    fun observeAll(): Flow<List<Tag>> = tagDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun insert(tag: Tag): Long = tagDao.insert(tag.toEntity())

    suspend fun update(tag: Tag) = tagDao.update(tag.toEntity())

    suspend fun delete(tag: Tag) = tagDao.delete(tag.toEntity())

    suspend fun getById(id: Long): Tag? = tagDao.getById(id)?.toDomain()

    suspend fun getCount(): Int = tagDao.getCount()

    fun observeTagIdsForTransaction(transactionId: Long): Flow<List<Long>> =
        transactionTagDao.observeTagIdsForTransaction(transactionId)

    suspend fun getTagIdsForTransaction(transactionId: Long): List<Long> =
        transactionTagDao.getTagIdsForTransaction(transactionId)

    private fun TagEntity.toDomain(): Tag = Tag(
        id = id,
        name = name,
        colorHex = colorHex,
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault())
    )

    private fun Tag.toEntity(): TagEntity = TagEntity(
        id = id,
        name = name,
        colorHex = colorHex,
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}