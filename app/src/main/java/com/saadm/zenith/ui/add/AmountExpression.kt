package com.saadm.zenith.ui.add

import com.saadm.zenith.domain.model.MoneyFormatter

// The keypad's amount: one figure, or figures joined by + and -, such as "120+45.50-10".

const val BACKSPACE = "⌫"
const val PLUS = "+"
const val MINUS_KEY = "-"

private const val MAX_WHOLE_DIGITS = 9
private const val MAX_TERMS = 10

/** The largest total one entry can hold, in paise. */
private const val MAX_TOTAL = 99_999_999_999L

private fun Char.isOperator() = this == '+' || this == '-'

private fun terms(amount: String): List<String> = amount.split('+', '-')

/** The amount after one keypad press. Presses that would make an unreadable amount are ignored. */
fun typeKey(current: String, key: String): String {
    if (key == BACKSPACE) return current.dropLast(1)
    if (key == PLUS || key == MINUS_KEY) {
        return when {
            current.isEmpty() -> current
            current.last().isOperator() -> current.dropLast(1) + key
            terms(current).size >= MAX_TERMS -> current
            else -> current + key
        }
    }
    val term = terms(current).last()
    val head = current.dropLast(term.length)
    if (key == ".") return if ('.' in term) current else head + term.ifEmpty { "0" } + "."
    val next = if (term == "0") key else term + key
    val whole = next.substringBefore('.')
    val decimals = next.substringAfter('.', "")
    return if (whole.length > MAX_WHOLE_DIGITS || decimals.length > 2) current else head + next
}

/** Whether more than a single figure has been typed. */
fun isSum(amount: String): Boolean = amount.any { it.isOperator() }

private fun termPaise(term: String): Long? =
    MoneyFormatter.parse(term) ?: 0L.takeIf { term.any { it == '0' } && term.all { it == '0' || it == '.' } }

/** The total in paise, which may be zero or below. Null while the text is not a complete sum. */
private fun total(amount: String): Long? {
    val figures = terms(amount)
    val signs = listOf('+') + amount.filter { it.isOperator() }.toList()
    var sum = 0L
    figures.forEachIndexed { index, figure ->
        val paise = termPaise(figure) ?: return null
        sum += if (signs[index] == '-') -paise else paise
    }
    return sum
}

/** The amount to save: a complete sum above zero and within the limit, else null. */
fun evaluate(amount: String): Long? = total(amount)?.takeIf { it in 1..MAX_TOTAL }

/** What is wrong with the typed amount, or null when it can be saved. */
fun amountProblem(amount: String): String? {
    if (amount.isNotEmpty() && amount.last().isOperator()) return "Finish the sum"
    val total = total(amount)
    return when {
        total == null -> "Enter an amount"
        total <= 0 -> if (isSum(amount)) "The total must be above zero" else "Enter an amount"
        total > MAX_TOTAL -> "That total is too large"
        else -> null
    }
}

private fun displayTerm(term: String): String {
    val whole = term.substringBefore('.').ifEmpty { "0" }
    val grouped = MoneyFormatter.format(whole.toLong() * 100).whole
    return if ('.' in term) "$grouped.${term.substringAfter('.')}" else grouped
}

/** The typed amount with Indian grouping and a true minus, keeping a trailing point or sign as typed. */
fun displayAmount(amount: String): String {
    val operators = amount.filter { it.isOperator() }
    val figures = terms(amount)
    return buildString {
        figures.forEachIndexed { index, figure ->
            // Nothing is shown after a trailing sign until a figure is typed.
            if (index == 0 || figure.isNotEmpty()) append(displayTerm(figure))
            operators.getOrNull(index)?.let { append(if (it == '-') " − " else " + ") }
        }
    }.trimEnd()
}
