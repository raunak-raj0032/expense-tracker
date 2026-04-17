package com.expensetracker.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: TransactionSyncRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val push = syncRepository.pushAll()
        if (push is SyncResult.Failure) return Result.retry()
        val pull = syncRepository.pullAll()
        return if (pull is SyncResult.Failure) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_PERIODIC = "transaction_sync_periodic"
        private const val UNIQUE_ONESHOT = "transaction_sync_oneshot"

        fun enqueuePeriodic(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<TransactionSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun enqueueOneShot(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniqueWork(UNIQUE_ONESHOT, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(UNIQUE_PERIODIC)
            workManager.cancelUniqueWork(UNIQUE_ONESHOT)
        }
    }
}
