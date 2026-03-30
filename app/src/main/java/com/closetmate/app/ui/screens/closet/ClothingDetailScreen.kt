package com.closetmate.app.ui.screens.closet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.closetmate.app.data.local.entity.ClothingEntity
import com.closetmate.app.data.local.entity.ClothingStatus
import com.closetmate.app.data.repository.ClothingRepository
import com.closetmate.app.ui.screens.closet.components.getCategoryEmoji
import com.closetmate.app.ui.screens.closet.components.getStatusColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ClothingDetailScreen(
    clothingId: String,
    onNavigateBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ClothingRepository(context) }
    val scope = rememberCoroutineScope()

    var clothing by remember { mutableStateOf<ClothingEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var isWearMarked by remember { mutableStateOf(false) }

    LaunchedEffect(clothingId) {
        clothing = repository.getById(clothingId)
    }

    // 刷新数据
    fun refresh() {
        scope.launch {
            clothing = repository.getById(clothingId)
        }
    }

    val item = clothing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("衣物详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 标记穿着
                    IconButton(
                        onClick = {
                            item?.let {
                                scope.launch {
                                    repository.incrementWearCount(it.id)
                                    isWearMarked = true
                                    refresh()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isWearMarked) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                            contentDescription = "标记穿着",
                            tint = if (isWearMarked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // 编辑
                    IconButton(onClick = { item?.let { onEdit(it.id) } }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    // 更多操作
                    Box {
                        IconButton(onClick = { showStatusMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            Text(
                                text = "修改状态",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            ClothingStatus.all.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status) },
                                    onClick = {
                                        item?.let {
                                            scope.launch {
                                                repository.updateStatus(it.id, status)
                                                refresh()
                                            }
                                        }
                                        showStatusMenu = false
                                    },
                                    leadingIcon = {
                                        if (item?.status == status) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "删除衣物",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showStatusMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // 图片轮播
                val imageUris = item.imageUris.split(",").filter { it.isNotBlank() }
                if (imageUris.isNotEmpty()) {
                    val pagerState = rememberPagerState { imageUris.size }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = imageUris[page],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // 页码指示器
                        if (imageUris.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(imageUris.size) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (pagerState.currentPage == index)
                                                    Color.White
                                                else
                                                    Color.White.copy(alpha = 0.5f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 无图片占位
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getCategoryEmoji(item.category),
                            style = MaterialTheme.typography.displayLarge
                        )
                    }
                }

                // 穿着标记提示
                if (isWearMarked) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "已标记今日穿着，穿着次数 +1",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // 详情内容
                Column(modifier = Modifier.padding(20.dp)) {
                    // 标题行：品类 + 状态
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = getStatusColor(item.status).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = item.status,
                                style = MaterialTheme.typography.labelMedium,
                                color = getStatusColor(item.status),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (item.brand.isNotBlank()) {
                        Text(
                            text = item.brand,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 穿着统计
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatChip(
                            label = "穿着次数",
                            value = "${item.wearCount} 次"
                        )
                        if (item.lastWornAt > 0) {
                            val daysAgo = ((System.currentTimeMillis() - item.lastWornAt) /
                                    (1000 * 60 * 60 * 24)).toInt()
                            StatChip(
                                label = "上次穿着",
                                value = if (daysAgo == 0) "今天" else "${daysAgo} 天前"
                            )
                        }
                        if (item.price > 0 && item.wearCount > 0) {
                            StatChip(
                                label = "每次成本",
                                value = "¥${String.format("%.1f", item.price / item.wearCount)}"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(20.dp))

                    // 基础属性
                    DetailSection(title = "基础信息") {
                        DetailRow("季节", item.seasons.replace(",", " · "))
                        DetailRow("颜色", item.colors.replace(",", " · "))
                        DetailRow("风格", item.styles.replace(",", " · "))
                    }

                    // 购买信息
                    val hasPurchaseInfo = item.price > 0 || item.purchaseChannel.isNotBlank() ||
                            item.purchaseDate.isNotBlank()
                    if (hasPurchaseInfo) {
                        Spacer(modifier = Modifier.height(16.dp))
                        DetailSection(title = "购买信息") {
                            if (item.price > 0) DetailRow("价格", "¥${item.price}")
                            if (item.purchaseChannel.isNotBlank()) DetailRow("渠道", item.purchaseChannel)
                            if (item.purchaseDate.isNotBlank()) DetailRow("日期", item.purchaseDate)
                        }
                    }

                    // 存放信息
                    if (item.storageLocation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        DetailSection(title = "存放信息") {
                            DetailRow("位置", item.storageLocation)
                        }
                    }

                    // 备注
                    if (item.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        DetailSection(title = "备注") {
                            Text(
                                text = item.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 创建时间
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    Text(
                        text = "添加于 ${dateFormat.format(Date(item.createdAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 底部操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("删除")
                        }
                        Button(
                            onClick = { onEdit(item.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("编辑")
                        }
                    }
                }
            }
        }
    }

    // 删除确认弹窗
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除衣物") },
            text = { Text("确定要删除这件衣物吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        item?.let {
                            scope.launch {
                                repository.delete(it)
                                onDeleted()
                            }
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 10.dp)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.65f)
        )
    }
}
