# Firebase + Google Sign-In Setup (Free Tier)

Phase 1 wires up Firebase Authentication with Google Sign-In. Firebase's Spark plan (free forever, no card required) covers everything we use.

## 1. Create a Firebase project
1. Go to https://console.firebase.google.com/ and sign in with the Google account you want to own the project.
2. Click **Add project** -> name it (e.g. "pocket-pulse") -> disable Google Analytics (optional) -> **Create**.

## 2. Register the Android app
1. In the project overview, click the Android icon to add an Android app.
2. **Package name**: `com.expensetracker.app` (must match `applicationId` in `app/build.gradle.kts`).
3. **App nickname**: anything (e.g. Pocket Pulse).
4. **Debug signing certificate SHA-1**: get it by running:
   ```bash
   ./gradlew signingReport
   ```
   Copy the `SHA1` line under `Variant: debug`. Paste it into the Firebase form.
   (You can add more SHAs later for release builds via Project Settings -> Your apps.)
   Current local debug SHA-1 for this workspace:
   `2D:B0:02:C8:C0:89:EB:23:06:F2:31:E7:E7:E2:E6:77:92:90:B8:EB`
5. Click **Register app**.

## 3. Drop `google-services.json` into the project
1. Download `google-services.json` from the Firebase console.
2. Place it at: `app/google-services.json` (same folder as `app/build.gradle.kts`).
3. Do **not** commit it if you consider it sensitive (it is safe to commit, but project-scoped).

## 4. Enable Google Sign-In
1. In Firebase Console -> **Build** -> **Authentication** -> **Get started**.
2. Under **Sign-in method** tab, click **Google** -> enable -> set support email -> **Save**.

## 5. Verify the Web client ID resource
Firebase auto-generates `R.string.default_web_client_id` from `google-services.json` (this is what `AuthRepository` reads). You don't need to hardcode anything. If you ever need it, it lives in the Firebase console under **Project settings -> General -> Web client ID**.

## 6. Build and run
```bash
./gradlew :app:assembleDebug
```
On first launch the app opens the animated Login screen. Tap **Continue with Google**, pick an account, and you'll land on Home with "Hi, {FirstName}" in the top bar. The logout icon next to Ledger clears the session.

## Notes / troubleshooting
- **"Developer error" / `10`**: SHA-1 in Firebase doesn't match the build. Re-run `signingReport` and confirm.
- **"No credentials available"**: the device has no Google account, or Google Play Services is missing. Add an account in system settings.
- **Build fails at `processDebugGoogleServices`**: `app/google-services.json` is still missing or is in the wrong folder. The correct path is exactly `app/google-services.json`.
- **Release builds**: add the release keystore SHA-1 to the same Firebase app before shipping.
- **Cost**: Spark plan covers 50k monthly active auth users. Phase 1 uses only Firebase Auth, so billing never activates.
