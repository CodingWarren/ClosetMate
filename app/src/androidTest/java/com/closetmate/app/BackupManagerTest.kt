package com.closetmate.app

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.closetmate.app.data.backup.BackupManager
import com.closetmate.app.data.local.AppDatabase
import com.closetmate.app.data.local.entity.ClothingEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * BackupManager 自动化测试
 *
 * 覆盖用例：
 *   BR-01  正常备份：有数据时生成有效 ZIP，内容完整
 *   BR-02  空数据备份：无衣物/图片时仍能生成 ZIP
 *   BR-03P 恢复提取逻辑：解压后文件出现在正确位置（不含重启）
 *   BR-04  损坏文件恢复：传入非法 URI 时返回 false，不崩溃
 *   BR-05  空备份恢复：解压空 ZIP 后现有数据被清空
 */
@RunWith(AndroidJUnit4::class)
class BackupManagerTest {

    private lateinit var context: Context

    // 测试用图片目录
    private lateinit var imagesDir: File

    // 测试用缓存目录（存放临时 ZIP）
    private lateinit var testCacheDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        imagesDir = File(context.filesDir, "clothing_images").also { it.mkdirs() }
        testCacheDir = File(context.cacheDir, "test_backup").also { it.mkdirs() }

        // 确保数据库已初始化（触发 Room 创建数据库文件）
        AppDatabase.getInstance(context)
    }

    @After
    fun tearDown() {
        // 清理测试产生的文件
        testCacheDir.deleteRecursively()
        File(context.cacheDir, "backup").deleteRecursively()
        imagesDir.deleteRecursively()
        imagesDir.mkdirs()

        // 关闭数据库连接，避免影响其他测试
        AppDatabase.closeAndReset()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BR-01  正常备份：有数据时生成有效 ZIP，内容完整
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun br01_createBackup_withData_generatesValidZip() = runBlocking {
        // 准备：向数据库插入2件衣物
        val db = AppDatabase.getInstance(context)
        db.clothingDao().insert(makeClothing("test-id-1", "T恤"))
        db.clothingDao().insert(makeClothing("test-id-2", "裤子"))

        // 准备：创建1张模拟图片
        val fakeImage = File(imagesDir, "img_test_001.jpg")
        fakeImage.writeBytes(ByteArray(1024) { it.toByte() }) // 1KB 假图片

        // 执行备份
        val backupUri = BackupManager.createBackup(context)

        // 断言1：URI 不为空
        assertNotNull("备份 URI 不应为 null", backupUri)

        // 断言2：ZIP 文件实际存在
        val zipPath = backupUri!!.path!!
        val zipFile = File(zipPath)
        assertTrue("备份 ZIP 文件应存在于磁盘", zipFile.exists())
        assertTrue("备份 ZIP 文件大小应 > 0", zipFile.length() > 0)

        // 断言3：ZIP 内容包含数据库文件
        val entries = ZipFile(zipFile).use { zf ->
            zf.entries().toList().map { it.name }
        }
        println("[BR-01] ZIP 内容：$entries")

        assertTrue(
            "ZIP 应包含数据库文件 (database/closetmate_database)",
            entries.any { it.startsWith("database/closetmate_database") }
        )

        // 断言4：ZIP 内容包含图片文件
        assertTrue(
            "ZIP 应包含图片文件 (images/img_test_001.jpg)",
            entries.contains("images/img_test_001.jpg")
        )

        println("[BR-01] ✅ 通过 — ZIP 路径：$zipPath，条目数：${entries.size}")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BR-02  空数据备份：无衣物/图片时仍能生成 ZIP
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun br02_createBackup_emptyData_generatesZipWithoutImages() = runBlocking {
        // 确保图片目录为空
        imagesDir.listFiles()?.forEach { it.delete() }

        // 执行备份
        val backupUri = BackupManager.createBackup(context)

        // 断言1：URI 不为空（即使没有图片也应成功）
        assertNotNull("空数据备份 URI 不应为 null", backupUri)

        val zipPath = backupUri!!.path!!
        val zipFile = File(zipPath)
        assertTrue("空数据备份 ZIP 文件应存在", zipFile.exists())

        val entries = ZipFile(zipFile).use { zf ->
            zf.entries().toList().map { it.name }
        }
        println("[BR-02] ZIP 内容：$entries")

        // 断言2：不包含图片条目
        assertFalse(
            "空数据备份不应包含图片条目",
            entries.any { it.startsWith("images/") }
        )

        println("[BR-02] ✅ 通过 — ZIP 条目数：${entries.size}")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BR-03P 恢复提取逻辑：解压后文件出现在正确位置（不含重启）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun br03p_extractAndReplace_validZip_restoresFilesToCorrectLocations() = runBlocking {
        // 准备：构造一个包含数据库文件和图片的 ZIP
        val fakeZip = File(testCacheDir, "test_restore.zip")
        val fakeDbContent = "FAKE_DB_CONTENT".toByteArray()
        val fakeImgContent = "FAKE_IMAGE_DATA".toByteArray()

        ZipOutputStream(FileOutputStream(fakeZip)).use { zos ->
            // 写入假数据库文件
            zos.putNextEntry(ZipEntry("database/closetmate_database"))
            zos.write(fakeDbContent)
            zos.closeEntry()

            // 写入假图片文件
            zos.putNextEntry(ZipEntry("images/img_restored_001.jpg"))
            zos.write(fakeImgContent)
            zos.closeEntry()
        }

        val zipUri = Uri.fromFile(fakeZip)

        // 执行提取（不含重启）
        val success = BackupManager.extractAndReplace(context, zipUri)

        // 断言1：提取成功
        assertTrue("extractAndReplace 应返回 true", success)

        // 断言2：数据库文件已被替换
        val dbFile = context.getDatabasePath("closetmate_database")
        assertTrue("数据库文件应存在于正确位置", dbFile.exists())
        assertArrayEquals(
            "数据库文件内容应与 ZIP 中一致",
            fakeDbContent,
            dbFile.readBytes()
        )

        // 断言3：图片文件已被恢复
        val restoredImg = File(imagesDir, "img_restored_001.jpg")
        assertTrue("图片文件应存在于 clothing_images 目录", restoredImg.exists())
        assertArrayEquals(
            "图片文件内容应与 ZIP 中一致",
            fakeImgContent,
            restoredImg.readBytes()
        )

        println("[BR-03P] ✅ 通过 — 数据库和图片均已恢复到正确位置")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BR-04  损坏文件恢复：传入非法 URI 时返回 false，不崩溃
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun br04_extractAndReplace_corruptedFile_returnsFalse() = runBlocking {
        // 准备：创建一个内容损坏的文件（不是有效 ZIP）
        val corruptedFile = File(testCacheDir, "corrupted.zip")
        corruptedFile.writeText("THIS IS NOT A VALID ZIP FILE CONTENT !!!")

        val corruptedUri = Uri.fromFile(corruptedFile)

        // 执行提取
        val success = BackupManager.extractAndReplace(context, corruptedUri)

        // 断言：应返回 false，不抛出异常
        assertFalse("损坏文件恢复应返回 false", success)

        println("[BR-04] ✅ 通过 — 损坏文件被优雅处理，返回 false")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BR-05  空备份恢复：解压空 ZIP 后现有数据被清空
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun br05_extractAndReplace_emptyZip_clearsExistingImages() = runBlocking {
        // 准备：在图片目录中放入一张图片
        val existingImg = File(imagesDir, "img_existing.jpg")
        existingImg.writeBytes(ByteArray(512) { 0xFF.toByte() })
        assertTrue("测试前提：图片文件应存在", existingImg.exists())

        // 准备：创建一个空 ZIP（不含任何条目）
        val emptyZip = File(testCacheDir, "empty_backup.zip")
        ZipOutputStream(FileOutputStream(emptyZip)).use { /* 不写入任何条目 */ }

        val emptyZipUri = Uri.fromFile(emptyZip)

        // 执行提取
        val success = BackupManager.extractAndReplace(context, emptyZipUri)

        // 断言1：提取成功
        assertTrue("空 ZIP 提取应返回 true", success)

        // 断言2：原有图片已被清空
        assertFalse(
            "恢复空备份后，原有图片应被删除",
            existingImg.exists()
        )

        // 断言3：图片目录本身仍存在（只是为空）
        assertTrue("clothing_images 目录应仍然存在", imagesDir.exists())
        assertEquals(
            "clothing_images 目录应为空",
            0,
            imagesDir.listFiles()?.size ?: 0
        )

        println("[BR-05] ✅ 通过 — 空备份恢复后现有图片已清空")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────────────────────────────────

    private fun makeClothing(id: String, category: String) = ClothingEntity(
        id = id,
        imageUris = "",
        category = category,
        seasons = "春,夏",
        colors = "白",
        styles = "休闲",
        brand = "测试品牌",
        price = 99.0,
        purchaseChannel = "",
        purchaseDate = "",
        storageLocation = "衣柜",
        status = "正常",
        wearCount = 0,
        lastWornAt = 0L,
        notes = "测试备注",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
