package com.expensetracker.app.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class ExpenseTrackerE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun endToEndSmokeTest() {
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        val currentMonthLabel = YearMonth.now().format(monthFormatter)
        val previousMonthLabel = YearMonth.now().minusMonths(1).format(monthFormatter)
        val nextMonthLabel = YearMonth.now().plusMonths(1).format(monthFormatter)

        waitForText("Expense Tracker")
        composeRule.onNodeWithText("View Ledger").performClick()
        waitForText("Ledger")
        composeRule.onNodeWithContentDescription("Back").performClick()
        waitForText("Expense Tracker")

        composeRule.onNodeWithTag("home_action_budget").performClick()
        waitForText("Budget")
        composeRule.onNodeWithContentDescription("Close").performClick()
        waitForText("Expense Tracker")

        composeRule.onNodeWithTag("home_action_statement").performClick()
        waitForText("Statement Import")
        composeRule.onNodeWithContentDescription("Back").performClick()
        waitForText("Expense Tracker")

        composeRule.onNodeWithContentDescription("Add Transaction").performClick()
        waitForText("Add Transaction")

        clickFirstNodeWithText("Income")
        clickFirstNodeWithText("Transfer")
        clickFirstNodeWithText("Refund")
        clickFirstNodeWithText("Expense")

        composeRule.onNodeWithTag("transaction_amount_input").performTextInput("123.45")
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithTag("transaction_notes_input").performTextInput("lunch")
        composeRule.onNodeWithText("Trip").performClick()
        composeRule.onNodeWithText("Save").performClick()

        waitForText("Expense Tracker")
        waitForText("123.45", substring = true)

        composeRule.onNodeWithText("Ledger").performClick()
        waitForText("Ledger")
        waitForText("lunch")
        waitForText("123.45", substring = true)

        composeRule.onNodeWithTag("ledger_search_input").performTextInput("lunch")
        waitForText("lunch")
        composeRule.onNodeWithTag("ledger_search_input").performTextClearance()
        waitForText("lunch")

        composeRule.onNodeWithTag("ledger_search_input").performTextInput("lunch")
        waitForText("lunch")
        composeRule.onNodeWithContentDescription("Filter").performClick()
        waitForText("Filter Transactions")
        composeRule.onNodeWithTag("ledger_filter_income").performClick()
        composeRule.onNodeWithTag("ledger_filter_apply").performClick()
        waitForText("No transactions found")

        composeRule.onNodeWithContentDescription("Filter").performClick()
        waitForText("Filter Transactions")
        composeRule.onNodeWithTag("ledger_filter_reset").performClick()
        waitForText("lunch")

        composeRule.onNodeWithText("Calendar").performClick()
        waitForText("Calendar")
        waitForText("Transactions")
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("lunch"))
        waitForText("lunch")
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Week"))
        composeRule.onNodeWithText("Week").performClick()
        waitForText("Period Breakdown")
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasText("Month"))
        composeRule.onNodeWithText("Month").performClick()
        waitForText(currentMonthLabel)
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasContentDescription("Next Month"))
        composeRule.onNodeWithContentDescription("Next Month").performClick()
        waitForText(nextMonthLabel)
        composeRule.onNodeWithTag("calendar_list").performScrollToNode(hasContentDescription("Previous Month"))
        composeRule.onNodeWithContentDescription("Previous Month").performClick()
        waitForText(currentMonthLabel)

        composeRule.onNodeWithText("Analytics").performClick()
        waitForText("Analytics")
        waitForText("By Category")
        waitForText("Food")
        waitForText("123.45", substring = true)
        composeRule.onNodeWithContentDescription("Previous").performClick()
        waitForText(previousMonthLabel)
        composeRule.onNodeWithContentDescription("Next").performClick()
        waitForText(currentMonthLabel)

        composeRule.onNodeWithText("Settings").performClick()
        waitForText("Settings")
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText("SMS & Notification Capture"))
        composeRule.onNodeWithTag("settings_capture_inbox").performClick()
        waitForText("Capture Inbox")
        waitForText("Import SMS History")
        composeRule.onNodeWithContentDescription("Back").performClick()
        waitForText("Settings")

        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText("Accounts"))
        composeRule.onNodeWithText("Accounts").performClick()
        waitForText("Accounts is not available in this prototype yet.", substring = true)

        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText("Backup"))
        waitForText("Backup")
        composeRule.onNodeWithTag("settings_backup").performClick()
        waitForText("Backup & Restore")
        composeRule.onNodeWithText("OK").performClick()

        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText("Version 1.0.0"))
        waitForText("Version 1.0.0")

        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasText("Reset All Data"))
        waitForText("Reset All Data")
        composeRule.onNodeWithTag("settings_reset_all").performClick()
        waitForText("Reset All Data?")
        composeRule.onNodeWithText("Cancel").performClick()
        waitUntilMissing("Reset All Data?")

        composeRule.onNodeWithTag("settings_reset_all").performClick()
        waitForText("Reset All Data?")
        composeRule.onNodeWithText("Delete").performClick()
        waitForText("Reset All Data is not available in this prototype yet.", substring = true)
    }

    private fun waitForText(
        text: String,
        substring: Boolean = false,
        timeoutMillis: Long = 10_000
    ) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = substring, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntilMissing(
        text: String,
        substring: Boolean = false,
        timeoutMillis: Long = 10_000
    ) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = substring, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private fun clickFirstNodeWithText(text: String) {
        composeRule.onAllNodesWithText(text, useUnmergedTree = true)[0].performClick()
    }
}
