package com.expensetracker.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Ledger : Screen("ledger")
    object LedgerDetail : Screen("ledger_detail")
    object Calendar : Screen("calendar")
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object CaptureInbox : Screen("capture_inbox")
    object AddTransaction : Screen("add_transaction")
    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: Long) = "transaction_detail/$transactionId"
    }
    object Accounts : Screen("accounts")
    object Categories : Screen("categories")
    object Tags : Screen("tags")
    object Suggestions : Screen("suggestions")
    object Rules : Screen("rules")
    object Budgets : Screen("budgets")
    object Import : Screen("import")
    object Backup : Screen("backup")
}
