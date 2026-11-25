package com.saadm.zenith.ui.navigation

// Shared by the payee details route and the NavHost. The string matches the one used before,
// so back stacks saved by earlier builds still resolve.
const val PAYEE_ID_ARG = "payeeId"

// Optional on the add route: set to edit that transaction.
const val TXN_ID_ARG = "txnId"

/**
 * Every navigable screen. Call sites use these objects rather than literal strings.
 * The route strings are the ones already in use, so saved back stacks keep working.
 */
sealed class Destination(val route: String) {
    data object Overview : Destination("home")
    data object Insights : Destination("insights")
    data object Settings : Destination("settings")
    data object Budgets : Destination("budgets")
    data object Transactions : Destination("transactions")
    data object Payees : Destination("people")
    data object Categories : Destination("categories")
    data object AddTransaction : Destination("add?$TXN_ID_ARG={$TXN_ID_ARG}")
    data object PayeeDetails : Destination("payee_details/{$PAYEE_ID_ARG}")

    companion object {
        /** Top-level tabs, in pill order. */
        val topLevelTabs: List<Destination> = listOf(Overview, Insights, Settings)

        const val ADD_ROUTE = "add"

        fun editRoute(txnId: Long): String = "add?$TXN_ID_ARG=$txnId"

        fun payeeDetailsRoute(payeeId: Long): String = "payee_details/$payeeId"

        /** Full-screen flows: no bottom bar. */
        fun isFullScreen(route: String?): Boolean =
            route == AddTransaction.route || route?.startsWith("payee_details") == true

        /** The tab a screen belongs to, so the pill highlights it. Insights sub-pages report Insights. */
        fun tabFor(route: String?): String? = when (route) {
            Overview.route, Insights.route, Settings.route -> route
            Budgets.route, Transactions.route, Payees.route, Categories.route -> Insights.route
            else -> null
        }
    }
}
