package com.saadm.zenith.domain.model

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import kotlin.math.abs

/**
 * Domain model for a payee, built from entity and derived financial summaries.
 * This represents a person in the user's network with tracked financial interactions.
 */
data class Payee(
    val id: Long,
    val name: String,
    val avatarUri: String?,
    val phone: String?,
    val upiId: String?,
    val createdAt: Long,
    /**
     * Amount payee owes to user (in smallest unit, e.g., paise).
     * Derived from transactions with type DUE_TO.
     */
    val dueToAmount: Long = 0,
    /**
     * Amount user owes to payee (in smallest unit, e.g., paise).
     * Derived from transactions with type DUE_FROM.
     */
    val dueFromAmount: Long = 0,
    /**
     * Net balance from user's perspective.
     * Positive = payee owes to user; negative = user owes to payee.
     */
    val netBalance: Long = dueFromAmount - dueToAmount,
    /**
     * Total number of non-deleted transactions involving this payee.
     */
    val transactionCount: Int = 0,
    /**
     * Timestamp of the most recent interaction, or null if none.
     */
    val lastInteractionAt: Long? = null,
    /**
     * Cached transaction history for this payee.
     * The domain model cannot query the database directly, so callers should
     * populate this list when they need transaction details.
     */
    val transactions: List<TransactionEntity> = emptyList()
) {
    /**
     * Check if this payee has any active balance (owing/owed).
     */
    fun hasActiveBalance(): Boolean = dueToAmount != 0L || dueFromAmount != 0L

    /**
     * Display-friendly balance text.
     */
    fun balanceText(): String =
        when {
            netBalance < 0 -> "- ${formatCompact(netBalance)}"
            netBalance > 0 -> "+ ${formatCompact(netBalance)}"
            else -> "settled"
        }

    fun formatCompact(bal: Long): String {
        val rupees = bal / 100.0
        val abs = abs(rupees)

        val formatted = when {
            abs >= 1_00_000 -> {
                val value = abs / 1_00_000
                if (value % 1.0 == 0.0) "${value.toInt()}L"
                else "%.1fL".format(value)
            }

            abs >= 1_000 -> {
                val value = abs / 1_000
                if (value % 1.0 == 0.0) "${value.toInt()}k"
                else "%.1fk".format(value)
            }

            else -> abs.toInt().toString()
        }

        return when {
            bal > 0 -> formatted
            bal < 0 -> formatted
            else -> "none"
        }
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val months = days / 30
        val years = days / 365

        return when {
            seconds < 60 -> "just now"
            minutes < 60 -> "$minutes m${if (minutes > 1) "" else ""} ago"
            hours < 24 -> "$hours hr${if (hours > 1) "s" else ""} ago"
            days < 7 -> "$days d${if (days > 1) "" else ""} ago"
            days < 30 -> "${days / 7} wk${if ((days / 7) > 1) "" else ""} ago"
            months < 12 -> "$months m${if (months > 1) "" else ""} ago"
            else -> "$years yr${if (years > 1) "s" else ""} ago"
        }
    }

    fun getAllTransactions(): List<TransactionEntity> {
        return transactions
            .asSequence()
            .filter { txn ->
                txn.payeeId == id &&
                    !txn.isDeleted &&
                    (txn.type == TxnType.DUE_TO || txn.type == TxnType.DUE_FROM)
            }
            .sortedWith(compareByDescending<TransactionEntity> { it.transactedAt }.thenByDescending { it.id })
            .toList()
    }

    fun getLastInteraction(): Long? {
        return when {
            lastInteractionAt != null -> lastInteractionAt
            else -> createdAt
        }
    }

    companion object {
        fun formatRelativeTime(transactedAt: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - transactedAt

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val months = days / 30
            val years = days / 365

            return when {
                seconds < 60 -> "just now"
                minutes < 60 -> "$minutes m${if (minutes > 1) "" else ""} ago"
                hours < 24 -> "$hours hr${if (hours > 1) "s" else ""} ago"
                days < 7 -> "$days d${if (days > 1) "" else ""} ago"
                days < 30 -> "${days / 7} wk${if ((days / 7) > 1) "" else ""} ago"
                months < 12 -> "$months m${if (months > 1) "" else ""} ago"
                else -> "$years yr${if (years > 1) "s" else ""} ago"
            }
        }
    }
}
