package com.expensetracker.app.budget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
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

private const val BAR_WIDTH_DP = 200

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BudgetWidgetEntryPoint {
    fun budgetTracker(): BudgetTracker
    fun userPreferences(): UserPreferences
}

class BudgetGlanceWidget : GlanceAppWidget() {

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
        val pct = if (hasBudget && budget > 0)
            ((spent.toFloat() / budget.toFloat()).coerceIn(0f, 1f) * BAR_WIDTH_DP).toInt()
        else 0
        val ok = remaining >= 0

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1A1A2E)))
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${period.label} budget",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF9AA0FF)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0xFF3B3B6B)))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable(actionRunCallback<CycleWidgetPeriodAction>())
                    ) {
                        Text(
                            text = "Switch",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.height(6.dp))
                if (hasBudget) {
                    Text(
                        text = "${formatRupees(spent)} of ${formatRupees(budget)}",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = if (ok) "${formatRupees(remaining)} left · $label"
                        else "Over by ${formatRupees(-remaining)} · $label",
                        style = TextStyle(
                            color = ColorProvider(if (ok) Color(0xFF7CFFC4) else Color(0xFFFF7C8A)),
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Box(
                        modifier = GlanceModifier
                            .width(BAR_WIDTH_DP.dp)
                            .height(6.dp)
                            .background(ColorProvider(Color(0xFF2A2A4A)))
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(pct.dp)
                                .height(6.dp)
                                .background(ColorProvider(if (ok) Color(0xFF7CFFC4) else Color(0xFFFF7C8A)))
                        ) {}
                    }
                } else {
                    Text(
                        text = "Set a monthly budget in app",
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp)
                    )
                }
            }
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
