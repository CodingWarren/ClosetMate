package com.closetmate.app.ui.screens.closet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.closetmate.app.data.local.entity.*
import com.closetmate.app.data.repository.ClothingRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** 图片压缩目标大小：500KB */
private const val MAX_IMAGE_SIZE_BYTES = 500 * 1024L

/** 图片最大边长（超过则等比缩放） */
private const val MAX_IMAGE_DIMENSION = 2048

/**
 * 将任意 URI（content:// 或 file://）复制到 App 私有目录，并自动压缩至 500KB 以内。
 * 返回持久化的 file:// URI。
 */
fun copyUriToPrivateStorage(context: Context, sourceUri: Uri): Uri {
    val dir = File(context.filesDir, "clothing_images").also { it.mkdirs() }
    val destFile = File(dir, "img_${System.currentTimeMillis()}_${(0..9999).random()}.jpg")

    // 1. 解码原始 Bitmap（先只读尺寸，避免 OOM）
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(sourceUri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }

    // 2. 计算采样率（inSampleSize），将图片缩放到 MAX_IMAGE_DIMENSION 以内
    val sampleSize = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bitmap = context.contentResolver.openInputStream(sourceUri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOptions)
    } ?: run {
        // 解码失败时直接复制原文件
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        }
        return Uri.fromFile(destFile)
    }

    // 3. 逐步降低 JPEG 质量，直到文件大小 ≤ 500KB
    var quality = 90
    do {
        val byteArray = java.io.ByteArrayOutputStream().also { bos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
        }.toByteArray()

        if (byteArray.size <= MAX_IMAGE_SIZE_BYTES || quality <= 40) {
            FileOutputStream(destFile).use { it.write(byteArray) }
            break
        }
        quality -= 10
    } while (true)

    bitmap.recycle()
    return Uri.fromFile(destFile)
}

/**
 * 计算 BitmapFactory 的 inSampleSize，使解码后的图片不超过 [reqWidth] x [reqHeight]。
 */
private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClothingScreen(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    editClothingId: String? = null
) {
    val context = LocalContext.current
    val repository = remember { ClothingRepository(context) }
    val scope = rememberCoroutineScope()

    // 步骤状态
    var currentStep by remember { mutableIntStateOf(1) }

    // 步骤1：图片
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // 步骤2：基础信息
    var selectedCategory by remember { mutableStateOf("") }
    var selectedSeasons by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedColors by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedStyles by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 步骤3：详细信息
    var brand by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedPurchaseChannel by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf("") }
    var storageLocation by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(ClothingStatus.NORMAL) }
    var notes by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }

    // 编辑模式：加载已有数据
    LaunchedEffect(editClothingId) {
        if (editClothingId != null) {
            val existing = repository.getById(editClothingId)
            existing?.let { item ->
                selectedImageUris = item.imageUris.split(",")
                    .filter { it.isNotBlank() }
                    .map { Uri.parse(it) }
                selectedCategory = item.category
                selectedSeasons = item.seasons.split(",").filter { it.isNotBlank() }.toSet()
                selectedColors = item.colors.split(",").filter { it.isNotBlank() }.toSet()
                selectedStyles = item.styles.split(",").filter { it.isNotBlank() }.toSet()
                brand = item.brand
                price = if (item.price > 0) item.price.toString() else ""
                selectedPurchaseChannel = item.purchaseChannel
                purchaseDate = item.purchaseDate
                storageLocation = item.storageLocation
                selectedStatus = item.status
                notes = item.notes
            }
        }
    }

    // 图片选择器：选完后立即复制到私有目录
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val copied = uris.map { copyUriToPrivateStorage(context, it) }
        val combined = (selectedImageUris + copied).distinctBy { it.toString() }.take(5)
        selectedImageUris = combined
    }

    // 相机拍照（临时 URI，拍完后复制到私有目录）
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            // 将相机临时文件复制到私有目录
            val persistentUri = copyUriToPrivateStorage(context, cameraImageUri!!)
            selectedImageUris = (selectedImageUris + persistentUri).take(5)
        }
    }

    // 相机权限申请
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val cacheDir = File(context.cacheDir, "camera_images").also { it.mkdirs() }
            val imageFile = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            val cacheDir = File(context.cacheDir, "camera_images").also { it.mkdirs() }
            val imageFile = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val isStep2Valid = selectedCategory.isNotBlank() &&
            selectedSeasons.isNotEmpty() &&
            selectedColors.isNotEmpty() &&
            selectedStyles.isNotEmpty()

    fun save() {
        scope.launch {
            isSaving = true
            val imageUrisStr = selectedImageUris.joinToString(",") { it.toString() }
            val clothing = if (editClothingId != null) {
                val existing = repository.getById(editClothingId)
                existing?.copy(
                    imageUris = imageUrisStr,
                    category = selectedCategory,
                    seasons = selectedSeasons.joinToString(","),
                    colors = selectedColors.joinToString(","),
                    styles = selectedStyles.joinToString(","),
                    brand = brand,
                    price = price.toDoubleOrNull() ?: 0.0,
                    purchaseChannel = selectedPurchaseChannel,
                    purchaseDate = purchaseDate,
                    storageLocation = storageLocation,
                    status = selectedStatus,
                    notes = notes,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                ClothingEntity(
                    id = UUID.randomUUID().toString(),
                    imageUris = imageUrisStr,
                    category = selectedCategory,
                    seasons = selectedSeasons.joinToString(","),
                    colors = selectedColors.joinToString(","),
                    styles = selectedStyles.joinToString(","),
                    brand = brand,
                    price = price.toDoubleOrNull() ?: 0.0,
                    purchaseChannel = selectedPurchaseChannel,
                    purchaseDate = purchaseDate,
                    storageLocation = storageLocation,
                    status = selectedStatus,
                    notes = notes
                )
            }
            clothing?.let {
                if (editClothingId != null) repository.update(it)
                else repository.insert(it)
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
                        text = if (editClothingId != null) "编辑衣物" else "添加衣物",
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
            // 步骤指示器
            StepIndicator(currentStep = currentStep, totalSteps = 3)

            // 步骤内容
            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    1 -> Step1ImagePicker(
                        selectedUris = selectedImageUris,
                        onPickFromGallery = { imagePickerLauncher.launch("image/*") },
                        onTakePhoto = { launchCamera() },
                        onRemoveImage = { uri ->
                            selectedImageUris = selectedImageUris.filter { it != uri }
                        }
                    )

                    2 -> Step2BasicInfo(
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        selectedSeasons = selectedSeasons,
                        onSeasonsChange = { selectedSeasons = it },
                        selectedColors = selectedColors,
                        onColorsChange = { selectedColors = it },
                        selectedStyles = selectedStyles,
                        onStylesChange = { selectedStyles = it }
                    )

                    3 -> Step3DetailInfo(
                        brand = brand,
                        onBrandChange = { brand = it },
                        price = price,
                        onPriceChange = { price = it },
                        selectedPurchaseChannel = selectedPurchaseChannel,
                        onPurchaseChannelChange = { selectedPurchaseChannel = it },
                        purchaseDate = purchaseDate,
                        onPurchaseDateChange = { purchaseDate = it },
                        storageLocation = storageLocation,
                        onStorageLocationChange = { storageLocation = it },
                        selectedStatus = selectedStatus,
                        onStatusChange = { selectedStatus = it },
                        notes = notes,
                        onNotesChange = { notes = it }
                    )
                }
            }

            // 底部按钮
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("上一步")
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < 3) currentStep++
                            else save()
                        },
                        enabled = when (currentStep) {
                            2 -> isStep2Valid
                            else -> true
                        } && !isSaving,
                        modifier = Modifier.weight(if (currentStep > 1) 1f else 1f)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                when (currentStep) {
                                    3 -> if (editClothingId != null) "保存修改" else "完成添加"
                                    else -> "下一步"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 步骤指示器 ───────────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(currentStep: Int, @Suppress("UNUSED_PARAMETER") totalSteps: Int) {
    val stepLabels = listOf("上传图片", "基础信息", "详细信息")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stepLabels.forEachIndexed { index, label ->
            val step = index + 1
            val isActive = step == currentStep
            val isDone = step < currentStep

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when {
                        isDone -> MaterialTheme.colorScheme.primary
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isDone) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = step.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive || isDone) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
    HorizontalDivider()
}

// ─── 步骤1：图片上传 ──────────────────────────────────────────────────────────

@Composable
private fun Step1ImagePicker(
    selectedUris: List<Uri>,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: (Uri) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "上传衣物图片",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "最多5张，支持拍照或从相册选择（可跳过）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // 图片网格
        val slots = 5
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 已选图片
            selectedUris.take(slots).forEach { uri ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(3f / 4f)
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                    IconButton(
                        onClick = { onRemoveImage(uri) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp)
                            .background(
                                MaterialTheme.colorScheme.error,
                                RoundedCornerShape(50)
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            // 添加按钮（未满5张时显示）
            if (selectedUris.size < slots) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(onClick = onPickFromGallery),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "添加图片",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "添加",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // 空白占位
                repeat(slots - selectedUris.size - 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 操作按钮行：拍照 + 相册
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onTakePhoto,
                enabled = selectedUris.size < 5,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("拍照")
            }
            OutlinedButton(
                onClick = onPickFromGallery,
                enabled = selectedUris.size < 5,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("相册")
            }
        }

        if (selectedUris.isEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "图片为可选项，可以先填写信息，之后再补充图片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

// ─── 步骤2：基础信息 ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun Step2BasicInfo(
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedSeasons: Set<String>,
    onSeasonsChange: (Set<String>) -> Unit,
    selectedColors: Set<String>,
    onColorsChange: (Set<String>) -> Unit,
    selectedStyles: Set<String>,
    onStylesChange: (Set<String>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "基础信息",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "以下字段均为必填",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // 品类（单选）
        FormSection(title = "品类 *") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClothingCategory.all.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategoryChange(category) },
                        label = { Text(category, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 季节（多选）
        FormSection(title = "季节 *（可多选）") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClothingSeason.all.forEach { season ->
                    FilterChip(
                        selected = selectedSeasons.contains(season),
                        onClick = {
                            val newSet = if (selectedSeasons.contains(season))
                                selectedSeasons - season else selectedSeasons + season
                            onSeasonsChange(newSet)
                        },
                        label = { Text(season, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 颜色（多选）
        val colorOptions = listOf(
            "白", "黑", "灰", "米", "红", "粉", "橙", "黄", "绿", "蓝", "紫", "棕", "花纹", "条纹", "格纹"
        )
        FormSection(title = "颜色 *（可多选）") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorOptions.forEach { color ->
                    FilterChip(
                        selected = selectedColors.contains(color),
                        onClick = {
                            val newSet = if (selectedColors.contains(color))
                                selectedColors - color else selectedColors + color
                            onColorsChange(newSet)
                        },
                        label = { Text(color, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 风格（多选）
        FormSection(title = "风格 *（可多选）") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClothingStyle.all.forEach { style ->
                    FilterChip(
                        selected = selectedStyles.contains(style),
                        onClick = {
                            val newSet = if (selectedStyles.contains(style))
                                selectedStyles - style else selectedStyles + style
                            onStylesChange(newSet)
                        },
                        label = { Text(style, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ─── 步骤3：详细信息 ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun Step3DetailInfo(
    brand: String,
    onBrandChange: (String) -> Unit,
    price: String,
    onPriceChange: (String) -> Unit,
    selectedPurchaseChannel: String,
    onPurchaseChannelChange: (String) -> Unit,
    purchaseDate: String,
    onPurchaseDateChange: (String) -> Unit,
    storageLocation: String,
    onStorageLocationChange: (String) -> Unit,
    selectedStatus: String,
    onStatusChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    val purchaseChannels = listOf(
        "实体店", "淘宝", "天猫", "京东", "拼多多", "抖音", "小红书", "海外购", "二手", "礼物", "其他"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "详细信息",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "以下字段均为选填",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // 品牌
        OutlinedTextField(
            value = brand,
            onValueChange = onBrandChange,
            label = { Text("品牌") },
            placeholder = { Text("如：优衣库、ZARA") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 价格
        OutlinedTextField(
            value = price,
            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) onPriceChange(it) },
            label = { Text("购买价格（元）") },
            placeholder = { Text("0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 购买渠道
        FormSection(title = "购买渠道") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                purchaseChannels.forEach { channel ->
                    FilterChip(
                        selected = selectedPurchaseChannel == channel,
                        onClick = {
                            onPurchaseChannelChange(
                                if (selectedPurchaseChannel == channel) "" else channel
                            )
                        },
                        label = { Text(channel, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 购买日期
        OutlinedTextField(
            value = purchaseDate,
            onValueChange = onPurchaseDateChange,
            label = { Text("购买日期") },
            placeholder = { Text("YYYY-MM-DD") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 存放位置
        OutlinedTextField(
            value = storageLocation,
            onValueChange = onStorageLocationChange,
            label = { Text("存放位置") },
            placeholder = { Text("如：衣柜第二层") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 衣物状态
        FormSection(title = "衣物状态") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClothingStatus.all.filter { it != ClothingStatus.DISPOSED }.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { onStatusChange(status) },
                        label = { Text(status, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 备注
        OutlinedTextField(
            value = notes,
            onValueChange = { if (it.length <= 200) onNotesChange(it) },
            label = { Text("备注") },
            placeholder = { Text("添加备注（最多200字）") },
            maxLines = 4,
            supportingText = { Text("${notes.length}/200") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ─── 通用表单区块 ─────────────────────────────────────────────────────────────

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 10.dp)
    )
    content()
}
