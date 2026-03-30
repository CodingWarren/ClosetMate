package com.closetmate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clothing")
data class ClothingEntity(
    @PrimaryKey
    val id: String,                    // UUID
    val imageUris: String,             // JSON array of image URIs (逗号分隔)
    val category: String,              // 品类
    val seasons: String,               // 季节 (逗号分隔: 春,夏,秋,冬)
    val colors: String,                // 颜色 (逗号分隔)
    val styles: String,                // 风格 (逗号分隔)
    val brand: String = "",            // 品牌
    val price: Double = 0.0,           // 价格
    val purchaseChannel: String = "",  // 购买渠道
    val purchaseDate: String = "",     // 购买日期 (YYYY-MM-DD)
    val storageLocation: String = "",  // 存放位置
    val status: String = "正常",       // 状态: 正常/待洗/待修复/闲置/已处置
    val wearCount: Int = 0,            // 穿着次数
    val lastWornAt: Long = 0L,         // 最后穿着时间 (timestamp)
    val notes: String = "",            // 备注
    val ownerId: String = "default",   // 归属用户
    val isShared: Boolean = false,     // 是否公共衣物
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 品类枚举
object ClothingCategory {
    const val TOP = "上衣"
    const val T_SHIRT = "T恤"
    const val SHIRT = "衬衫"
    const val SWEATER = "毛衣"
    const val HOODIE = "卫衣"
    const val PANTS = "裤子"
    const val SKIRT = "裙子"
    const val JACKET = "外套"
    const val COAT = "大衣"
    const val DOWN_JACKET = "羽绒服"
    const val DRESS = "连衣裙"
    const val SUIT = "套装"
    const val UNDERWEAR = "内衣"
    const val SPORTSWEAR = "运动服"
    const val SHOES = "鞋子"
    const val BAG = "包包"
    const val ACCESSORY = "配饰"
    const val OTHER = "其他"

    val all = listOf(
        TOP, T_SHIRT, SHIRT, SWEATER, HOODIE, PANTS, SKIRT,
        JACKET, COAT, DOWN_JACKET, DRESS, SUIT, UNDERWEAR,
        SPORTSWEAR, SHOES, BAG, ACCESSORY, OTHER
    )
}

// 季节枚举
object ClothingSeason {
    const val SPRING = "春"
    const val SUMMER = "夏"
    const val AUTUMN = "秋"
    const val WINTER = "冬"
    const val ALL_SEASON = "四季"

    val all = listOf(SPRING, SUMMER, AUTUMN, WINTER, ALL_SEASON)
}

// 风格枚举
object ClothingStyle {
    const val COMMUTE = "通勤"
    const val CASUAL = "休闲"
    const val SPORT = "运动"
    const val FORMAL = "正式"
    const val DATE = "约会"
    const val OUTDOOR = "户外"
    const val VACATION = "度假"
    const val STREET = "街头"
    const val VINTAGE = "复古"
    const val MINIMAL = "简约"
    const val OTHER = "其他"

    val all = listOf(COMMUTE, CASUAL, SPORT, FORMAL, DATE, OUTDOOR, VACATION, STREET, VINTAGE, MINIMAL, OTHER)
}

// 衣物状态枚举
object ClothingStatus {
    const val NORMAL = "正常"
    const val TO_WASH = "待洗"
    const val TO_REPAIR = "待修复"
    const val IDLE = "闲置"
    const val DISPOSED = "已处置"

    val all = listOf(NORMAL, TO_WASH, TO_REPAIR, IDLE, DISPOSED)
}
