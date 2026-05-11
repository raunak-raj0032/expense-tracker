# Scheduled Notification System — Implementation Plan

## Overview

4 `Worker` classes, all scheduled via `WorkManager`. No UI toggle needed — all logic is app-controlled. Uses existing `BudgetTracker.observe()` to get live data.

---

## Channels & Notification IDs

| Channel | ID | Priority |
|---|---|---|
| Budget alerts | `budget_alerts` | HIGH |
| Smart reminders | `smart_reminders` | DEFAULT |

| Notification | ID | Channel |
|---|---|---|
| Inactivity reminder | `4910` | `smart_reminders` |
| Quote of the day | `4911` | `smart_reminders` |
| Daily budget alert | `4912` | `budget_alerts` |
| Weekly budget alert | `4913` | `budget_alerts` |
| Monthly budget alert | `4914` | `budget_alerts` |

---

## New Files

### `core/notif/NotificationChannels.kt`

Creates two channels on app start:

- `smart_reminders` — `IMPORTANCE_DEFAULT`, description "Inactivity reminders and daily motivation"
- `budget_alerts` — `IMPORTANCE_HIGH`, description "Alerts when your budget runs low"

---

### `core/notif/LastActiveTracker.kt`

Thin `SharedPreferences` wrapper (not DataStore — simpler for workers to access from any thread):

| Key | Type | Purpose |
|---|---|---|
| `last_app_open_ms` | Long | Epoch millis of last `onResume` |
| `quote_sent_date` | String | "yyyy-MM-dd" of last quote send |
| `daily_alert_sent` | Boolean | Today's daily budget alert fired |
| `weekly_alert_sent` | Boolean | This week's budget alert fired |
| `monthly_alert_sent` | Boolean | This month's budget alert fired |

Exposed as injectable `Singleton`. Updated by `MainActivity` on every `onResume`. Workers read only.

---

### `work/AppActivityWorker.kt` — Inactivity Reminder

- **Interval**: 15 minutes (`PeriodicWorkRequestBuilder`)
- **Logic**:
  - Read `last_app_open_ms` from `LastActiveTracker`
  - If elapsed > 4 hours → show notification:
    - **Title**: "Hey, missing you! 👋"
    - **Body** (personalized): `"{FirstName}, you've been away for {X} hours. Log today's expenses to stay on track!"`
    - Fallback if no first name: `"You haven't opened Pocket Pulse in {X} hours. Come log today's expenses!"`
    - **Mood**: `PennyMood.Curious` (half-cut asset)
    - **Tap**: opens `AddTransaction` screen
- **Reset**: whenever user opens the app (tracked via `onResume`)

---

### `work/QuoteOfTheDayWorker.kt` — Daily Quote

- **Interval**: Once-daily (`PeriodicWorkRequestBuilder` with `WorkManager.setInitialDelay()` targeting ~9 AM)
- **Logic**:
  - Read `quote_sent_date`, if today's date already there → skip silently
  - Pick random quote from list, send notification:
    - **Title**: "Your daily money wisdom 💡"
    - **Body** (personalized): `"{Quote}"` with optional context e.g. `"— You've saved ₹X this month!"`
    - **Mood**: `PennyMood.Cheer` (half-cut asset)
    - **Tap**: opens Home screen
  - Update `quote_sent_date` to today
- **Quote list** (20+ quotes, randomly selected):
  - *"Small daily savings lead to big financial freedom."*
  - *"Track every rupee, waste not a single one."*
  - *"Wealth is not about how much you earn — it's how much you keep."*
  - *"Your future self will thank you for every ₹100 saved today."*
  - *"Financial freedom is freedom from worry."*
  - *"Every expense you track is a step toward your goals."*
  - *"A penny saved is a penny earned."*
  - *"Budgeting isn't restriction — it's making your money work for you."*
  - *"Today's discipline, tomorrow's freedom."*
  - *"The best time to save was last month. The second best is now."*
  - *"Track the small things; the big picture takes care of itself."*
  - *"You can't improve what you don't measure."*
  - *"Consistency beats perfection — keep logging."*
  - *"Money saved is money earned — and you just earned ₹X today!"*
  - *"Your piggy bank is getting heavier, {name}!"*
  - *"Savings grow quietly. Keep going, {name}."*
  - *"Every ₹100 you save is a vote for your future."*
  - *"Slow and steady wins the money race."*
  - *"The habit of saving is the root of all wealth."*
  - *"Know where your money goes, and watch it grow."*
  - *"Discipline today, freedom tomorrow."*
  - *"One rupee saved is one rupee earned."*
  - *"Don't count your money while sitting at the table."*
  - *"Track it, control it, grow it."*

---

### `work/BudgetAlertWorker.kt` — Budget Threshold Alerts

- **Interval**: 30 minutes (`PeriodicWorkRequestBuilder`)
- **Logic**: Run all three budget checks in parallel:
  1. `budgetTracker.observe(DAILY)` → if `progressFraction >= 0.80` and not sent today → fire
  2. `budgetTracker.observe(WEEKLY)` → if `progressFraction >= 0.80` and not sent today → fire
  3. `budgetTracker.observe(MONTHLY)` → if `progressFraction >= 0.95` or `remaining < 0` and not sent today → fire
- **Reset**: on new day, new week, new month — reset the `sent` flags (detect by comparing stored date)
- **Notifications**:

  **Daily budget low** (Mood: `PennyMood.Surprised` half-cut):
  - Title: "Daily budget running low 🔔"
  - Body: `"{Name}, you've spent {X}% of today's budget. ₹{remaining} left — keep going!"`

  **Weekly budget low** (Mood: `PennyMood.Excited` half-cut):
  - Title: "Weekly budget check ⚠️"
  - Body: `"{Name}, {X}% of this week's budget used. ₹{remaining} remaining. Pace yourself!"`

  **Monthly budget alert** (Mood: `PennyMood.Excited` or `PennyMood.Surprised` based on severity):
  - Title: "Monthly budget {almost/at limit} 📊"
  - Body (if over): `"{Name}, you've overspent your monthly budget by ₹{over}. Consider adjusting."`
  - Body (if almost): `"{Name}, only ₹{remaining} left this month — {X}% used. Last few days, spend wisely!"`

  All tap → `Budgets` screen

---

### `work/WorkerScheduler.kt`

Utility object with:

- `scheduleAll(context)` — schedules all 3 periodic workers. Called from `ExpenseTrackerApp.onCreate()` and `BootReceiver.onReceive()`
- `cancelAll(context)` — cancels all 3 workers
- Individual `schedule/remit/quote/budgetAlert(context)` methods

Workers use `ExistingPeriodicWorkPolicy.KEEP` so repeated calls don't double-schedule.

---

### `SettingsScreen.kt` — Debug Test Section

New collapsible section at the bottom of Settings:

```
📡 Notification Debug
─────────────────────────
[Send Reminder Now]         → fires AppActivityWorker immediately
[Send Quote Now]            → fires QuoteOfTheDayWorker immediately
[Send Daily Budget Alert]   → fires budget alert for daily
[Send Weekly Budget Alert]  → fires budget alert for weekly
[Send Monthly Budget Alert] → fires budget alert for monthly
─────────────────────────
```

Card format, subtle styling — clearly debug/dev tools. Each button calls `WorkManager.enqueue()` with a one-shot (`OneTimeWorkRequest`) variant of the respective worker. No DataStore changes needed.

---

## Key Implementation Details

### Penny in notifications

Use `BitmapFactory.decodeStream(context.assets.open(assetPath))` — same pattern used in `Piggy.kt`. Load the **half-cut** asset for all notification types. Pass the mood to the worker so it can decode the right PNG. Notification large icon set from the decoded bitmap.

### Personalization

Workers get `UserPreferences` injected (Hilt). Read `firstName` from auth state. Fall back to no-name if not signed in.

### Battery & timing

All workers use `setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())` to ensure they fire even on low battery.

### Boot handling

`BootReceiver` already exists — add a call to `WorkerScheduler.scheduleAll()` alongside the existing `BudgetNotificationService.start()`.

### App lifecycle tracking

`MainActivity.onResume()` updates `LastActiveTracker.lastAppOpenMs` to current time. This resets the inactivity timer on every app open.

---

## File List

| File | Action |
|---|---|
| `core/notif/NotificationChannels.kt` | Create |
| `core/notif/LastActiveTracker.kt` | Create |
| `work/AppActivityWorker.kt` | Create |
| `work/QuoteOfTheDayWorker.kt` | Create |
| `work/BudgetAlertWorker.kt` | Create |
| `work/WorkerScheduler.kt` | Create |
| `core/prefs/UserPreferences.kt` | Modify — add first name pref or read from AuthRepository |
| `ui/screens/settings/SettingsScreen.kt` | Modify — add debug section |
| `capture/BootReceiver.kt` | Modify — call WorkerScheduler.scheduleAll() |
| `ExpenseTrackerApp.kt` | Modify — call WorkerScheduler.scheduleAll() in onCreate() |
| `AndroidManifest.xml` | Modify — declare new workers / receivers if needed |