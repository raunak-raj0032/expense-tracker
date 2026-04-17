# Firestore Cloud Backup Setup (Phase 2)

Phase 2 adds private per-user transaction backup using Cloud Firestore. Stays inside the Spark (free) plan.

## 1. Enable Firestore
1. Firebase Console -> your project -> **Build** -> **Firestore Database** -> **Create database**.
2. Choose **Start in production mode** (we'll lock it down below).
3. Pick a location close to your users (you can't change this later).

## 2. Paste the security rules
**Rules** tab, replace with:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/transactions/{txnId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Publish. These rules ensure a user can only read/write their own `users/{uid}/transactions/*` subcollection.

## 3. Done
No client config needed — Firestore uses the same `google-services.json` you already dropped in `app/` for Phase 1.

## What gets synced
- Transactions only (the `transactions` collection, keyed by local Room id).
- Push happens on sign-in, every 6 hours via WorkManager, and on demand from **Profile -> Sync now**.
- Pull is also run on sign-in, filling this device from the cloud if the cloud copy is newer (by `updatedAt`).

## What does NOT sync (yet)
- Accounts, categories, merchants, budgets, tags. Those are device-local. If you sync to a fresh device, transactions will reference account IDs that may not exist locally. For now this is acceptable — sync is backup/restore, not true multi-device.

## Cost
- Spark plan: 50k reads/day, 20k writes/day, 1 GiB stored. Normal personal use is far below this.
- Each device pushes a full batch on sync — fine for hundreds of transactions; revisit if you hit thousands.

## Troubleshooting
- **`PERMISSION_DENIED`** on first sync: you're not signed in, or the rules weren't published.
- **Sync button shows nothing happens**: check Logcat for Firestore errors and confirm network.
- **Duplicate transactions after reinstall**: expected — pull reinserts cloud rows with new local ids since the old local ids are gone. The fingerprint index on Room will still dedupe captured SMS/notification data on subsequent runs.
