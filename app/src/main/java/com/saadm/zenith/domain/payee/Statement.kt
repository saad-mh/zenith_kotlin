package com.saadm.zenith.domain.payee

import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MoneyFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private const val STATEMENT_ENTRIES = 10

// Absolute dates: the message may be read long after it is sent.
private val statementDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/**
 * A plain-text balance statement to send to [name], written from the user to them: "you" is the
 * person, "I" is the user. [notes] holds each transaction's note. Newest entries first.
 */
fun balanceStatement(
    name: String,
    ledger: PayeeLedger,
    notes: Map<Long, String?>,
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val amount = MoneyFormatter.format(abs(ledger.net)).text
    val balance = when {
        ledger.net > 0 -> "You owe me $amount."
        ledger.net < 0 -> "I owe you $amount."
        else -> "We're all settled."
    }
    val recent = ledger.history.asReversed().take(STATEMENT_ENTRIES).map { entry ->
        val what = when (entry.type) {
            TxnType.DUE_TO -> "You owe"
            TxnType.DUE_FROM -> "I owe"
            TxnType.SETTLEMENT_FROM -> "You paid"
            else -> "I paid"
        }
        listOfNotNull(
            Instant.ofEpochMilli(entry.at).atZone(zone).format(statementDate),
            "$what ${MoneyFormatter.format(entry.amount).text}",
            notes[entry.transactionId]?.trim()?.ifEmpty { null }
        ).joinToString(" · ")
    }
    val earlier = ledger.history.size - recent.size
    return buildString {
        append("Hi $name,\n").append(balance)
        if (recent.isNotEmpty()) {
            append("\n\nRecent:\n").append(recent.joinToString("\n"))
            if (earlier > 0) append("\n($earlier earlier not shown)")
        }
    }
}
