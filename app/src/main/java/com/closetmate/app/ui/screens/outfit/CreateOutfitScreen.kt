package com.closetmate.app.ui.screens.outfit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.closetmate.app.data.local.entity.ClothingEntity
import com.closetmate.app.data.local.entity.OutfitEntity
import com.closetmate.app.data.local.entity.OutfitScene
import com.closetmate.app.data.repository.OutfitRepository
import com.closetmate.app.ui.screens.closet.components.getCategoryEmoji
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// 搭配槽位定义
data class OutfitSlot(
    val key: String,
    val label: String,
    val emoji: String,
    val categories: List<String>  // 该槽位接受的品类
)

val outfitSlots = listOf(
    OutfitSlot("top", "上衣", "👕", listOf("上衣", "T恤", "衬衫", "毛衣", "卫衣", "连衣裙", "套装", "运动服")),
    OutfitSlot("bottom", "下装", "👖", listOf("裤子", "裙子", "套装")),
    OutfitSlot("outer", "外套", "🧥", listOf("外套", "大衣", "羽绒服")),
    OutfitSlot("shoes", "鞋子", "👟", listOf("鞋子")),
    OutfitSlot("bag", "包包", "👜", listOf("包包")),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateOutfitScreen(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    editOutfitId: String? = null,
    viewModel: OutfitViewModel = viewModel()
) {
    val context = LocalContext.current
    val outfitRepository = remember { OutfitRepository(context) }
    val scope = rememberCoroutineScope()
    val allClothing by viewModel.allClothing.collectAsStateWithLifecycle()

    // 搭配槽位选择状态
    var selectedItems by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var outfitName by remember { mutableStateOf("") }
    var selectedScenes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // 当前正在选择的槽位
    var activeSlotKey by remember { mutableStateOf<String?>(null) }

    // 编辑模式：加载已有数据
    LaunchedEffect(editOutfitId) {
        if (editOutfitId != null) {
            val existing = outfitRepository.getById(editOutfitId)
            existing?.let { outfit ->
                outfitName = outfit.name
                selectedScenes = outfit.sceneTags.split(",").filter { it.isNotBlank() }.toSet()
                notes = outfit.notes
                val map = mutableMapOf<String, String>()
                if (outfit.topId.isNotBlank()) map["top"] = outfit.topId
                if (outfit.bottomId.isNotBlank()) map["bottom"] = outfit.bottomId
                if (outfit.outerId.isNotBlank()) map["outer"] = outfit.outerId
                if (outfit.shoesId.isNotBlank()) map["shoes"] = outfit.shoesId
                if (outfit.bagId.isNotBlank()) map["bag"] = outfit.bagId
                selectedItems = map
            }
        }
    }

    // 默认名称
    LaunchedEffect(Unit) {
        if (outfitName.isBlank() && editOutfitId == null) {
            val dateStr = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date())
            outfitName = "搭配 $dateStr"
        }
    }

    val hasAnyItem = selectedItems.isNotEmpty()

    fun save() {
        scope.launch {
            isSaving = true
            val outfit = if (editOutfitId != null) {
                val existing = outfitRepository.getById(editOutfitId)
                existing?.copy(
                    name = outfitName.ifBlank { "搭配" },
                    topId = selectedItems["top"] ?: "",
                    bottomId = selectedItems["bottom"] ?: "",
                    outerId = selectedItems["outer"] ?: "",
                    shoesId = selectedItems["shoes"] ?: "",
                    bagId = selectedItems["bag"] ?: "",
                    sceneTags = selectedScenes.joinToString(","),
                    notes = notes,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                OutfitEntity(
                    id = UUID.randomUUID().toString(),
                    name = outfitName.ifBlank { "搭配" },
                    topId = selectedItems["top"] ?: "",
                    bottomId = selectedItems["bottom"] ?: "",
                    outerId = selectedItems["outer"] ?: "",
                    shoesId = selectedItems["shoes"] ?: "",
                    bagId = selectedItems["bag"] ?: "",
                    sceneTags = selectedScenes.joinToString(",")
                )
            }
            outfit?.let {
                if (editOutfitId != null) outfitRepository.update(it)
                else outfitRepository.insert(it)
            }
            isSaving = false
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editOutfitId != null) "编辑搭配" else "创建搭配",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // 搭配名称
                OutlinedTextField(
                    value = outfitName,
                    onValueChange = { if (it.length <= 20) outfitName = it },
                    label = { Text("搭配名称") },
                    singleLine = true,
                    supportingText = { Text("${outfitName.length}/20") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 搭配槽位
                Text(
                    text = "选择单品",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                outfitSlots.forEach { slot ->
                    val selectedId = selectedItems[slot.key]
                    val selectedClothing = if (selectedId != null)
                        allClothing.find { it.id == selectedId } else null

                    OutfitSlotItem(
                        slot = slot,
                        selectedClothing = selectedClothing,
                        onClick = { activeSlotKey = slot.key },
                        onClear = {
                            selectedItems = selectedItems - slot.key
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 场景标签
                Text(
                    text = "场景标签",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutfitScene.all.forEach { scene ->
                        FilterChip(
                            selected = selectedScenes.contains(scene),
                            onClick = {
                                selectedScenes = if (selectedScenes.contains(scene))
                                    selectedScenes - scene else selectedScenes + scene
                            },
                            label = { Text(scene, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 备注
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 100) notes = it },
                    label = { Text("备注（选填）") },
                    maxLines = 3,
                    supportingText = { Text("${notes.length}/100") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 底部保存按钮
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = { save() },
                    enabled = hasAnyItem && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (editOutfitId != null) "保存修改" else "保存搭配")
                    }
                }
            }
        }
    }

    // 衣物选择底部弹窗
    if (activeSlotKey != null) {
        val slot = outfitSlots.find { it.key == activeSlotKey }!!
        val filteredClothing = allClothing.filter { clothing ->
            slot.categories.any { cat -> clothing.category == cat }
        }

        ClothingPickerSheet(
            slotLabel = slot.label,
            clothingList = filteredClothing,
            selectedId = selectedItems[slot.key],
            onSelect = { clothingId ->
                selectedItems = selectedItems + (slot.key to clothingId)
                activeSlotKey = null
            },
            onDismiss = { activeSlotKey = null }
        )
    }
}

@Composable
private fun OutfitSlotItem(
    slot: OutfitSlot,
    selectedClothing: ClothingEntity?,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图片/占位
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (selectedClothing != null) {
                    val imageUri = selectedClothing.imageUris.split(",").firstOrNull { it.isNotBlank() }
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = getCategoryEmoji(selectedClothing.category),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                } else {
                    Text(
                        text = slot.emoji,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selectedClothing != null) {
                    Text(
                        text = selectedClothing.category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (selectedClothing.brand.isNotBlank()) {
                        Text(
                            text = selectedClothing.brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "点击选择${slot.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedClothing != null) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClothingPickerSheet(
    slotLabel: String,
    clothingList: List<ClothingEntity>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择${slotLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            if (clothingList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "👕", style = MaterialTheme.typography.headlineLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无${slotLabel}类衣物",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(clothingList) { clothing ->
                        val isSelected = clothing.id == selectedId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(clothing.id) }
                                .then(
                                    if (isSelected) Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(10.dp)
                                    ) else Modifier
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val imageUri = clothing.imageUris.split(",")
                                        .firstOrNull { it.isNotBlank() }
                                    if (imageUri != null) {
                                        AsyncImage(
                                            model = imageUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = getCategoryEmoji(clothing.category),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = clothing.category,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val tags = buildList {
                                        if (clothing.brand.isNotBlank()) add(clothing.brand)
                                        val colors = clothing.colors.split(",").firstOrNull { it.isNotBlank() }
                                        if (colors != null) add(colors)
                                    }.joinToString(" · ")
                                    if (tags.isNotBlank()) {
                                        Text(
                                            text = tags,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
