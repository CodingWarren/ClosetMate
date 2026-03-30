package com.closetmate.app.data.local.dao

import androidx.room.*
import com.closetmate.app.data.local.entity.OutfitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outfit: OutfitEntity)

    @Update
    suspend fun update(outfit: OutfitEntity)

    @Delete
    suspend fun delete(outfit: OutfitEntity)

    @Query("DELETE FROM outfits WHERE id = :id")
    suspend fun deleteById(id: String)

    // 查询所有搭配（按创建时间倒序）
    @Query("SELECT * FROM outfits ORDER BY createdAt DESC")
    fun getAllOutfits(): Flow<List<OutfitEntity>>

    // 查询收藏搭配
    @Query("SELECT * FROM outfits WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteOutfits(): Flow<List<OutfitEntity>>

    // 按场景标签筛选
    @Query("SELECT * FROM outfits WHERE sceneTags LIKE '%' || :scene || '%' ORDER BY createdAt DESC")
    fun getByScene(scene: String): Flow<List<OutfitEntity>>

    // 根据ID查询
    @Query("SELECT * FROM outfits WHERE id = :id")
    suspend fun getById(id: String): OutfitEntity?

    // 更新收藏状态
    @Query("UPDATE outfits SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    // 更新穿着次数和最后穿着时间
    @Query("UPDATE outfits SET wearCount = wearCount + 1, lastWornAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementWearCount(id: String, timestamp: Long = System.currentTimeMillis())

    // 统计总数
    @Query("SELECT COUNT(*) FROM outfits")
    fun getTotalCount(): Flow<Int>

    // 查询包含某件衣物的搭配
    @Query("""
        SELECT * FROM outfits 
        WHERE topId = :clothingId 
           OR bottomId = :clothingId 
           OR outerId = :clothingId 
           OR shoesId = :clothingId 
           OR bagId = :clothingId 
           OR accessoryIds LIKE '%' || :clothingId || '%'
        ORDER BY createdAt DESC
    """)
    fun getOutfitsContainingClothing(clothingId: String): Flow<List<OutfitEntity>>
}
