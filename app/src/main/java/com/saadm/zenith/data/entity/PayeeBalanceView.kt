package com.saadm.zenith.data.entity

import androidx.room.DatabaseView

/** SQL of the payee_balances view. The migration and the annotation both use it, so they cannot drift. */
object PayeeBalancesViewSql {
	/**
	 * Each payee's net position: dues and settlements, signed from the user's side.
	 * Positive net means the payee owes the user; negative means the user owes the payee.
	 * dueToAmount and dueFromAmount are the two sides of that net, so one of them is always zero.
	 */
	const val SELECT = """
		SELECT
			p.id,
			p.name,
			p.avatarUri,
			p.phone,
			p.upiId,
			p.createdAt,
			p.isDeleted,
			MAX(0, COALESCE(SUM(CASE WHEN t.isDeleted = 0 THEN
				CASE t.type
					WHEN 'DUE_TO' THEN t.amount
					WHEN 'SETTLEMENT_TO' THEN t.amount
					WHEN 'DUE_FROM' THEN -t.amount
					WHEN 'SETTLEMENT_FROM' THEN -t.amount
					ELSE 0
				END
			ELSE 0 END), 0)) as dueToAmount,
			MAX(0, -COALESCE(SUM(CASE WHEN t.isDeleted = 0 THEN
				CASE t.type
					WHEN 'DUE_TO' THEN t.amount
					WHEN 'SETTLEMENT_TO' THEN t.amount
					WHEN 'DUE_FROM' THEN -t.amount
					WHEN 'SETTLEMENT_FROM' THEN -t.amount
					ELSE 0
				END
			ELSE 0 END), 0)) as dueFromAmount,
			COUNT(DISTINCT CASE WHEN t.isDeleted = 0 THEN t.id END) as transactionCount,
			MAX(CASE WHEN t.isDeleted = 0 THEN t.transactedAt ELSE NULL END) as lastInteractionAt
		FROM payees p
		LEFT JOIN transactions t ON p.id = t.payeeId
		GROUP BY p.id
	"""
}

/**
 * Derived summary per payee, computed from transactions and never stored.
 * The payee_balances view is the source. Its two amounts are the open sides of a single net balance.
 */
@DatabaseView(
	viewName = "payee_balances",
	value = PayeeBalancesViewSql.SELECT
)
data class PayeeBalanceView(
	val id: Long,
	val name: String,
	val avatarUri: String?,
	val phone: String?,
	val upiId: String?,
	val createdAt: Long,
	val isDeleted: Boolean,
	val dueToAmount: Long,       // Amount payee owes to user, after settlements (smallest unit)
	val dueFromAmount: Long,     // Amount user owes to payee, after settlements (smallest unit)
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
