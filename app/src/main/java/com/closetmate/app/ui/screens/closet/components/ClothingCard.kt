package com.closetmate.app.ui.screens.closet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.closetmate.app.data.local.entity.ClothingEntity
import com.closetmate.app.data.local.entity.ClothingStatus
import com.closetmate.app.ui.theme.*

@Composable
fun ClothingCard(
    clothing: ClothingEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageUris = clothing.imageUris.split(",").filter { it.isNotBlank() }
    val firstImage = imageUris.firstOrNull()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // 图片区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                if (firstImage != null) {
                    AsyncImage(
                        model = firstImage,
                        contentDescription = clothing.category,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // 无图片占位
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getCategoryEmoji(clothing.category),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                // 状态标签（非正常状态时显示）
                if (clothing.status != ClothingStatus.NORMAL) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = getStatusColor(clothing.status).copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = clothing.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // 多图标识
                if (imageUris.size > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "1/${imageUris.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 信息区域
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = clothing.category,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (clothing.brand.isNotBlank()) {
                    Text(
                        text = clothing.brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 颜色标签
                val colors = clothing.colors.split(",").filter { it.isNotBlank() }
                if (colors.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        colors.take(2).forEach { color ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = color,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getCategoryEmoji(category: String): String = when (category) {
    "上衣", "T恤", "衬衫", "毛衣", "卫衣" -> "👕"
    "裤子" -> "👖"
    "裙子", "连衣裙" -> "👗"
    "外套", "大衣", "羽绒服" -> "🧥"
    "套装" -> "🤵"
    "内衣" -> "🩱"
    "运动服" -> "🏃"
    "鞋子" -> "👟"
    "包包" -> "👜"
    "配饰" -> "💍"
    else -> "👔"
}

fun getStatusColor(status: String) = when (status) {
    ClothingStatus.TO_WASH -> Warning
    ClothingStatus.TO_REPAIR -> Danger
    ClothingStatus.IDLE -> TextMuted
    ClothingStatus.DISPOSED -> TextMuted
    else -> Success
}
