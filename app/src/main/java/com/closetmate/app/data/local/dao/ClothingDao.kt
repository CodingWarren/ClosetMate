package com.closetmate.app.data.local.dao

import androidx.room.*
import com.closetmate.app.data.local.entity.ClothingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClothingDao {

    // 插入衣物
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clothing: ClothingEntity)

    // 更新衣物
    @Update
    suspend fun update(clothing: ClothingEntity)

    // 删除衣物
    @Delete
    suspend fun delete(clothing: ClothingEntity)

    // 根据ID删除
    @Query("DELETE FROM clothing WHERE id = :id")
    suspend fun deleteById(id: String)

    // 查询所有衣物（按创建时间倒序）
    @Query("SELECT * FROM clothing WHERE status != '已处置' ORDER BY createdAt DESC")
    fun getAllClothing(): Flow<List<ClothingEntity>>

    // 一次性查询所有衣物（用于迁移，不含 Flow）
    @Query("SELECT * FROM clothing")
    suspend fun getAllOnce(): List<ClothingEntity>

    // 根据ID查询
    @Query("SELECT * FROM clothing WHERE id = :id")
    suspend fun getById(id: String): ClothingEntity?

    // 按品类筛选
    @Query("SELECT * FROM clothing WHERE category = :category AND status != '已处置' ORDER BY createdAt DESC")
    fun getByCategory(category: String): Flow<List<ClothingEntity>>

    // 按状态筛选
    @Query("SELECT * FROM clothing WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: String): Flow<List<ClothingEntity>>

    // 按季节筛选（seasons字段包含该季节）
    @Query("SELECT * FROM clothing WHERE seasons LIKE '%' || :season || '%' AND status != '已处置' ORDER BY createdAt DESC")
    fun getBySeason(season: String): Flow<List<ClothingEntity>>

    // 关键词搜索（品牌/备注/存放位置）
    @Query("""
        SELECT * FROM clothing 
        WHERE (brand LIKE '%' || :keyword || '%' 
            OR notes LIKE '%' || :keyword || '%' 
            OR storageLocation LIKE '%' || :keyword || '%')
        AND status != '已处置'
        ORDER BY createdAt DESC
    """)
    fun search(keyword: String): Flow<List<ClothingEntity>>

    // 查询闲置衣物（超过指定天数未穿）
    @Query("""
        SELECT * FROM clothing 
        WHERE (lastWornAt < :beforeTimestamp OR lastWornAt = 0) 
        AND status != '已处置'
        ORDER BY lastWornAt ASC
    """)
    fun getIdleClothing(beforeTimestamp: Long): Flow<List<ClothingEntity>>

    // 更新穿着次数和最后穿着时间
    @Query("UPDATE clothing SET wearCount = wearCount + 1, lastWornAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementWearCount(id: String, timestamp: Long = System.currentTimeMillis())

    // 更新衣物状态
    @Query("UPDATE clothing SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    // 统计总数
    @Query("SELECT COUNT(*) FROM clothing WHERE status != '已处置'")
    fun getTotalCount(): Flow<Int>

    // 按品类统计数量
    @Query("SELECT category, COUNT(*) as count FROM clothing WHERE status != '已处置' GROUP BY category")
    fun getCategoryStats(): Flow<List<CategoryCount>>

    // 统计总消费金额
    @Query("SELECT SUM(price) FROM clothing WHERE status != '已处置'")
    fun getTotalSpending(): Flow<Double?>
}

// 品类统计数据类
data class CategoryCount(
    val category: String,
    val count: Int
)
