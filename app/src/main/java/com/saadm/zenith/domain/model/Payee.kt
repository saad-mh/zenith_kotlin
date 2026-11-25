package com.saadm.zenith.domain.model

import com.saadm.zenith.data.entity.TransactionEntity

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
    val netBalance: Long = dueToAmount - dueFromAmount,
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
}
