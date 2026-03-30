package com.closetmate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfits")
data class OutfitEntity(
    @PrimaryKey
    val id: String,                        // UUID
    val name: String,                      // 搭配名称（最多20字）
    val topId: String = "",                // 上衣 ID
    val bottomId: String = "",             // 下装 ID
    val outerId: String = "",              // 外套 ID
    val shoesId: String = "",              // 鞋子 ID
    val bagId: String = "",                // 包包 ID
    val accessoryIds: String = "",         // 配饰 IDs（逗号分隔，可多件）
    val sceneTags: String = "",            // 场景标签（逗号分隔）
    val isFavorite: Boolean = false,       // 是否收藏
    val wearCount: Int = 0,                // 使用次数
    val lastWornAt: Long = 0L,             // 最后使用时间
    val notes: String = "",                // 备注（最多100字）
    val ownerId: String = "default",       // 归属用户
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 搭配场景标签
object OutfitScene {
    const val COMMUTE = "通勤"
    const val DATE = "约会"
    const val SPORT = "运动"
    const val CASUAL = "休闲"
    const val FORMAL = "正式"
    const val TRAVEL = "旅行"
    const val OTHER = "其他"

    val all = listOf(COMMUTE, DATE, SPORT, CASUAL, FORMAL, TRAVEL, OTHER)
}
