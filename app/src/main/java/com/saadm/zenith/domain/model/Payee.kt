package com.saadm.zenith.domain.model

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
	val lastInteractionAt: Long? = null
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
		val abs = kotlin.math.abs(rupees)

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
            else -> "settled"
        }
	}
}
