package com.closetmate.app.data.repository

import android.content.Context
import com.closetmate.app.data.local.AppDatabase
import com.closetmate.app.data.local.entity.OutfitEntity
import kotlinx.coroutines.flow.Flow

class OutfitRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).outfitDao()

    fun getAllOutfits(): Flow<List<OutfitEntity>> = dao.getAllOutfits()

    fun getFavoriteOutfits(): Flow<List<OutfitEntity>> = dao.getFavoriteOutfits()

    fun getByScene(scene: String): Flow<List<OutfitEntity>> = dao.getByScene(scene)

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()

    fun getOutfitsContainingClothing(clothingId: String): Flow<List<OutfitEntity>> =
        dao.getOutfitsContainingClothing(clothingId)

    suspend fun getById(id: String): OutfitEntity? = dao.getById(id)

    suspend fun insert(outfit: OutfitEntity) = dao.insert(outfit)

    suspend fun update(outfit: OutfitEntity) = dao.update(outfit)

    suspend fun delete(outfit: OutfitEntity) = dao.delete(outfit)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun updateFavorite(id: String, isFavorite: Boolean) =
        dao.updateFavorite(id, isFavorite)

    suspend fun incrementWearCount(id: String) = dao.incrementWearCount(id)
}
