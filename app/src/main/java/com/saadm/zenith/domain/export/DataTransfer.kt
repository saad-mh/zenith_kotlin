package com.saadm.zenith.domain.export

import com.saadm.zenith.domain.model.TxnFilter
import java.io.InputStream
import java.io.OutputStream

/**
 * Moves data in and out of a file. Writes stream page by page, so a long history does not load
 * into memory at once. Nothing here changes the data it reads.
 */
interface DataTransfer {

    /** Transactions matching [filter] as CSV, one row each, with a header row. */
    suspend fun writeCsv(output: OutputStream, filter: TxnFilter = TxnFilter.ALL)

    /** Every table, archived rows included, as one JSON document. [now] stamps the export. */
    suspend fun writeJson(output: OutputStream, now: Long = System.currentTimeMillis())

    /**
     * Reads a JSON document from [writeJson] and writes it in, keeping ids so references still line up.
     * Rows already present with the same id are replaced. Nothing else is removed.
     */
    suspend fun readJson(input: InputStream)
}

object ExportFormat {
    const val NAME = "zenith"
    const val VERSION = 1
}
