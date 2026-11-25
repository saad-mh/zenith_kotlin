package com.saadm.zenith.domain.usecase

import com.saadm.zenith.domain.export.DataTransfer
import com.saadm.zenith.domain.model.TxnFilter
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/** Writes transactions as CSV for a spreadsheet. The file picker gives the output stream. */
class ExportTransactionsCsv @Inject constructor(private val transfer: DataTransfer) {
    suspend operator fun invoke(output: OutputStream, filter: TxnFilter = TxnFilter.ALL) =
        transfer.writeCsv(output, filter)
}

/** Writes every table as JSON, which can be read back in with [ImportEverythingJson]. */
class ExportEverythingJson @Inject constructor(private val transfer: DataTransfer) {
    suspend operator fun invoke(output: OutputStream) = transfer.writeJson(output)
}

class ImportEverythingJson @Inject constructor(private val transfer: DataTransfer) {
    suspend operator fun invoke(input: InputStream) = transfer.readJson(input)
}
