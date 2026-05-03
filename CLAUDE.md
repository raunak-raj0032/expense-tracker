# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (minification enabled)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.expensetracker.app.capture.PaymentMessageParserTest"

# Run instrumented (device/emulator) tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

**Requirements:** Java 17, Android SDK 34, Gradle 8.13+

## Architecture

This is an offline-first Android app (Jetpack Compose + Room + Hilt). All financial data stays on-device; Firebase Auth is the only network-required feature (optional — the app degrades gracefully if `google-services.json` is absent).

### Layer diagram

```
UI (Composables + ViewModels)
    └── Repositories  (core/data/repository/)
            └── Room DAOs  (core/database/dao/)
                    └── Room Entities  (core/database/entity/)
            Domain models (core/model/) — returned by repositories, never entities
```

- **Single `ExpenseDatabase`** (`core/database/ExpenseDatabase.kt`) — 10 entities: Account, Category, Tag, Merchant, MerchantAlias, Transaction, TransactionTag, CaptureEvent, Rule, Budget.
- **Repositories** expose `Flow<List<DomainModel>>` for reactive UI and `suspend` functions for one-shot mutations. Never inject DAOs directly into ViewModels.
- **Hilt DI** wires everything. Modules live in `di/`: `DatabaseModule` (Room + DAOs), `AiModule` (binds `OllamaAiManager` → `OnDeviceAiManager`), `AuthModule` (wraps Firebase).
- **`UserPreferences`** (`core/prefs/`) — DataStore for non-transactional prefs (onboarding, currency, AI endpoint, biometric toggle, etc.).

### Navigation & screens

`MainNavigation.kt` owns the single `NavHost`. Auth state + onboarding flag drive automatic redirects (Onboarding → Login → Home). The bottom nav has five tabs: Home, Ledger, Calendar, Analytics, Settings.

Screens that exist outside the bottom nav (accessed via push): AddEditTransaction, CaptureInbox, BudgetSetup, StatementImport, Tags, Profile.

### Auto-capture pipeline

Transactions can enter the system from three sources, all funneled through `CaptureEventRepository`:

1. **`ExpenseNotificationListenerService`** — listens to a hard-coded set of UPI/bank package names plus keyword-based fallback for unlisted apps.
2. **`ExpenseSmsReceiver`** — broadcast receiver for SMS.
3. **`UpiAccessibilityService`** — reads UPI payment screens via accessibility events.

All three call `PaymentMessageParser` (regex-based, no network) to extract amount, direction, merchant, and reference from raw text. Parsed events land in `CaptureEvent` with `TransactionStatus.SUGGESTED`; the user confirms them in `CaptureReviewScreen`.

### AI (optional)

`OnDeviceAiManager` is the interface; `OllamaAiManager` is the production implementation — it talks to a self-hosted Ollama server over HTTP (default model: `llama3.2:3b`). The endpoint is stored in `UserPreferences`. AI is used for natural-language transaction search and ledger filtering (`AiAppliedFilters`). `StubOnDeviceAiManager` is provided for testing.

### Statement import

`StatementImportParser` reads PDF bank statements (pdfbox-android) and CSV files, extracts transactions using the same `PaymentMessageParser` narrative logic, and surfaces drafts for user review.

### Budget widget

`BudgetGlanceWidget` — Glance AppWidget that shows the current month's budget vs. spend. Period is configurable via `UserPreferences`.

## Firebase / Auth

- Firebase Auth is optional. Without `app/google-services.json`, auth methods throw `AuthException("Firebase Auth is not configured…")` and the app falls back gracefully.
- Auth flows: Google Sign-In (Credential Manager), Email/Password, Anonymous (guest). All go through `AuthRepository`.
- Auth state is a `Flow<AuthState>` (SignedIn / SignedOut / Loading). `MainNavigation` listens to it to gate routes.

## Key conventions

- **Money is stored as `Long` minor units** (paise for INR). `CurrencyConverter` and `LocalHomeCurrency` handle display formatting. Never store or pass amounts as `Double`.
- **`TransactionStatus`**: `CONFIRMED` for user-entered, `SUGGESTED` for auto-captured (pending review).
- Screens follow the pattern: one `*Screen.kt` (Composable) paired with one `*ViewModel.kt` (Hilt `@HiltViewModel`). ViewModels expose `StateFlow`/`Flow`, not `LiveData`.
- Database schema version is `1` with `exportSchema = false`. Increment version and add a migration for any schema change.
