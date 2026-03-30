package com.closetmate.app.security

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

/** DataStore 实例（每个 Context 只创建一次） */
val Context.lockDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_lock")

/**
 * 应用锁管理器
 *
 * 功能：
 *   - 6 位 PIN 码（SHA-256 哈希存储）
 *   - 生物识别（Face ID / 指纹）
 *   - 内存中的锁定状态（isLocked）
 *
 * 使用方式：
 *   - 在 MainActivity.onStart 中调用 lockIfEnabled()
 *   - 在 Compose 中观察 isLocked，显示 LockScreen 覆盖层
 *   - 验证成功后调用 unlock()
 */
object AppLockManager {

    private val KEY_LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
    private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
    private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")

    // ── 内存锁定状态 ──────────────────────────────────────────────────────────

    /** true = 当前处于锁定状态，需要验证 */
    val isLocked = mutableStateOf(false)

    // ── 设置读取（Flow） ───────────────────────────────────────────────────────

    fun isLockEnabled(context: Context): Flow<Boolean> =
        context.lockDataStore.data.map { it[KEY_LOCK_ENABLED] ?: false }

    fun isBiometricEnabled(context: Context): Flow<Boolean> =
        context.lockDataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }

    fun hasPinSet(context: Context): Flow<Boolean> =
        context.lockDataStore.data.map { (it[KEY_PIN_HASH] ?: "").isNotBlank() }

    // ── 设置写入 ──────────────────────────────────────────────────────────────

    /**
     * 设置 PIN 码并自动启用应用锁
     * @param pin 6 位数字字符串
     */
    suspend fun setPin(context: Context, pin: String) {
        context.lockDataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hashPin(pin)
            prefs[KEY_LOCK_ENABLED] = true
        }
    }

    /**
     * 清除 PIN 码并禁用应用锁（同时关闭生物识别）
     */
    suspend fun clearPin(context: Context) {
        context.lockDataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
            prefs[KEY_LOCK_ENABLED] = false
            prefs[KEY_BIOMETRIC_ENABLED] = false
        }
        isLocked.value = false
    }

    /**
     * 启用或禁用生物识别解锁
     */
    suspend fun setBiometricEnabled(context: Context, enabled: Boolean) {
        context.lockDataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    // ── PIN 验证 ──────────────────────────────────────────────────────────────

    /**
     * 验证 PIN 码
     * @return true = 正确
     */
    suspend fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.lockDataStore.data.first()
        val storedHash = prefs[KEY_PIN_HASH] ?: return false
        return hashPin(pin) == storedHash
    }

    // ── 锁定 / 解锁 ───────────────────────────────────────────────────────────

    /**
     * 如果应用锁已启用，则锁定 App（在 onStart 中调用）
     */
    suspend fun lockIfEnabled(context: Context) {
        val enabled = context.lockDataStore.data.first()[KEY_LOCK_ENABLED] ?: false
        if (enabled) {
            isLocked.value = true
        }
    }

    /** 解锁 App */
    fun unlock() {
        isLocked.value = false
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
