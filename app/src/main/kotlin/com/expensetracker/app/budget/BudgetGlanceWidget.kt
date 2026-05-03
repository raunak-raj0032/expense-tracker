package com.expensetracker.app.budget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.expensetracker.app.core.prefs.UserPreferences
import com.expensetracker.app.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val KEY_PERIOD = stringPreferencesKey("widget_period")
private val KEY_BUDGET = longPreferencesKey("widget_budget_minor")
private val KEY_SPENT = longPreferencesKey("widget_spent_minor")
private val KEY_HAS_BUDGET = booleanPreferencesKey("widget_has_budget")
private val KEY_PERIOD_LABEL = stringPreferencesKey("widget_period_label")

private val SIZE_TINY = DpSize(110.dp, 40.dp)
private val SIZE_SMALL = DpSize(180.dp, 80.dp)
private val SIZE_MEDIUM = DpSize(250.dp, 110.dp)

private val Bg = Color(0xFF111128)
private val BgAccent = Color(0xFF1B1B3F)
private val Accent = Color(0xFF8C8CFF)
private val OnBg = Color(0xFFF5F5FF)
private val OnBgDim = Color(0xFF9AA0FF)
private val Ok = Color(0xFF7CFFC4)
private val Bad = Color(0xFFFF7C8A)
private val TrackBg = Color(0xFF2A2A4A)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BudgetWidgetEntryPoint {
    fun budgetTracker(): BudgetTracker
    fun userPreferences(): UserPreferences
}

class BudgetGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_TINY, SIZE_SMALL, SIZE_MEDIUM))
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val prefs = currentState<Preferences>()
        val period = BudgetPeriod.fromName(prefs[KEY_PERIOD])
        val hasBudget = prefs[KEY_HAS_BUDGET] ?: false
        val budget = prefs[KEY_BUDGET] ?: 0L
        val spent = prefs[KEY_SPENT] ?: 0L
        val label = prefs[KEY_PERIOD_LABEL] ?: "This month"
        val remaining = budget - spent
        val ok = remaining >= 0
        val pctFloat = if (hasBudget && budget > 0) (spent.toFloat() / budget.toFloat()).coerceIn(0f, 1f) else 0f

        val size = LocalSize.current

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(ColorProvider(Bg))
                .clickable(actionStartActivity<MainActivity>())
        ) {
            when {
                size.height < 60.dp -> TinyLayout(period, hasBudget, ok, remaining)
                size.height < 100.dp -> SmallLayout(period, hasBudget, ok, remaining, pctFloat)
                else -> MediumLayout(period, hasBudget, spent, budget, ok, remaining, pctFloat, label)
            }
        }
    }
}

@Composable
private fun TinyLayout(
    period: BudgetPeriod, hasBudget: Boolean,
    ok: Boolean, remaining: Long
) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = period.label.uppercase(),
                style = TextStyle(color = ColorProvider(OnBgDim), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
            if (hasBudget) {
                Text(
                    text = if (ok) "${formatRupeesShort(remaining)} left"
                    else "−${formatRupeesShort(-remaining)}",
                    style = TextStyle(
                        color = ColorProvider(if (ok) Ok else Bad),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            } else {
                Text(
                    text = "Set budget",
                    style = TextStyle(color = ColorProvider(OnBg), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
        SwitchPill()
    }
}

@Composable
private fun SmallLayout(
    period: BudgetPeriod, hasBudget: Boolean,
    ok: Boolean, remaining: Long, pctFloat: Float
) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(14.dp)
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PeriodBadge(period.label)
            Spacer(GlanceModifier.defaultWeight())
            SwitchPill()
        }
        Spacer(GlanceModifier.height(8.dp))
        if (hasBudget) {
            Text(
                text = if (ok) "${formatRupees(remaining)} left"
                else "Over by ${formatRupees(-remaining)}",
                style = TextStyle(
                    color = ColorProvider(if (ok) Ok else Bad),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(6.dp))
            ProgressBar(pctFloat, ok, widthDp = 200)
        } else {
            Text(
                text = "Set a monthly budget",
                style = TextStyle(color = ColorProvider(OnBg), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Tap to open the app",
                style = TextStyle(color = ColorProvider(OnBgDim), fontSize = 11.sp)
            )
        }
    }
}

@Composable
private fun MediumLayout(
    period: BudgetPeriod, hasBudget: Boolean, spent: Long, budget: Long,
    ok: Boolean, remaining: Long, pctFloat: Float, label: String
) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PeriodBadge(period.label)
            Spacer(GlanceModifier.defaultWeight())
            SwitchPill()
        }
        Spacer(GlanceModifier.height(10.dp))
        if (hasBudget) {
            Text(
                text = formatRupees(spent),
                style = TextStyle(color = ColorProvider(OnBg), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "of ${formatRupees(budget)} · $label",
                style = TextStyle(color = ColorProvider(OnBgDim), fontSize = 11.sp)
            )
            Spacer(GlanceModifier.height(8.dp))
            ProgressBar(pctFloat, ok, widthDp = 260)
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = if (ok) "${formatRupees(remaining)} remaining"
                else "Over by ${formatRupees(-remaining)}",
                style = TextStyle(
                    color = ColorProvider(if (ok) Ok else Bad),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        } else {
            Text(
                text = "Set a monthly budget",
                style = TextStyle(color = ColorProvider(OnBg), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "Tap to open Pocket Pulse and add one",
                style = TextStyle(color = ColorProvider(OnBgDim), fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun PeriodBadge(text: String) {
    Box(
        modifier = GlanceModifier
            .cornerRadius(10.dp)
            .background(ColorProvider(BgAccent))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(color = ColorProvider(Accent), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun SwitchPill() {
    Box(
        modifier = GlanceModifier
            .cornerRadius(10.dp)
            .background(ColorProvider(Accent))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clickable(actionRunCallback<CycleWidgetPeriodAction>())
    ) {
        Text(
            text = "↻",
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun ProgressBar(pct: Float, ok: Boolean, widthDp: Int) {
    val filled = (widthDp * pct).toInt().coerceAtLeast(0)
    Box(
        modifier = GlanceModifier
            .width(widthDp.dp)
            .height(6.dp)
            .cornerRadius(3.dp)
            .background(ColorProvider(TrackBg))
    ) {
        if (filled > 0) {
            Box(
                modifier = GlanceModifier
                    .width(filled.dp)
                    .height(6.dp)
                    .cornerRadius(3.dp)
                    .background(ColorProvider(if (ok) Ok else Bad))
            ) {}
        }
    }
}

class CycleWidgetPeriodAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext, BudgetWidgetEntryPoint::class.java
        )
        val prefs = ep.userPreferences()
        val current = BudgetPeriod.fromName(prefs.budgetWidgetPeriod.first())
        prefs.setBudgetWidgetPeriod(current.next().name)
        BudgetWidgetUpdater.refresh(context)
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetGlanceWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        BudgetWidgetUpdater.refresh(context)
    }
}

object BudgetWidgetUpdater {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun refresh(context: Context) {
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext, BudgetWidgetEntryPoint::class.java
        )
        val tracker = ep.budgetTracker()
        val prefs = ep.userPreferences()
        scope.launch {
            val period = BudgetPeriod.fromName(prefs.budgetWidgetPeriod.first())
            val snap = tracker.observe(period).first()
            val widget = BudgetGlanceWidget()
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(BudgetGlanceWidget::class.java)
            ids.forEach { id ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { p ->
                    p.toMutablePreferences().apply {
                        this[KEY_PERIOD] = period.name
                        this[KEY_BUDGET] = snap.budgetMinor ?: 0L
                        this[KEY_SPENT] = snap.spentMinor
                        this[KEY_HAS_BUDGET] = snap.hasBudget
                        this[KEY_PERIOD_LABEL] = snap.periodLabel
                    }
                }
                widget.update(context, id)
            }
        }
    }
}

private fun formatRupees(minor: Long): String {
    val rupees = minor / 100
    val paise = minor % 100
    return "₹$rupees${if (paise > 0) ".${paise.toString().padStart(2, '0')}" else ""}"
}

private fun formatRupeesShort(minor: Long): String {
    val rupees = minor / 100
    return when {
        rupees >= 100_000 -> "₹%.1fL".format(rupees / 100_000.0)
        rupees >= 1_000 -> "₹%.1fk".format(rupees / 1_000.0)
        else -> "₹$rupees"
    }
}
