package com.closetmate.app.data.repository

import android.content.Context
import com.closetmate.app.data.local.AppDatabase
import com.closetmate.app.data.local.dao.CategoryCount
import com.closetmate.app.data.local.entity.ClothingEntity
import kotlinx.coroutines.flow.Flow

class ClothingRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).clothingDao()

    fun getAllClothing(): Flow<List<ClothingEntity>> = dao.getAllClothing()

    fun getByCategory(category: String): Flow<List<ClothingEntity>> = dao.getByCategory(category)

    fun getByStatus(status: String): Flow<List<ClothingEntity>> = dao.getByStatus(status)

    fun getBySeason(season: String): Flow<List<ClothingEntity>> = dao.getBySeason(season)

    fun search(keyword: String): Flow<List<ClothingEntity>> = dao.search(keyword)

    fun getIdleClothing(beforeTimestamp: Long): Flow<List<ClothingEntity>> =
        dao.getIdleClothing(beforeTimestamp)

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()

    fun getCategoryStats(): Flow<List<CategoryCount>> = dao.getCategoryStats()

    fun getTotalSpending(): Flow<Double?> = dao.getTotalSpending()

    suspend fun getAllOnce(): List<ClothingEntity> = dao.getAllOnce()

    suspend fun getById(id: String): ClothingEntity? = dao.getById(id)

    suspend fun insert(clothing: ClothingEntity) = dao.insert(clothing)

    suspend fun update(clothing: ClothingEntity) = dao.update(clothing)

    suspend fun delete(clothing: ClothingEntity) = dao.delete(clothing)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun incrementWearCount(id: String) = dao.incrementWearCount(id)

    suspend fun updateStatus(id: String, status: String) = dao.updateStatus(id, status)
}
