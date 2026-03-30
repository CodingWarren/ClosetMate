package com.closetmate.app.data.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.closetmate.app.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 数据备份与恢复管理器
 *
 * 备份内容：
 *   database/ → Room 数据库文件（.db / .db-shm / .db-wal）
 *   images/   → 衣物图片（files/clothing_images/）
 *
 * 备份文件格式：ClosetMate_Backup_YYYYMMDD_HHmmss.zip
 */
object BackupManager {

    private const val DB_NAME = "closetmate_database"
    private const val AUTHORITY = "com.closetmate.app.fileprovider"

    // ZIP 内部目录名
    private const val ZIP_DB_DIR = "database/"
    private const val ZIP_IMG_DIR = "images/"

    // ─────────────────────────────────────────────────────────────────────────
    // 备份
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 创建备份 ZIP 文件，返回可用于分享的 FileProvider URI。
     * 失败时返回 null。
     */
    suspend fun createBackup(context: Context): Uri? = withContext(Dispatchers.IO) {
        try {
            // 1. 将 WAL 日志合并到主数据库文件，确保数据完整
            runCatching {
                AppDatabase.getInstance(context)
                    .openHelper.writableDatabase
                    .execSQL("PRAGMA wal_checkpoint(FULL)")
            }

            // 2. 准备输出目录
            val backupDir = File(context.cacheDir, "backup").also { it.mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val zipFile = File(backupDir, "ClosetMate_Backup_$timestamp.zip")

            // 3. 打包
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // 3a. 数据库文件
                val dbFile = context.getDatabasePath(DB_NAME)
                listOf(dbFile, File("${dbFile.path}-shm"), File("${dbFile.path}-wal"))
                    .filter { it.exists() }
                    .forEach { file ->
                        zos.putNextEntry(ZipEntry("$ZIP_DB_DIR${file.name}"))
                        FileInputStream(file).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }

                // 3b. 图片文件
                val imagesDir = File(context.filesDir, "clothing_images")
                if (imagesDir.exists()) {
                    imagesDir.listFiles()?.forEach { imgFile ->
                        zos.putNextEntry(ZipEntry("$ZIP_IMG_DIR${imgFile.name}"))
                        FileInputStream(imgFile).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }

            // 4. 返回 FileProvider URI（供分享使用）
            FileProvider.getUriForFile(context, AUTHORITY, zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 分享备份文件（调用系统分享面板）
     */
    fun shareBackup(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ClosetMate 衣橱数据备份")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享备份文件到…"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 恢复
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 从用户选择的 ZIP 文件恢复数据。
     *
     * 流程：
     *   1. 关闭并重置数据库单例
     *   2. 删除现有数据库文件和图片
     *   3. 解压 ZIP 到对应目录
     *   4. 重启 App
     *
     * @return true 表示恢复成功并已触发重启；false 表示失败
     */
    suspend fun restoreBackup(context: Context, zipUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val success = extractAndReplace(context, zipUri)
        if (success) {
            withContext(Dispatchers.Main) {
                restartApp(context)
            }
        }
        success
    }

    /**
     * 核心提取逻辑（不含重启），供测试直接调用。
     *
     * @return true 表示提取成功；false 表示失败
     */
    internal suspend fun extractAndReplace(context: Context, zipUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // 1. 关闭数据库
                AppDatabase.closeAndReset()

                // 2. 删除现有数据
                val dbFile = context.getDatabasePath(DB_NAME)
                listOf(dbFile, File("${dbFile.path}-shm"), File("${dbFile.path}-wal"))
                    .filter { it.exists() }
                    .forEach { it.delete() }

                val imagesDir = File(context.filesDir, "clothing_images")
                imagesDir.deleteRecursively()
                imagesDir.mkdirs()

                // 3. 解压 ZIP
                context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val destFile = when {
                                entry.name.startsWith(ZIP_DB_DIR) -> {
                                    val fileName = entry.name.removePrefix(ZIP_DB_DIR)
                                    File(dbFile.parentFile, fileName)
                                }
                                entry.name.startsWith(ZIP_IMG_DIR) -> {
                                    val fileName = entry.name.removePrefix(ZIP_IMG_DIR)
                                    File(imagesDir, fileName)
                                }
                                else -> null
                            }
                            if (destFile != null && !entry.isDirectory) {
                                destFile.parentFile?.mkdirs()
                                FileOutputStream(destFile).use { zis.copyTo(it) }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    /**
     * 强制重启 App（恢复数据后调用）
     */
    private fun restartApp(context: Context) {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
        (context as? Activity)?.finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
