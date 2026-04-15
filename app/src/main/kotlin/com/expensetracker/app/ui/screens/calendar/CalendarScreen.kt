@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.expensetracker.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.core.model.CalendarDay
import com.expensetracker.app.ui.screens.home.TransactionListItem
import com.expensetracker.app.ui.screens.home.formatAmount
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionHeader
import com.expensetracker.app.ui.theme.appButtonSizing
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private enum class CalendarViewMode {
    MONTH,
    WEEK,
    CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onDayClick: (LocalDate) -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var customStart by remember { mutableStateOf(LocalDate.now().minusDays(6)) }
    var customEnd by remember { mutableStateOf(LocalDate.now()) }
    var showCustomRangePicker by remember { mutableStateOf(false) }

    val periodRange = remember(viewMode, currentMonth, selectedDate, customStart, customEnd) {
        when (viewMode) {
            CalendarViewMode.MONTH -> currentMonth.atDay(1) to currentMonth.atEndOfMonth()
            CalendarViewMode.WEEK -> weekRange(selectedDate)
            CalendarViewMode.CUSTOM -> customStart to customEnd
        }
    }

    LaunchedEffect(viewMode, currentMonth, selectedDate, customStart, customEnd) {
        val constrainedDate = selectedDate.coerceIn(periodRange.first, periodRange.second)
        if (constrainedDate != selectedDate) {
            selectedDate = constrainedDate
            return@LaunchedEffect
        }

        if (viewMode == CalendarViewMode.MONTH) {
            viewModel.loadCalendarData(currentMonth)
        } else {
            viewModel.loadRangeData(periodRange.first, periodRange.second)
        }
        viewModel.selectDate(constrainedDate)
    }

    val periodLabel = when (viewMode) {
        CalendarViewMode.MONTH -> currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        CalendarViewMode.WEEK -> "${periodRange.first.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${periodRange.second.format(DateTimeFormatter.ofPattern("dd MMM"))}"
        CalendarViewMode.CUSTOM -> "${periodRange.first.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${periodRange.second.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Calendar")
                        Text(
                            text = "Review spending by month, week, or a custom range",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("calendar_list"),
            contentPadding = PaddingValues(
                start = ScreenEdgePadding,
                top = 8.dp,
                end = ScreenEdgePadding,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PeriodNavigator(
                    viewMode = viewMode,
                    periodLabel = periodLabel,
                    selectedDate = selectedDate,
                    onPrevious = {
                        when (viewMode) {
                            CalendarViewMode.MONTH -> currentMonth = currentMonth.minusMonths(1)
                            CalendarViewMode.WEEK -> selectedDate = selectedDate.minusWeeks(1)
                            CalendarViewMode.CUSTOM -> {
                                val span = ChronoUnit.DAYS.between(customStart, customEnd) + 1
                                customStart = customStart.minusDays(span)
                                customEnd = customEnd.minusDays(span)
                                selectedDate = selectedDate.minusDays(span)
                            }
                        }
                    },
                    onNext = {
                        when (viewMode) {
                            CalendarViewMode.MONTH -> currentMonth = currentMonth.plusMonths(1)
                            CalendarViewMode.WEEK -> selectedDate = selectedDate.plusWeeks(1)
                            CalendarViewMode.CUSTOM -> {
                                val span = ChronoUnit.DAYS.between(customStart, customEnd) + 1
                                customStart = customStart.plusDays(span)
                                customEnd = customEnd.plusDays(span)
                                selectedDate = selectedDate.plusDays(span)
                            }
                        }
                    }
                )
            }

            item {
                CalendarModeSelector(
                    viewMode = viewMode,
                    onViewModeChange = { mode ->
                        viewMode = mode
                        if (mode == CalendarViewMode.MONTH) {
                            currentMonth = YearMonth.from(selectedDate)
                        }
                    },
                    onOpenCustomPicker = { showCustomRangePicker = true }
                )
            }

            if (viewMode == CalendarViewMode.MONTH) {
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accent = MaterialTheme.colorScheme.primary
                    ) {
                        CalendarGrid(
                            yearMonth = currentMonth,
                            calendarDays = uiState.calendarDays,
                            selectedDate = selectedDate,
                            onDateClick = { date ->
                                selectedDate = date
                                viewModel.selectDate(date)
                                onDayClick(date)
                            }
                        )
                    }
                }
            } else {
                item {
                    PeriodOverviewCard(
                        viewMode = viewMode,
                        rangeStart = periodRange.first,
                        rangeEnd = periodRange.second,
                        expenseTotal = uiState.periodExpenseTotal,
                        incomeTotal = uiState.periodIncomeTotal,
                        transactionCount = uiState.periodTransactionCount
                    )
                }

                item {
                    Text(
                        text = "Period Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.periodDays) { day ->
                    PeriodDayCard(
                        day = day,
                        isSelected = day.date == selectedDate,
                        onClick = {
                            selectedDate = day.date
                            viewModel.selectDate(day.date)
                            onDayClick(day.date)
                        }
                    )
                }
            }

            item {
                DaySummaryCard(
                    date = selectedDate,
                    expenseTotal = uiState.dailyTotals[selectedDate]?.expenseTotal ?: 0,
                    incomeTotal = uiState.dailyTotals[selectedDate]?.incomeTotal ?: 0,
                    transactionCount = uiState.dailyTotals[selectedDate]?.transactionCount ?: 0
                )
            }

            item {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.transactionsForSelectedDate.isEmpty()) {
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accent = MaterialTheme.colorScheme.tertiary
                    ) {
                        Text(
                            text = "No transactions for this date yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.transactionsForSelectedDate) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
            }
        }
    }

    if (showCustomRangePicker) {
        val rangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customStart.toEpochMillis(),
            initialSelectedEndDateMillis = customEnd.toEpochMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showCustomRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = rangePickerState.selectedStartDateMillis?.toLocalDate()
                        val end = rangePickerState.selectedEndDateMillis?.toLocalDate()
                        if (start != null && end != null) {
                            customStart = start
                            customEnd = end
                            selectedDate = selectedDate.coerceIn(start, end)
                            viewMode = CalendarViewMode.CUSTOM
                        }
                        showCustomRangePicker = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = rangePickerState,
                title = {
                    Text(
                        text = "Custom Range",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PeriodNavigator(
    viewMode: CalendarViewMode,
    periodLabel: String,
    selectedDate: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.secondary
    ) {
        SectionHeader(
            eyebrow = "Calendar",
            title = periodLabel,
            subtitle = "Focused on ${selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
            trailing = {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = if (viewMode == CalendarViewMode.MONTH) "Previous Month" else "Previous Period"
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (viewMode == CalendarViewMode.MONTH) "Next Month" else "Next Period"
                    )
                }
            }
        )
    }
}

@Composable
private fun CalendarModeSelector(
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onOpenCustomPicker: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.tertiary,
        contentPadding = PaddingValues(14.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3
        ) {
            CalendarViewMode.entries.forEach { mode ->
                FilterChip(
                    selected = viewMode == mode,
                    onClick = { onViewModeChange(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.fillMaxWidth(0.31f)
                )
            }
            OutlinedButton(
                onClick = onOpenCustomPicker,
                modifier = Modifier
                    .fillMaxWidth()
                    .appButtonSizing()
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Select Range",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PeriodOverviewCard(
    viewMode: CalendarViewMode,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    expenseTotal: Long,
    incomeTotal: Long,
    transactionCount: Int
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.primary
    ) {
        SectionHeader(
            eyebrow = when (viewMode) {
                CalendarViewMode.MONTH -> "Month View"
                CalendarViewMode.WEEK -> "Week View"
                CalendarViewMode.CUSTOM -> "Custom View"
            },
            title = "${rangeStart.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${rangeEnd.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
            subtitle = "Periodical data breakdown"
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 3
        ) {
            SummaryPill(
                label = "Spent",
                value = formatAmount(expenseTotal),
                accent = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(0.31f)
            )
            SummaryPill(
                label = "Income",
                value = formatAmount(incomeTotal),
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(0.31f)
            )
            SummaryPill(
                label = "Events",
                value = transactionCount.toString(),
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(0.31f)
            )
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = accent.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun PeriodDayCard(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = day.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${day.transactionCount} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(day.expenseTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = formatAmount(day.incomeTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    calendarDays: Map<LocalDate, CalendarDay>,
    selectedDate: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    val daysOfWeek = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val firstDayOfMonth = yearMonth.atDay(1)
        val startOffset = firstDayOfMonth.dayOfWeek.value % 7
        val totalDays = yearMonth.lengthOfMonth()
        val totalCells = startOffset + totalDays
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val visibleDate = firstDayOfMonth
                        .minusDays(startOffset.toLong())
                        .plusDays(cellIndex.toLong())
                    val isCurrentMonth = YearMonth.from(visibleDate) == yearMonth
                    val isSelected = visibleDate == selectedDate
                    val isToday = visibleDate == LocalDate.now()
                    val hasTransactions = isCurrentMonth && calendarDays.containsKey(visibleDate)

                    CalendarDayCell(
                        date = visibleDate.takeIf { isCurrentMonth },
                        isSelected = isSelected,
                        isToday = isToday,
                        hasTransactions = hasTransactions,
                        transactionCount = calendarDays[visibleDate]?.transactionCount ?: 0,
                        modifier = Modifier.weight(1f),
                        onClick = { if (isCurrentMonth) onDateClick(visibleDate) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    hasTransactions: Boolean,
    transactionCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.tertiary
        hasTransactions -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = modifier
            .aspectRatio(0.86f)
            .clickable(enabled = date != null, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            isToday -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
            hasTransactions -> MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = if (date != null) 0.36f else 0.08f)
        )
    ) {
        if (date != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasTransactions) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = (10 + transactionCount.coerceAtMost(4) * 4).dp, height = 4.dp)
                                .background(accent, CircleShape)
                        )
                        Text(
                            text = transactionCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DaySummaryCard(
    date: LocalDate,
    expenseTotal: Long,
    incomeTotal: Long,
    transactionCount: Int
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.tertiary
    ) {
        SectionHeader(
            eyebrow = "Focused Day",
            title = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd")),
            subtitle = "Daily pulse overview"
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 3
        ) {
            SummaryPill(
                label = "Spent",
                value = formatAmount(expenseTotal),
                accent = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(0.31f)
            )
            SummaryPill(
                label = "Income",
                value = formatAmount(incomeTotal),
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(0.31f)
            )
            SummaryPill(
                label = "Transactions",
                value = transactionCount.toString(),
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(0.31f)
            )
        }
    }
}

private fun weekRange(anchor: LocalDate): Pair<LocalDate, LocalDate> {
    val daysFromSunday = anchor.dayOfWeek.value % 7
    val start = anchor.minusDays(daysFromSunday.toLong())
    return start to start.plusDays(6)
}

private fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun LocalDate.coerceIn(start: LocalDate, end: LocalDate): LocalDate {
    return when {
        this < start -> start
        this > end -> end
        else -> this
    }
}
