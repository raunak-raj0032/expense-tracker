# Backup/Restore Feature Plan

## Overview
Add import/export backup functionality with device restriction - backup can only be restored on the same device by default, with email verification for cross-device restore.

## Core Requirements

### 1. Device Identification
- Use **ANDROID_ID** from `Settings.Secure` - unique per device, survives app reinstalls but not factory reset
- Store device ID in backup header

### 2. Email Verification Flow
- **Export**: User sets a backup email in Settings → export generates backup file
- **Import on new device**:
  1. User selects backup file
  2. If device ID mismatch, app sends 6-digit code to the backup email
  3. User enters code to confirm restore

### 3. Restore Options (asked at restore time)
- **Clean rewrite**: Delete all current data → restore from backup
- **Merge**: Keep existing data, add new items that don't conflict

### 4. Factory Reset Leniency
- If device ID doesn't match (factory reset), show warning → user confirms to proceed anyway

### 5. Privacy
- Offline-first: No Firebase, no external data sync
- All financial data stays on device
- Email verification only used for backup restore, not stored long-term

## Technical Implementation

### Data to Back Up
| Data | Storage |
|------|---------|
| All 10 Room entities | Database (Account, Category, Tag, Merchant, MerchantAlias, Transaction, TransactionTag, CaptureEvent, Rule, Budget) |
| UserPreferences | DataStore (home_currency, biometric_enabled, budget settings, etc.) |
| Backup metadata | File header (version, device ID, timestamp) |

### Backup File Structure
```json
{
  "version": 1,
  "deviceId": "android-id-here",
  "createdAt": 1704067200000,
  "preferences": { "home_currency": "INR", ... },
  "accounts": [...],
  "categories": [...],
  "transactions": [...],
  ...
}
```

### Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `DeviceIdProvider` | `core/device/DeviceIdProvider.kt` | Get ANDROID_ID |
| `BackupRepository` | `core/data/repository/BackupRepository.kt` | Export/import logic + validation |
| `BackupScreen` | `ui/screens/backup/BackupScreen.kt` | UI |
| `BackupViewModel` | `ui/screens/backup/BackupViewModel.kt` | State + actions |
| Email logic | `core/backup/EmailSender.kt` | SMTP verification codes |

### Existing Infrastructure Used
- Room Database with 10 entities
- DataStore for preferences
- Hilt for DI
- Navigation system already has Backup route

## Implementation Status

### Completed
- DeviceIdProvider created
- BackupRepository with export/import logic
- EmailSender for verification codes
- BackupViewModel with full state management
- BackupScreen UI with settings, export, import flows
- Navigation wired in MainNavigation
- All DAOs updated with getAll/deleteAll methods

### User Flow
1. **Settings → Backup** → Opens Backup screen
2. Configure backup email + SMTP settings → Save
3. **Export**: Tap "Export Backup" → Save JSON file
4. **Import**: Select file → If different device, enter code → Choose merge/rewrite → Restore