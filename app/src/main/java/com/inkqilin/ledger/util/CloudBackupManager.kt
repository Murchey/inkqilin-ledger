package com.inkqilin.ledger.util

import android.content.Context
import com.inkqilin.ledger.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** 个人账本云备份：导出 Room 库 zip → COS 私有桶 */
object CloudBackupManager {
    private const val DB_NAME = "ledger_database"
    private const val SETTINGS_ENTRY = "app_settings.json"

    private fun prefixOf(config: CosConfig): String =
        config.prefix.trim().trimEnd('/').ifBlank { "backups/v1" }

    data class LocalExport(val bytes: ByteArray, val fileName: String, val size: Long) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LocalExport) return false
            return fileName == other.fileName && size == other.size && bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + size.hashCode()
            return result
        }
    }

    /**
     * 导出数据库；若 [password] 非空则 AES-GCM 加密整包。
     * [auto] 为 true 时使用自动备份文件名（与手动备份区分）。
     */
    suspend fun exportDatabaseZip(
        context: Context,
        password: CharArray? = null,
        auto: Boolean = false
    ): LocalExport =
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            runCatching {
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
            }

            val dbFile = context.getDatabasePath(DB_NAME)
            val wal = context.getDatabasePath("$DB_NAME-wal")
            val shm = context.getDatabasePath("$DB_NAME-shm")
            if (!dbFile.exists()) error("找不到本地数据库")

            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val encrypted = password != null && password.isNotEmpty()
            val fileName = when {
                auto && encrypted -> "auto_backup_${ts}_enc.zip"
                auto -> "auto_backup_$ts.zip"
                encrypted -> "ledger_${ts}_enc.zip"
                else -> "ledger_$ts.zip"
            }

            val bos = ByteArrayOutputStream()
            ZipOutputStream(bos).use { zos ->
                fun putFile(f: File, entryName: String) {
                    if (!f.exists() || f.length() == 0L) return
                    zos.putNextEntry(ZipEntry(entryName))
                    f.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
                putFile(dbFile, DB_NAME)
                putFile(wal, "$DB_NAME-wal")
                putFile(shm, "$DB_NAME-shm")
                // 应用设置（主题、备份计划、COS 配置等）
                runCatching {
                    val settings = ThemeManager(context).exportSettingsJson()
                    zos.putNextEntry(ZipEntry(SETTINGS_ENTRY))
                    zos.write(settings.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
            val raw = bos.toByteArray()
            val bytes = if (encrypted) BackupCrypto.encrypt(raw, password!!) else raw
            LocalExport(bytes, fileName, bytes.size.toLong())
        }

    /** 上传备份到 COS；password 非空则加密 */
    suspend fun uploadBackup(
        context: Context,
        config: CosConfig,
        password: CharArray? = null,
        auto: Boolean = false
    ): CosObjectMeta {
        val export = exportDatabaseZip(context, password, auto)
        val key = "${prefixOf(config)}/${export.fileName}"
        CosClient.putObject(
            config = config,
            key = key,
            bytes = export.bytes,
            contentType = "application/octet-stream"
        )
        return CosObjectMeta(
            key = key,
            size = export.size,
            lastModified = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        )
    }

    /** 删除云端 auto_backup*（仅自动备份，不动手动备份） */
    suspend fun deletePreviousAutoCloudBackups(config: CosConfig) {
        listBackups(config)
            .filter { it.key.substringAfterLast('/').startsWith("auto_backup") }
            .forEach { runCatching { deleteBackup(config, it.key) } }
    }

    /** 列出云端备份 */
    suspend fun listBackups(config: CosConfig): List<CosObjectMeta> {
        return CosClient.listObjects(config, "${prefixOf(config)}/")
            .filter { it.key.endsWith(".zip") }
    }

    /**
     * 下载并恢复数据库；若为加密包须提供 [password]。
     */
    suspend fun downloadAndRestore(
        context: Context,
        config: CosConfig,
        key: String,
        password: CharArray? = null
    ): Unit = withContext(Dispatchers.IO) {
        val raw = CosClient.getObject(config, key)
        val zipBytes = if (BackupCrypto.isEncrypted(raw)) {
            BackupCrypto.decrypt(raw, password ?: charArrayOf())
        } else {
            raw
        }
        restoreZipBytes(context, zipBytes)
    }

    /** 读取文件头判断是否加密备份 */
    fun isLocalBackupEncrypted(file: File): Boolean {
        if (!file.exists() || file.length() < 5) return false
        val header = ByteArray(5)
        file.inputStream().use { input ->
            var read = 0
            while (read < 5) {
                val n = input.read(header, read, 5 - read)
                if (n < 0) break
                read += n
            }
            if (read < 5) return false
        }
        return header[0] == 'I'.code.toByte() &&
            header[1] == 'Q'.code.toByte() &&
            header[2] == 'B'.code.toByte() &&
            header[3] == 'K'.code.toByte() &&
            header[4] == '1'.code.toByte()
    }

    private fun hasSqliteMagic(bytes: ByteArray): Boolean {
        // "SQLite format 3\0"
        val magic = byteArrayOf(
            0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66,
            0x6F, 0x72, 0x6D, 0x61, 0x74, 0x20, 0x33, 0x00
        )
        if (bytes.size < magic.size) return false
        for (i in magic.indices) {
            if (bytes[i] != magic[i]) return false
        }
        return true
    }

    /**
     * 用备份 zip 覆盖本地库。
     * 会先校验 SQLite 文件头，并自动做「恢复前安全副本」；失败时回滚。
     */
    fun restoreZipBytes(context: Context, zipBytes: ByteArray) {
        if (zipBytes.isEmpty()) error("备份内容为空，已取消恢复")
        val files = mutableMapOf<String, ByteArray>()
        try {
            ZipInputStream(zipBytes.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        files[entry.name] = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            error("备份包无法解析，已取消恢复：${e.message}")
        }

        val mainDb = files[DB_NAME]
            ?: error("备份包中缺少 $DB_NAME，已取消恢复")
        if (mainDb.size < 100 || !hasSqliteMagic(mainDb)) {
            error("备份包中的数据库无效或已损坏，已取消恢复")
        }

        val dbFile = context.getDatabasePath(DB_NAME)
        val wal = context.getDatabasePath("$DB_NAME-wal")
        val shm = context.getDatabasePath("$DB_NAME-shm")

        // 恢复前安全副本（放在应用私有目录，便于失败回滚）
        val safetyDir = File(localBackupDir(context), "pre_restore")
        safetyDir.mkdirs()
        val safetyFile = File(safetyDir, "ledger_database_safety.db")
        val walSafety = File(safetyDir, "ledger_database_safety.db-wal")
        val shmSafety = File(safetyDir, "ledger_database_safety.db-shm")
        runCatching { safetyFile.delete() }
        runCatching { walSafety.delete() }
        runCatching { shmSafety.delete() }
        if (dbFile.exists()) {
            dbFile.copyTo(safetyFile, overwrite = true)
        }
        if (wal.exists()) runCatching { wal.copyTo(walSafety, overwrite = true) }
        if (shm.exists()) runCatching { shm.copyTo(shmSafety, overwrite = true) }

        AppDatabase.closeAndClear()

        try {
            dbFile.parentFile?.mkdirs()
            wal.delete()
            shm.delete()
            dbFile.outputStream().use { it.write(mainDb) }
            // 不恢复包内 wal/shm，避免与新主库不一致；下次打开会重建
            // 恢复应用设置（主题/备份计划/COS 等）；旧备份无此文件则跳过
            files[SETTINGS_ENTRY]?.toString(Charsets.UTF_8)?.let { settingsJson ->
                runCatching {
                    kotlinx.coroutines.runBlocking {
                        ThemeManager(context).importSettingsJson(settingsJson)
                    }
                }
            }
        } catch (e: Exception) {
            // 回滚到恢复前
            runCatching {
                if (safetyFile.exists()) {
                    safetyFile.copyTo(dbFile, overwrite = true)
                    if (walSafety.exists()) walSafety.copyTo(wal, overwrite = true)
                    if (shmSafety.exists()) shmSafety.copyTo(shm, overwrite = true)
                }
            }
            error("恢复写入失败，已尝试回滚：${e.message}")
        }
    }

    /** 删除云端对象，并确认远端已不存在 */
    suspend fun deleteBackup(config: CosConfig, key: String) {
        CosClient.deleteObject(config, key)
        val stillThere = runCatching { CosClient.objectExists(config, key) }.getOrDefault(true)
        if (stillThere) {
            error("云端对象删除后仍存在，请检查存储桶是否开启了版本控制")
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        return String.format(Locale.US, "%.2f GB", mb / 1024.0)
    }

    // ── 本地备份（应用私有目录，卸载会丢失；可再导出到系统文件） ──

    fun localBackupDir(context: Context): File =
        File(context.filesDir, "backups").apply { mkdirs() }

    /** 生成备份写入应用私有 backups 目录；password 非空则加密 */
    suspend fun createLocalBackup(
        context: Context,
        password: CharArray? = null,
        auto: Boolean = false
    ): File =
        withContext(Dispatchers.IO) {
            val export = exportDatabaseZip(context, password, auto)
            val target = File(localBackupDir(context), export.fileName)
            target.outputStream().use { it.write(export.bytes) }
            target
        }

    /** 自动备份文件（文件名以 auto_backup 开头） */
    fun listAutoLocalBackups(context: Context): List<File> =
        listLocalBackups(context).filter { it.name.startsWith("auto_backup") }

    /** 删除上一次自动备份（仅 auto_backup*，不动手动备份） */
    fun deletePreviousAutoLocalBackups(context: Context) {
        listAutoLocalBackups(context).forEach { runCatching { deleteLocalBackup(it) } }
    }

    fun listLocalBackups(context: Context): List<File> {
        val dir = localBackupDir(context)
        return dir.listFiles { f -> f.isFile && (f.name.endsWith(".zip") || f.name.endsWith(".iqbackup")) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * 从系统文件选择器 Uri 导入备份到本地列表。
     * 接受明文 zip（PK 开头）或应用加密包（IQBK1）。
     */
    suspend fun importBackupFromUri(
        context: Context,
        uri: android.net.Uri,
        displayName: String?
    ): File = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("无法读取所选文件")
        if (bytes.size < 32) error("文件过小，不是有效备份")

        val isZip = bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        val isEnc = BackupCrypto.isEncrypted(bytes)
        if (!isZip && !isEnc) {
            error("不是账本备份文件（需为 zip 或本应用加密包）")
        }
        // 明文 zip 再验一下能否当 zip 打开
        if (isZip) {
            runCatching {
                java.util.zip.ZipInputStream(bytes.inputStream()).use { zis ->
                    if (zis.nextEntry == null) error("zip 为空")
                }
            }.getOrElse { error("zip 无法解析：${it.message}") }
        }

        var name = displayName?.substringAfterLast('/')?.trim().orEmpty()
            .ifBlank { "imported_${System.currentTimeMillis()}" }
        if (!name.endsWith(".zip") && !name.endsWith(".iqbackup")) {
            name = if (isEnc) "${name}_enc.zip" else "$name.zip"
        }
        // 避免覆盖已有文件
        var target = File(localBackupDir(context), name)
        var seq = 1
        while (target.exists()) {
            val base = name.substringBeforeLast('.')
            val ext = name.substringAfterLast('.')
            target = File(localBackupDir(context), "${base}_$seq.$ext")
            seq++
        }
        target.outputStream().use { it.write(bytes) }
        target
    }

    // ── 恢复前安全副本 / 救灾 ──

    fun safetyBackupFile(context: Context): File =
        File(File(localBackupDir(context), "pre_restore"), "ledger_database_safety.db")

    fun hasSafetyCopy(context: Context): Boolean {
        val f = safetyBackupFile(context)
        return f.exists() && f.length() > 100
    }

    /**
     * 用恢复前安全副本直接覆盖当前库（救灾用）。
     * 仅在确认当前库异常时使用。
     */
    fun restoreFromSafetyCopy(context: Context) {
        val safety = safetyBackupFile(context)
        if (!safety.exists()) error("没有找到恢复前安全副本")
        val bytes = safety.readBytes()
        if (bytes.size < 100 || !hasSqliteMagic(bytes)) {
            error("安全副本不是有效 SQLite，无法恢复")
        }
        val dbFile = context.getDatabasePath(DB_NAME)
        val wal = context.getDatabasePath("$DB_NAME-wal")
        val shm = context.getDatabasePath("$DB_NAME-shm")
        AppDatabase.closeAndClear()
        dbFile.parentFile?.mkdirs()
        wal.delete()
        shm.delete()
        dbFile.outputStream().use { it.write(bytes) }
    }

    /** 调试/救灾：列出备份相关目录下的文件名 */
    fun describeLocalBackupFiles(context: Context): String {
        val dir = localBackupDir(context)
        val pre = File(dir, "pre_restore")
        val zips = listLocalBackups(context).joinToString { it.name }
        val pres = (pre.listFiles()?.joinToString { "${it.name}(${it.length()})" } ?: "无")
        val db = context.getDatabasePath(DB_NAME)
        val dbInfo = if (db.exists()) "${db.length()} bytes" else "不存在"
        return "当前库: $dbInfo\n本地 zip: ${zips.ifBlank { "无" }}\n安全副本目录: $pres"
    }

    fun restoreLocalBackup(context: Context, file: File, password: CharArray? = null) {
        if (!file.exists()) error("本地备份文件不存在")
        val raw = file.readBytes()
        val zipBytes = if (BackupCrypto.isEncrypted(raw)) {
            BackupCrypto.decrypt(raw, password ?: charArrayOf())
        } else {
            raw
        }
        restoreZipBytes(context, zipBytes)
    }

    /**
     * 彻底删除本地备份：先用零字节覆写文件内容，再 unlink。
     * 注意：闪存/系统快照层面无法保证物理抹除，但可显著降低普通恢复难度。
     */
    fun deleteLocalBackup(file: File): Boolean {
        if (!file.exists()) return true
        return try {
            val length = file.length()
            if (length > 0L) {
                java.io.RandomAccessFile(file, "rw").use { raf ->
                    raf.seek(0L)
                    val chunk = ByteArray(1024 * 256)
                    var written = 0L
                    while (written < length) {
                        val n = minOf(chunk.size.toLong(), length - written).toInt()
                        raf.write(chunk, 0, n)
                        written += n
                    }
                    raf.fd.sync()
                }
            }
            val deleted = file.delete()
            deleted && !file.exists()
        } catch (_: Exception) {
            runCatching { file.delete() }
            !file.exists()
        }
    }

    /** 把本地备份 zip 字节写到 SAF 选择的 Uri */
    fun copyLocalBackupToUri(context: Context, file: File, uri: android.net.Uri): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } != null
        }.getOrDefault(false)
    }
}
