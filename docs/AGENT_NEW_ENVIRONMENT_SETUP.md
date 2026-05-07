# Agent Setup Guide: New Environment

Use this guide when recreating the Expense Tracker app on a new machine or handing it to another coding agent.

## Project Snapshot

- Android app written in Kotlin with Jetpack Compose.
- Package/application id: `com.expensetracker.app`.
- Offline-first architecture with Room, DataStore, Hilt, Coroutines/Flow, Navigation Compose, Glance widgets, and MPAndroidChart.
- Financial data is local-first. Firebase Auth is optional and the app should build without `app/google-services.json`.
- Optional hosted/self-hosted LLM support uses an Ollama-compatible HTTP endpoint. Default model is `llama3.2:3b`.
- UPI capture can use notifications, SMS, and an Android accessibility service. Captured items become review suggestions before they are saved as confirmed transactions.

## Required Tooling

Install these on the new system:

- JDK 17.
- Android Studio with Android SDK Platform 36 installed.
- Android SDK build tools compatible with AGP 9.2.0.
- Git.
- Optional: Ollama, if AI search/insights should be tested.

The Gradle wrapper is checked in and currently points to Gradle `9.4.1`, so prefer the wrapper over a system Gradle install.

## First-Time Checkout

1. Clone the repo.
2. Open the repo root in Android Studio.
3. Confirm `local.properties` points to the Android SDK on that machine:

   ```properties
   sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
   ```

   On macOS/Linux, use that machine's SDK path instead.

4. Let Android Studio sync Gradle.
5. Build from the repo root:

   ```powershell
   .\gradlew.bat assembleDebug
   ```

   On macOS/Linux:

   ```bash
   ./gradlew assembleDebug
   ```

## Important Build Details

- Root plugins:
  - Android Gradle Plugin `9.2.0`
  - Kotlin Compose plugin `2.3.20`
  - KSP `2.3.7`
  - Hilt `2.59.2`
  - Google Services plugin `4.4.2`
- App SDK values:
  - `compileSdk = 36`
  - `targetSdk = 34`
  - `minSdk = 26`
- Gradle memory is intentionally conservative in `gradle.properties`:
  - single worker
  - in-process Kotlin compiler
  - Serial GC
  - reduced heap

Keep those settings unless the new machine has enough memory and the build is stable.

## Optional Firebase Auth Setup

Firebase is optional. The app is expected to build and run without Firebase configuration.

If Firebase auth is needed:

1. Create/configure a Firebase Android app for package `com.expensetracker.app`.
2. Download `google-services.json`.
3. Place it at:

   ```text
   app/google-services.json
   ```

4. Rebuild.

If the file is absent, the Google Services plugin is not applied and `default_web_client_id` is supplied as an empty resource so local/offline builds keep working.

## Optional Ollama / Hosted LLM Setup

AI features use `OllamaAiManager` through the `OnDeviceAiManager` interface.

Default model:

```text
llama3.2:3b
```

For local machine testing:

```bash
ollama pull llama3.2:3b
ollama serve
```

For phone-to-computer testing, the phone must reach the host machine over LAN. On Windows, the helper script can be used as a reference:

```powershell
.\start-ollama-lan.ps1
```

Then set the AI endpoint inside the app settings to the reachable host URL, for example:

```text
http://192.168.1.20:11434
```

Use emulator loopback only when running on an Android emulator:

```text
http://10.0.2.2:11434
```

## Android Permissions To Test Manually

Several features require explicit device permissions/settings:

- Notification listener for payment/bank app notifications.
- SMS receive/read permissions for SMS transaction capture and import.
- Accessibility service for UPI payment screen capture.
- Post notifications on modern Android versions.
- Biometric/PIN settings for app lock.

UPI accessibility service metadata lives at:

```text
app/src/main/res/xml/upi_accessibility_service.xml
```

The service should only create reviewable suggestions, not silently confirm transactions.

## Smoke Test Checklist

Run these checks after setup:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

Then install/run on a device or emulator and verify:

- App opens through onboarding/login to Home.
- Manual transaction add/edit works.
- Ledger, Calendar, Analytics, Settings, Budget, Backup, Tags, and Profile screens open.
- Local backup export/restore screen opens.
- Statement import screen opens.
- AI settings accept an Ollama endpoint.
- Capture inbox opens and displays suggested transactions when capture sources are enabled.
- Budget notification/widget resources compile.

## Notes For Future Agents

- Do not move financial data off-device unless the user explicitly asks for sync/cloud features.
- Keep Firebase optional. Local-only builds are a supported path.
- Keep UPI/SMS/notification capture user-reviewed.
- Money is stored as `Long` minor units. Do not introduce `Double` for persisted amounts.
- ViewModels should talk to repositories, not DAOs directly.
- If adding or changing Room schema, increment the database version and add migrations.
- Prefer small focused commits grouped by feature or setup concern.
