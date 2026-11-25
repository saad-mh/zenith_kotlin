package com.saadm.zenith.data.export

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import androidx.room.withTransaction
import com.saadm.zenith.data.db.AppDatabase
import com.saadm.zenith.data.entity.AccountEntity
import com.saadm.zenith.data.entity.AccountType
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.export.DataTransfer
import com.saadm.zenith.domain.export.ExportFormat
import com.saadm.zenith.domain.model.TxnFilter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class DataTransferImpl @Inject constructor(
    private val db: AppDatabase
) : DataTransfer {

    override suspend fun writeCsv(output: OutputStream, filter: TxnFilter) {
        val categories = db.categoryDao().getAll().associate { it.id to it.name }
        val payees = db.payeeDao().getAll().associate { it.id to it.name }
        val accounts = db.accountDao().getAll().associate { it.id to it.name }
        OutputStreamWriter(output, StandardCharsets.UTF_8).buffered().use { writer ->
            writer.write("date,type,amount,currency,category,payee,account,note\n")
            forEachTransactionPage { page ->
                page.filter { !it.isDeleted && filter.matches(it) }.forEach { txn ->
                    writer.write(
                        listOf(
                            dateText(txn.transactedAt),
                            txn.type.name,
                            BigDecimal.valueOf(txn.amount, 2).toPlainString(),
                            txn.currency,
                            categories[txn.categoryId].orEmpty(),
                            txn.payeeId?.let { payees[it] }.orEmpty(),
                            txn.accountId?.let { accounts[it] }.orEmpty(),
                            txn.note.orEmpty()
                        ).joinToString(",") { csvField(it) }
                    )
                    writer.write("\n")
                }
            }
        }
    }

    override suspend fun writeJson(output: OutputStream, now: Long) {
        JsonWriter(OutputStreamWriter(output, StandardCharsets.UTF_8).buffered()).use { w ->
            w.beginObject()
            w.name("format").value(ExportFormat.NAME)
            w.name("version").value(ExportFormat.VERSION.toLong())
            w.name("exportedAt").value(now)

            w.name("categories").beginArray()
            db.categoryDao().getAll().forEach { c ->
                w.beginObject()
                w.name("id").value(c.id)
                w.name("name").value(c.name)
                w.name("emoji").value(c.emoji)
                w.name("colorHex").value(c.colorHex)
                w.name("txnType").value(c.txnType.name)
                w.name("sortOrder").value(c.sortOrder.toLong())
                w.name("isDefault").value(c.isDefault)
                w.name("isDeleted").value(c.isDeleted)
                w.endObject()
            }
            w.endArray()

            w.name("accounts").beginArray()
            db.accountDao().getAll().forEach { a ->
                w.beginObject()
                w.name("id").value(a.id)
                w.name("name").value(a.name)
                w.name("type").value(a.type.name)
                w.name("colorHex").value(a.colorHex)
                w.name("emoji").value(a.emoji)
                w.name("sortOrder").value(a.sortOrder.toLong())
                w.name("isDeleted").value(a.isDeleted)
                w.name("openingBalance").value(a.openingBalance)
                w.endObject()
            }
            w.endArray()

            w.name("payees").beginArray()
            db.payeeDao().getAll().forEach { p ->
                w.beginObject()
                w.name("id").value(p.id)
                w.name("name").value(p.name)
                w.nullableString("avatarUri", p.avatarUri)
                w.nullableString("phone", p.phone)
                w.nullableString("upiId", p.upiId)
                w.name("createdAt").value(p.createdAt)
                w.name("isDeleted").value(p.isDeleted)
                w.endObject()
            }
            w.endArray()

            w.name("transactions").beginArray()
            forEachTransactionPage { page ->
                page.forEach { txn ->
                    w.beginObject()
                    w.name("id").value(txn.id)
                    w.name("amount").value(txn.amount)
                    w.name("type").value(txn.type.name)
                    w.name("transactedAt").value(txn.transactedAt)
                    w.name("createdAt").value(txn.createdAt)
                    w.name("updatedAt").value(txn.updatedAt)
                    w.name("categoryId").value(txn.categoryId)
                    w.nullableLong("payeeId", txn.payeeId)
                    w.nullableLong("accountId", txn.accountId)
                    w.nullableString("note", txn.note)
                    w.nullableString("receiptUri", txn.receiptUri)
                    w.name("currency").value(txn.currency)
                    w.name("isDeleted").value(txn.isDeleted)
                    w.nullableLong("deletedAt", txn.deletedAt)
                    w.endObject()
                }
            }
            w.endArray()
            w.endObject()
        }
    }

    override suspend fun readJson(input: InputStream) {
        JsonReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
            db.withTransaction {
                reader.beginObject()
                var checkedFormat = false
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "format" -> {
                            require(reader.nextString() == ExportFormat.NAME) { "This file is not a Zenith export" }
                            checkedFormat = true
                        }
                        "version" -> {
                            val version = reader.nextInt()
                            require(checkedFormat) { "The format must come before the version" }
                            require(version <= ExportFormat.VERSION) { "This export was made by a newer version of Zenith" }
                        }
                        "categories" -> readArray(reader) { fields ->
                            db.categoryDao().upsert(
                                CategoryEntity(
                                    id = fields.long("id"),
                                    name = fields.string("name"),
                                    emoji = fields.string("emoji"),
                                    colorHex = fields.string("colorHex"),
                                    txnType = TxnType.valueOf(fields.string("txnType")),
                                    sortOrder = fields.long("sortOrder").toInt(),
                                    isDefault = fields.bool("isDefault"),
                                    isDeleted = fields.bool("isDeleted")
                                )
                            )
                        }
                        "accounts" -> readArray(reader) { fields ->
                            db.accountDao().upsert(
                                AccountEntity(
                                    id = fields.long("id"),
                                    name = fields.string("name"),
                                    type = AccountType.valueOf(fields.string("type")),
                                    colorHex = fields.string("colorHex"),
                                    emoji = fields.string("emoji"),
                                    sortOrder = fields.long("sortOrder").toInt(),
                                    isDeleted = fields.bool("isDeleted"),
                                    openingBalance = fields.long("openingBalance")
                                )
                            )
                        }
                        "payees" -> readArray(reader) { fields ->
                            db.payeeDao().upsert(
                                PayeeEntity(
                                    id = fields.long("id"),
                                    name = fields.string("name"),
                                    avatarUri = fields.stringOrNull("avatarUri"),
                                    phone = fields.stringOrNull("phone"),
                                    upiId = fields.stringOrNull("upiId"),
                                    createdAt = fields.long("createdAt"),
                                    isDeleted = fields.bool("isDeleted")
                                )
                            )
                        }
                        "transactions" -> {
                            val batch = mutableListOf<TransactionEntity>()
                            readArray(reader) { fields ->
                                batch += TransactionEntity(
                                    id = fields.long("id"),
                                    amount = fields.long("amount"),
                                    type = TxnType.valueOf(fields.string("type")),
                                    transactedAt = fields.long("transactedAt"),
                                    createdAt = fields.long("createdAt"),
                                    updatedAt = fields.long("updatedAt"),
                                    categoryId = fields.long("categoryId"),
                                    payeeId = fields.longOrNull("payeeId"),
                                    accountId = fields.longOrNull("accountId"),
                                    note = fields.stringOrNull("note"),
                                    receiptUri = fields.stringOrNull("receiptUri"),
                                    currency = fields.string("currency"),
                                    isDeleted = fields.bool("isDeleted"),
                                    deletedAt = fields.longOrNull("deletedAt")
                                )
                                if (batch.size >= BATCH) {
                                    db.transactionDao().upsertAll(batch.toList())
                                    batch.clear()
                                }
                            }
                            if (batch.isNotEmpty()) db.transactionDao().upsertAll(batch.toList())
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                require(checkedFormat) { "This file is not a Zenith export" }
            }
        }
    }

    private suspend fun forEachTransactionPage(action: suspend (List<TransactionEntity>) -> Unit) {
        var offset = 0
        while (true) {
            val page = db.transactionDao().page(BATCH, offset)
            if (page.isEmpty()) break
            action(page)
            offset += page.size
        }
    }

    private suspend fun readArray(reader: JsonReader, onObject: suspend (Fields) -> Unit) {
        reader.beginArray()
        while (reader.hasNext()) {
            onObject(Fields(readFields(reader)))
        }
        reader.endArray()
    }

    private fun readFields(reader: JsonReader): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            map[name] = when (reader.peek()) {
                JsonToken.STRING -> reader.nextString()
                JsonToken.NUMBER -> reader.nextLong()
                JsonToken.BOOLEAN -> reader.nextBoolean()
                JsonToken.NULL -> {
                    reader.nextNull()
                    null
                }
                else -> {
                    reader.skipValue()
                    null
                }
            }
        }
        reader.endObject()
        return map
    }

    private class Fields(private val values: Map<String, Any?>) {
        fun long(name: String): Long = (values[name] as? Long) ?: error("Missing number '$name'")
        fun longOrNull(name: String): Long? = values[name] as? Long
        fun string(name: String): String = (values[name] as? String) ?: error("Missing text '$name'")
        fun stringOrNull(name: String): String? = values[name] as? String
        fun bool(name: String): Boolean = (values[name] as? Boolean) ?: error("Missing flag '$name'")
    }

    private companion object {
        const val BATCH = 500
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun dateText(millis: Long): String =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(DATE_FORMAT)

        /** Quotes a CSV field, and stops spreadsheet apps reading a leading = + - @ as a formula. */
        fun csvField(raw: String): String {
            val guarded = if (raw.isNotEmpty() && raw[0] in "=+-@\t\r") "'$raw" else raw
            return if (guarded.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                "\"" + guarded.replace("\"", "\"\"") + "\""
            } else {
                guarded
            }
        }

        fun JsonWriter.nullableString(name: String, value: String?) {
            name(name)
            if (value == null) nullValue() else value(value)
        }

        fun JsonWriter.nullableLong(name: String, value: Long?) {
            name(name)
            if (value == null) nullValue() else value(value)
        }
    }
}
