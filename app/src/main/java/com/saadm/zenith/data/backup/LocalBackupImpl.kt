package com.saadm.zenith.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.saadm.zenith.data.db.AppDatabase
import com.saadm.zenith.data.preferences.AppPreferencesStore
import com.saadm.zenith.domain.backup.LocalBackup
import com.saadm.zenith.domain.export.DataTransfer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * File layout: "ZNTHBAK1" (8 bytes), salt (16), IV (12), then AES-256-GCM ciphertext of the JSON
 * export. The key comes from the passphrase with PBKDF2-HMAC-SHA256. GCM's tag rejects a wrong
 * passphrase and any change to the file.
 */
class LocalBackupImpl(
    private val db: AppDatabase,
    private val transfer: DataTransfer,
    private val preferences: AppPreferencesStore,
    private val cacheDir: File
) : LocalBackup {

    @Inject
    constructor(
        db: AppDatabase,
        transfer: DataTransfer,
        preferences: AppPreferencesStore,
        @ApplicationContext context: Context
    ) : this(db, transfer, preferences, context.cacheDir)

    private val random = SecureRandom()

    override suspend fun writeBackup(output: OutputStream, passphrase: CharArray) {
        require(passphrase.size >= MIN_PASSPHRASE) { "Use a passphrase of at least $MIN_PASSPHRASE characters" }
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        output.write(MAGIC)
        output.write(salt)
        output.write(iv)
        CipherOutputStream(output, cipher(Cipher.ENCRYPT_MODE, passphrase, salt, iv)).use { encrypted ->
            transfer.writeJson(encrypted)
        }
    }

    override suspend fun restoreBackup(input: InputStream, passphrase: CharArray, replaceEverything: Boolean) {
        require(replaceEverything) { "Restoring replaces everything on this device. Confirm that first." }
        val header = readHeader(input)
        val salt = header.copyOfRange(MAGIC.size, MAGIC.size + SALT_BYTES)
        val iv = header.copyOfRange(MAGIC.size + SALT_BYTES, HEADER_BYTES)

        // Decrypt the whole file to a scratch file first. The tag is checked at the end, so a wrong
        // passphrase or a damaged file is caught before anything is removed.
        val scratch = File.createTempFile("restore", ".json", cacheDir)
        try {
            try {
                CipherInputStream(input, cipher(Cipher.DECRYPT_MODE, passphrase, salt, iv)).use { source ->
                    scratch.outputStream().use { source.copyTo(it) }
                }
            } catch (e: IOException) {
                throw IllegalArgumentException("Wrong passphrase, or the backup is damaged", e)
            }
            db.withTransaction {
                wipeTables()
                scratch.inputStream().use { transfer.readJson(it) }
            }
        } finally {
            scratch.delete()
        }
    }

    override suspend fun deleteAllData(confirmation: String) {
        require(confirmation == "DELETE") { "Type DELETE to confirm" }
        db.clearAllTables()
        preferences.clear()
    }

    override suspend fun clearCache(): Long {
        val freed = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        return freed
    }

    // Children first, so no foreign key ever points at a missing row.
    private fun wipeTables() {
        val sqlite = db.openHelper.writableDatabase
        listOf("transactions", "budgets", "payees", "accounts", "categories").forEach { table ->
            sqlite.execSQL("DELETE FROM $table")
        }
    }

    private fun readHeader(input: InputStream): ByteArray {
        val header = ByteArray(HEADER_BYTES)
        var read = 0
        while (read < HEADER_BYTES) {
            val n = input.read(header, read, HEADER_BYTES - read)
            if (n < 0) break
            read += n
        }
        require(read == HEADER_BYTES && header.copyOf(MAGIC.size).contentEquals(MAGIC)) { "This is not a Zenith backup" }
        return header
    }

    private fun cipher(mode: Int, passphrase: CharArray, salt: ByteArray, iv: ByteArray): Cipher {
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS))
            .encoded
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        }
    }

    private companion object {
        val MAGIC: ByteArray = "ZNTHBAK1".toByteArray(Charsets.US_ASCII)
        const val SALT_BYTES = 16
        const val IV_BYTES = 12
        const val HEADER_BYTES = 8 + SALT_BYTES + IV_BYTES
        const val ITERATIONS = 210_000
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val MIN_PASSPHRASE = 8
    }
}
