package com.inkqilin.ledger.util

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份包可选加密：AES-256-GCM + PBKDF2。
 *
 * 密文布局：
 *  MAGIC(5) | SALT(16) | IV(12) | ciphertext+tag
 */
object BackupCrypto {
    private val MAGIC = byteArrayOf('I'.code.toByte(), 'Q'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte(), '1'.code.toByte())
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val KEY_BITS = 256
    private const val PBKDF2_ITERATIONS = 120_000
    private const val GCM_TAG_BITS = 128

    fun isEncrypted(data: ByteArray): Boolean {
        if (data.size < MAGIC.size + SALT_LEN + IV_LEN) return false
        for (i in MAGIC.indices) {
            if (data[i] != MAGIC[i]) return false
        }
        return true
    }

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "密码不能为空" }
        val random = SecureRandom()
        val salt = ByteArray(SALT_LEN).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { random.nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ct = cipher.doFinal(plain)
        return MAGIC + salt + iv + ct
    }

    /** @throws IllegalArgumentException 密码错误或数据损坏 */
    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        if (!isEncrypted(data)) return data
        if (password.isEmpty()) throw IllegalArgumentException("请输入备份密码")
        val offset = MAGIC.size
        val salt = data.copyOfRange(offset, offset + SALT_LEN)
        val iv = data.copyOfRange(offset + SALT_LEN, offset + SALT_LEN + IV_LEN)
        val ct = data.copyOfRange(offset + SALT_LEN + IV_LEN, data.size)
        val key = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(ct)
        } catch (_: Exception) {
            throw IllegalArgumentException("密码错误或备份文件已损坏")
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }
}
