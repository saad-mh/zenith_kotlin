package com.saadm.zenith.ui.navigation

// Shared by the payee details route and the NavHost. The string matches the one used before,
// so back stacks saved by earlier builds still resolve.
const val PAYEE_ID_ARG = "payeeId"

const val SETTINGS_PAGE_ARG = "page"

// Optional on the add route: set to edit that transaction.
const val TXN_ID_ARG = "txnId"

// With it, the add route copies that transaction into a new one instead.
const val REPEAT_ARG = "repeat"

/**
 * Every navigable screen. Call sites use these objects rather than literal strings.
 * The route strings are the ones already in use, so saved back stacks keep working.
 */
sealed class Destination(val route: String) {
    /** The page holding the three tabs; the bottom of the stack. */
    data object Tabs : Destination("tabs")
    data object Overview : Destination("home")
    data object Insights : Destination("insights")
    data object Settings : Destination("settings")
    data object Budgets : Destination("budgets")
    data object Transactions : Destination("transactions")
    data object Payees : Destination("people")
    data object Categories : Destination("categories")
    data object AddTransaction : Destination("add?$TXN_ID_ARG={$TXN_ID_ARG}&$REPEAT_ARG={$REPEAT_ARG}")
    data object PayeeDetails : Destination("payee_details/{$PAYEE_ID_ARG}")
    data object SettingsDetail : Destination("settings_page/{$SETTINGS_PAGE_ARG}")

    companion object {
        /** Top-level tabs, in pill order. */
        // Lazy: built eagerly, it captures a null for whichever object triggered this class's initialisation.
        val topLevelTabs: List<Destination> by lazy { listOf(Overview, Insights, Settings) }

        const val ADD_ROUTE = "add"

        fun editRoute(txnId: Long): String = "add?$TXN_ID_ARG=$txnId"

        fun repeatRoute(txnId: Long): String = "${editRoute(txnId)}&$REPEAT_ARG=true"

        fun settingsRoute(page: String): String = "settings_page/$page"

        fun payeeDetailsRoute(payeeId: Long): String = "payee_details/$payeeId"
    }
}
