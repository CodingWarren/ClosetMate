package com.closetmate.app.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.closetmate.app.data.backup.BackupManager
import com.closetmate.app.security.AppLockManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── 弹窗状态 ──────────────────────────────────────────────────────────────
    var showAboutDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // 备份/恢复状态
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // 应用锁状态
    val isLockEnabled by AppLockManager.isLockEnabled(context).collectAsState(initial = false)
    val isBiometricEnabled by AppLockManager.isBiometricEnabled(context).collectAsState(initial = false)

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinVerifyDialog by remember { mutableStateOf(false) }
    var pinVerifyAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // 生物识别可用性
    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // 文件选择器（用于恢复）
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 用户信息卡片 ──────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "我的衣橱",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "本地数据，隐私安全",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // ── 数据管理 ──────────────────────────────────────────────────────
            SettingsSection(title = "数据管理") {
                SettingsItem(
                    icon = Icons.Default.CloudUpload,
                    title = "备份数据",
                    subtitle = if (isBackingUp) "正在备份…" else "将衣橱数据打包为 ZIP 文件",
                    onClick = {
                        if (!isBackingUp) {
                            isBackingUp = true
                            scope.launch {
                                val uri = BackupManager.createBackup(context)
                                isBackingUp = false
                                if (uri != null) {
                                    BackupManager.shareBackup(context, uri)
                                } else {
                                    Toast.makeText(context, "备份失败，请重试", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    trailingContent = {
                        if (isBackingUp) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                SettingsItem(
                    icon = Icons.Default.CloudDownload,
                    title = "恢复数据",
                    subtitle = if (isRestoring) "正在恢复…" else "从备份 ZIP 文件恢复衣橱数据",
                    onClick = {
                        if (!isRestoring) restoreFileLauncher.launch("*/*")
                    },
                    trailingContent = {
                        if (isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "清空数据",
                    subtitle = "删除所有衣物和搭配记录",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearDataDialog = true }
                )
            }

            // ── 安全 ──────────────────────────────────────────────────────────
            SettingsSection(title = "安全") {
                // 应用锁开关
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isLockEnabled) {
                                // 关闭锁：需要先验证 PIN
                                pinVerifyAction = {
                                    scope.launch { AppLockManager.clearPin(context) }
                                }
                                showPinVerifyDialog = true
                            } else {
                                // 开启锁：先设置 PIN
                                showPinSetupDialog = true
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "应用锁",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isLockEnabled) "已启用 PIN 码保护" else "启用后需要 PIN 码或生物识别解锁",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showPinSetupDialog = true
                            } else {
                                pinVerifyAction = {
                                    scope.launch { AppLockManager.clearPin(context) }
                                }
                                showPinVerifyDialog = true
                            }
                        }
                    )
                }

                // 修改 PIN 码（仅在锁已启用时显示）
                if (isLockEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsItem(
                        icon = Icons.Default.Pin,
                        title = "修改 PIN 码",
                        subtitle = "更换当前 6 位 PIN 码",
                        onClick = {
                            pinVerifyAction = { showPinSetupDialog = true }
                            showPinVerifyDialog = true
                        }
                    )

                    // 生物识别（仅在设备支持时显示）
                    if (biometricAvailable) {
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        AppLockManager.setBiometricEnabled(context, !isBiometricEnabled)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "生物识别解锁",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "使用指纹或面部识别解锁",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        AppLockManager.setBiometricEnabled(context, enabled)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ── 显示设置 ──────────────────────────────────────────────────────
            SettingsSection(title = "显示设置") {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "主题颜色",
                    subtitle = "跟随系统",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "深色模式",
                    subtitle = "跟随系统",
                    onClick = { /* TODO */ }
                )
            }

            // ── 关于 ──────────────────────────────────────────────────────────
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "关于 ClosetMate",
                    subtitle = "版本 1.0.0",
                    onClick = { showAboutDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                SettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "隐私说明",
                    subtitle = "所有数据仅存储在本地设备",
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── 弹窗 ──────────────────────────────────────────────────────────────────

    // 关于弹窗
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Checkroom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("ClosetMate", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("版本：1.0.0")
                    Text("一款简洁好用的衣橱管理应用，帮助你整理衣物、规划搭配、追踪穿着记录。")
                    Text(
                        text = "所有数据均存储在本地，不上传任何个人信息。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("确定") }
            }
        )
    }

    // 清空数据确认弹窗
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("清空所有数据") },
            text = { Text("此操作将删除所有衣物和搭配记录，且无法恢复。确定要继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = { showClearDataDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("取消") }
            }
        )
    }

    // 恢复数据确认弹窗
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("恢复数据") },
            text = { Text("恢复操作将覆盖当前所有衣物和搭配数据，且无法撤销。\n\n恢复完成后 App 将自动重启。\n\n确定要继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        val uri = pendingRestoreUri ?: return@TextButton
                        pendingRestoreUri = null
                        isRestoring = true
                        scope.launch {
                            val success = BackupManager.restoreBackup(context, uri)
                            isRestoring = false
                            if (!success) {
                                Toast.makeText(context, "恢复失败，请检查备份文件是否正确", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("确认恢复") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingRestoreUri = null
                }) { Text("取消") }
            }
        )
    }

    // PIN 设置弹窗
    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onPinSet = { pin ->
                scope.launch {
                    AppLockManager.setPin(context, pin)
                    showPinSetupDialog = false
                    Toast.makeText(context, "PIN 码已设置，应用锁已启用", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // PIN 验证弹窗（用于关闭锁或修改 PIN 前的验证）
    if (showPinVerifyDialog) {
        PinVerifyDialog(
            onDismiss = {
                showPinVerifyDialog = false
                pinVerifyAction = null
            },
            onVerified = {
                showPinVerifyDialog = false
                pinVerifyAction?.invoke()
                pinVerifyAction = null
            }
        )
    }
}

// ── PIN 设置弹窗 ───────────────────────────────────────────────────────────────

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1 = 输入, 2 = 确认
    var firstPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text(if (step == 1) "设置 PIN 码" else "确认 PIN 码", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (step == 1) "请输入 6 位数字 PIN 码" else "请再次输入 PIN 码以确认",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            pin = input
                            errorMessage = ""
                        }
                    },
                    label = { Text("PIN 码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorMessage.isNotBlank(),
                    supportingText = if (errorMessage.isNotBlank()) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin.length != 6) {
                    errorMessage = "PIN 码必须为 6 位数字"
                    return@TextButton
                }
                if (step == 1) {
                    firstPin = pin
                    pin = ""
                    step = 2
                } else {
                    if (pin == firstPin) {
                        onPinSet(pin)
                    } else {
                        errorMessage = "两次输入不一致，请重试"
                        pin = ""
                        step = 1
                        firstPin = ""
                    }
                }
            }) {
                Text(if (step == 1) "下一步" else "确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ── PIN 验证弹窗 ───────────────────────────────────────────────────────────────

@Composable
private fun PinVerifyDialog(
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("验证 PIN 码", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "请输入当前 PIN 码以继续",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            pin = input
                            errorMessage = ""
                        }
                    },
                    label = { Text("PIN 码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorMessage.isNotBlank(),
                    supportingText = if (errorMessage.isNotBlank()) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val correct = AppLockManager.verifyPin(context, pin)
                    if (correct) {
                        onVerified()
                    } else {
                        errorMessage = "PIN 码错误，请重试"
                        pin = ""
                    }
                }
            }) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ── 通用组件 ──────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == MaterialTheme.colorScheme.onSurface)
                MaterialTheme.colorScheme.onSurfaceVariant
            else titleColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
