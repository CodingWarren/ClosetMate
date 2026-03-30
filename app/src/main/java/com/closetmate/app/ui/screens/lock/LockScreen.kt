package com.closetmate.app.ui.screens.lock

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.closetmate.app.security.AppLockManager
import kotlinx.coroutines.launch

private const val PIN_LENGTH = 6

/**
 * 应用锁屏幕
 *
 * @param showBiometric 是否显示生物识别按钮
 * @param onBiometricRequest 点击生物识别按钮时的回调（由 MainActivity 处理实际弹窗）
 */
@Composable
fun LockScreen(
    showBiometric: Boolean = false,
    onBiometricRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {
            // ── 标题区域 ──────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🔒", fontSize = 52.sp)
                Text(
                    text = "ClosetMate",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "请输入 6 位 PIN 码解锁",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── PIN 点状指示器 ─────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(PIN_LENGTH) { index ->
                        val filled = index < pin.length
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                errorMessage.isNotBlank() -> MaterialTheme.colorScheme.error
                                filled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            label = "dot_color_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ── 数字键盘 ──────────────────────────────────────────────────────
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("bio", "0", "del")
                )
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "del" -> PinKeyButton(onClick = {
                                        if (pin.isNotEmpty()) {
                                            pin = pin.dropLast(1)
                                            errorMessage = ""
                                        }
                                    }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "删除",
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    "bio" -> {
                                        if (showBiometric) {
                                            PinKeyButton(onClick = onBiometricRequest) {
                                                Icon(
                                                    Icons.Default.Fingerprint,
                                                    contentDescription = "生物识别",
                                                    modifier = Modifier.size(28.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(68.dp))
                                        }
                                    }

                                    else -> PinKeyButton(onClick = {
                                        if (pin.length < PIN_LENGTH) {
                                            pin += key
                                            errorMessage = ""
                                            // 输满 6 位后自动验证
                                            if (pin.length == PIN_LENGTH) {
                                                val enteredPin = pin
                                                scope.launch {
                                                    val correct = AppLockManager.verifyPin(context, enteredPin)
                                                    if (correct) {
                                                        AppLockManager.unlock()
                                                    } else {
                                                        errorMessage = "PIN 码错误，请重试"
                                                        pin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Medium
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
}

@Composable
private fun PinKeyButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(68.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
