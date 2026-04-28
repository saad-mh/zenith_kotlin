package com.saadm.zenith.data.entity

import androidx.room.DatabaseView

/**
 * Materialized view for payee financial summaries, computed dynamically from transactions.
 * This ensures all derived values (balances, interaction counts) stay in sync with the
 * transactions dataset without storing redundant data on PayeeEntity.
 */
@DatabaseView(
	viewName = "payee_balances",
	value = """
		SELECT
			p.id,
			p.name,
			p.avatarUri,
			p.phone,
			p.upiId,
			p.createdAt,
			p.isDeleted,
			COALESCE(SUM(CASE WHEN t.type = 'DUE_TO' AND t.isDeleted = 0 THEN t.amount ELSE 0 END), 0) as dueToAmount,
			COALESCE(SUM(CASE WHEN t.type = 'DUE_FROM' AND t.isDeleted = 0 THEN t.amount ELSE 0 END), 0) as dueFromAmount,
			COUNT(DISTINCT CASE WHEN t.isDeleted = 0 THEN t.id END) as transactionCount,
			MAX(CASE WHEN t.isDeleted = 0 THEN t.transactedAt ELSE NULL END) as lastInteractionAt
		FROM payees p
		LEFT JOIN transactions t ON p.id = t.payeeId
		GROUP BY p.id
	"""
)
data class PayeeBalanceView(
	val id: Long,
	val name: String,
	val avatarUri: String?,
	val phone: String?,
	val upiId: String?,
	val createdAt: Long,
	val isDeleted: Boolean,
	val dueToAmount: Long,       // Amount payee owes to user (in smallest unit)
	val dueFromAmount: Long,     // Amount user owes to payee (in smallest unit)
	val transactionCount: Int,
	val lastInteractionAt: Long?
) {
	/**
	 * Net balance from user's perspective: positive = payee owes, negative = user owes.
	 */
	fun netBalanceAmount(): Long = dueToAmount - dueFromAmount

	/**
	 * Settlement status indicator based on net balance.
	 */
	fun settlementStatus(): String = when {
		dueToAmount == 0L && dueFromAmount == 0L -> "settled"
		else -> "pending"
	}
}

