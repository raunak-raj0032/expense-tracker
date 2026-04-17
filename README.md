# Expense Tracker

A production-grade Android expense tracking application with auto-capture from UPI and bank apps, calendar views, and analytics.

## Features

- **Manual Expense Tracking**: Quick entry for expenses, income, transfers, and refunds
- **Auto-Capture**: Automatic transaction detection from UPI apps (Google Pay, PhonePe, Paytm, bank apps)
- **Calendar View**: Month calendar with daily expense/income markers and drill-down
- **Analytics**: Monthly spending trends, category breakdown, merchant analysis
- **Accounts**: Multiple account support (Cash, Bank, Wallet, Credit Card)
- **Categories**: Customizable expense/income categories
- **Tags**: Flexible tagging for trips, projects, reimbursements
- **Budgets**: Monthly budget tracking with alerts
- **Security**: App lock with PIN/biometric
- **Export**: CSV export and local backup/restore
- **Offline-First**: All data stored locally

## Tech Stack

- Kotlin
- Jetpack Compose
- Room Database
- Hilt DI
- Coroutines + Flow
- Navigation Compose
- MPAndroidChart

## Build

```bash
# Check Java and Android SDK
java -version

# Install Android SDK command line tools
# Set ANDROID_HOME to your SDK path

# Build debug APK
./gradlew assembleDebug

# Or on Windows
gradlew.bat assembleDebug
```

Google Sign-In requires Firebase project setup first. See [FIREBASE_SETUP.md](FIREBASE_SETUP.md) and place `google-services.json` at `app/google-services.json` before building.

## Setup Requirements

1. Java 17
2. Android SDK 34
3. Gradle 8.2+

## Live UI Development

This project is a Jetpack Compose Android app, so the closest workflow to `pnpm dev`
is Android Studio `Live Edit` plus the shared `app` debug run configuration.

1. Open the project in Android Studio.
2. Start an emulator or connect a device.
3. Run the shared `app` configuration once in `Debug` mode.
4. Enable `Settings > Editor > Live Edit`.
5. Edit a Compose screen and save to push UI changes to the running app.

Best file to test live changes first:
`app/src/main/kotlin/com/expensetracker/app/ui/screens/home/HomeScreen.kt`

Best highly visible changes to try:
- Change the `Pocket Pulse` title text in `HomeScreen`.
- Change a card background or accent color in `MonthlySummaryCard`.
- Change the bottom navigation labels in `MainNavigation`.

Use `Apply Changes` when Live Edit cannot update a change in place. Do a full rerun
for manifest changes, dependency changes, database schema changes, or non-UI wiring.

## Project Structure

```
app/
├── src/main/
│   ├── kotlin/com/expensetracker/app/
│   │   ├── core/
│   │   │   ├── model/        # Domain models
│   │   │   ├── database/    # Room entities, DAOs
│   │   │   └── data/        # Repositories
│   │   ├── ui/
│   │   │   ├── screens/     # Screen composables + ViewModels
│   │   │   ├── navigation/
│   │   │   └── theme/
│   │   ├── capture/        # Notification capture service
│   │   └── di/            # Hilt modules
│   └── res/
└── build.gradle.kts
```

## Permissions

- `POST_NOTIFICATIONS` - Capture transaction notifications
- `RECEIVE_BOOT_COMPLETED` - Auto-start after boot
- `USE_BIOMETRIC` - App lock

## Privacy

All financial data is stored locally on-device. The app does not transmit personal financial data to external servers.

## License

MIT
